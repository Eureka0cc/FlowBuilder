package com.eureka.flowbuilder.core.handler;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.eureka.flowbuilder.ai.model.message.*;
import com.eureka.flowbuilder.ai.tools.BaseTool;
import com.eureka.flowbuilder.ai.tools.ToolManager;
import com.eureka.flowbuilder.model.entity.User;
import com.eureka.flowbuilder.model.enums.ChatHistoryMessageTypeEnum;
import com.eureka.flowbuilder.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

/**
 * JSON 消息流处理器
 * 处理 VUE_PROJECT 类型的复杂流式响应，包含工具调用信息
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    @Resource
    private ToolManager toolManager;

    /**
     * 处理 TokenStream（VUE_PROJECT）
     * 解析 JSON 消息并重组为完整的响应格式
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        // 收集数据用于生成后端记忆格式
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 用于跟踪已展示过的工具调用，避免流式分片重复刷屏
        Set<String> seenToolRequestKeys = new HashSet<>();
        return originFlux
                .map(chunk -> {
                    // 解析每个 JSON 消息块
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolRequestKeys);
                })
                .filter(StrUtil::isNotEmpty) // 过滤空字串
                .doOnComplete(() -> {
                    // 流式响应完成后，添加 AI 消息到对话历史
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());

                })
                .doOnError(error -> log.error("Vue 项目流处理异常, appId={}", appId, error))
                .onErrorResume(error -> {
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    return Flux.just("\n\n⚠️ " + errorMessage + "\n\n");
                });
    }

    /**
     * 解析并收集 TokenStream 数据
     */
    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolRequestKeys) {
        // 解析 JSON
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        switch (typeEnum) {
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                // 直接拼接响应
                chatHistoryStringBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                String toolName = toolRequestMessage.getName();
                Integer requestIndex = toolRequestMessage.getRequestIndex();
                // 流式分片阶段可能尚未带上 name，需等待后续分片
                if (StrUtil.isBlank(toolName)) {
                    return "";
                }
                String dedupeKey = buildToolRequestDedupeKey(toolId, toolName, requestIndex);
                if (seenToolRequestKeys.contains(dedupeKey)) {
                    return "";
                }
                seenToolRequestKeys.add(dedupeKey);
                BaseTool tool = toolManager.resolveTool(toolName, toolId);
                String requestDisplay;
                if (tool != null) {
                    requestDisplay = tool.generateToolRequestResponse();
                } else {
                    log.warn("未注册的工具调用: name={}, id={}", toolName, toolId);
                    requestDisplay = String.format("\n\n[选择工具] %s\n\n", toolName);
                }
                chatHistoryStringBuilder.append(requestDisplay);
                return requestDisplay;
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                String toolName = toolExecutedMessage.getName();
                String output;
                if (StrUtil.isBlank(toolName)) {
                    output = "\n\n[工具执行完成]\n\n";
                } else {
                    BaseTool tool = toolManager.getTool(toolName);
                    if (tool != null) {
                        JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                        output = String.format("\n\n%s\n\n", tool.generateToolExecutedResult(jsonObject));
                    } else {
                        log.warn("未注册的工具执行结果: name={}", toolName);
                        output = String.format("\n\n[工具执行] %s\n\n", toolName);
                    }
                }
                chatHistoryStringBuilder.append(output);
                return output;
            }
            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return "";
            }
        }
    }

    /**
     * 工具请求去重键：优先 id；流式分片 id 不稳定时用 name+index
     */
    private static String buildToolRequestDedupeKey(String toolId, String toolName, Integer requestIndex) {
        // 流式分片下 id 可能变化，优先用 index+name 去重
        if (requestIndex != null) {
            return "idx:" + requestIndex + ":" + toolName;
        }
        if (StrUtil.isNotBlank(toolId)) {
            return "id:" + toolId;
        }
        return "name:" + toolName;
    }
}
