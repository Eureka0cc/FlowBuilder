package com.eureka.flowbuilder.core;

import cn.hutool.json.JSONUtil;
import com.eureka.flowbuilder.ai.AiCodeGeneratorService;
import com.eureka.flowbuilder.ai.AiCodeGeneratorServiceFactory;
import com.eureka.flowbuilder.ai.model.HtmlCodeResult;
import com.eureka.flowbuilder.ai.model.MultiFileCodeResult;
import com.eureka.flowbuilder.ai.model.message.AiResponseMessage;
import com.eureka.flowbuilder.ai.model.message.ToolExecutedMessage;
import com.eureka.flowbuilder.ai.model.message.ToolRequestMessage;
import com.eureka.flowbuilder.constant.AppConstant;
import com.eureka.flowbuilder.core.builder.VueProjectBuilder;
import com.eureka.flowbuilder.core.parser.CodeParserExecutor;
import com.eureka.flowbuilder.core.saver.CodeFileSaverExecutor;
import com.eureka.flowbuilder.exception.BusinessException;
import com.eureka.flowbuilder.exception.ErrorCode;
import com.eureka.flowbuilder.model.enums.CodeGenTypeEnum;
import com.eureka.flowbuilder.service.AiUsageLogService;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private AiUsageLogService aiUsageLogService;

    @Value("${langchain4j.open-ai.streaming-chat-model.model-name}")
    private String streamingModelName;

    @Value("${langchain4j.open-ai.reasoning-streaming-chat-model.model-name}")
    private String reasoningModelName;

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用ID
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 根据应用ID获取对应的AI代码生成服务
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(htmlCodeResult, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(multiFileCodeResult, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用ID
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        return generateAndSaveCodeStream(userMessage, codeGenTypeEnum, appId, null);
    }

    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum,
                                                  Long appId, Long userId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 根据应用ID获取对应的AI代码生成服务
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                TokenStream codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeTokenStream(codeStream, CodeGenTypeEnum.HTML, appId, userId);
            }
            case MULTI_FILE -> {
                TokenStream codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeTokenStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId, userId);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream, appId, userId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @param appId 应用Id
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId, Long userId) {
        return Flux.create(sink -> {
            long startTime = System.currentTimeMillis();
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest, index);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        aiUsageLogService.recordSuccess(appId, userId, reasoningModelName,
                                CodeGenTypeEnum.VUE_PROJECT.getValue(), "CHAT_GEN",
                                response.metadata().tokenUsage(), System.currentTimeMillis() - startTime);
                        String projectPath = AppConstant.getVueProjectDirPath(appId);
                        log.info("Vue 项目 AI 对话完成，开始异步构建: {}", projectPath);
                        // 同步写入 building，避免前端在 SSE 结束后轮询到上一次 ready=true
                        vueProjectBuilder.markBuildStarting(projectPath);
                        vueProjectBuilder.buildProjectAsync(projectPath);
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        aiUsageLogService.recordFailure(appId, userId, reasoningModelName,
                                CodeGenTypeEnum.VUE_PROJECT.getValue(), "CHAT_GEN",
                                System.currentTimeMillis() - startTime, error);
                        log.error("Vue 项目 AI 对话失败: appId={}", appId, error);
                        sink.error(error);
                    })
                    .start();
        });
    }


    /**
     * 通用流式代码处理方法
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @param appId     应用ID
     * @return 流式响应
     */
    private Flux<String> processCodeTokenStream(TokenStream tokenStream, CodeGenTypeEnum codeGenType,
                                                Long appId, Long userId) {
        return Flux.create(sink -> {
            StringBuilder codeBuilder = new StringBuilder();
            long startTime = System.currentTimeMillis();
            tokenStream.onPartialResponse(chunk -> {
                        codeBuilder.append(chunk);
                        sink.next(chunk);
                    })
                    .onCompleteResponse(response -> {
                        try {
                            Object parsedResult = CodeParserExecutor.executeParser(codeBuilder.toString(), codeGenType);
                            File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                            log.info("保存成功，路径为：{}", savedDir.getAbsolutePath());
                        } catch (Exception e) {
                            log.error("保存失败: {}", e.getMessage(), e);
                        }
                        aiUsageLogService.recordSuccess(appId, userId, streamingModelName,
                                codeGenType.getValue(), "CHAT_GEN", response.metadata().tokenUsage(),
                                System.currentTimeMillis() - startTime);
                        sink.complete();
                    })
                    .onError(error -> {
                        aiUsageLogService.recordFailure(appId, userId, streamingModelName,
                                codeGenType.getValue(), "CHAT_GEN",
                                System.currentTimeMillis() - startTime, error);
                        sink.error(error);
                    })
                    .start();
        });
    }

}
