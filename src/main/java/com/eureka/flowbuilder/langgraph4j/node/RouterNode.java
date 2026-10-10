package com.eureka.flowbuilder.langgraph4j.node;

import com.eureka.flowbuilder.ai.AiCodeGenTypeRoutingService;
import com.eureka.flowbuilder.ai.AiCodeGenTypeRoutingServiceFactory;
import com.eureka.flowbuilder.langgraph4j.state.WorkflowContext;
import com.eureka.flowbuilder.model.enums.CodeGenTypeEnum;
import com.eureka.flowbuilder.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class RouterNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 智能路由");

            CodeGenTypeEnum generationType;
            try {
                // 每次创建新实例，避免多线程共享单例导致的 ConcurrentModificationException
                AiCodeGenTypeRoutingServiceFactory factory = SpringContextUtil.getBean(AiCodeGenTypeRoutingServiceFactory.class);
                AiCodeGenTypeRoutingService routingService = factory.createAiCodeGenTypeRoutingService();
                generationType = routingService.routeCodeGenType(context.getOriginalPrompt()).content();
                log.info("AI智能路由完成，选择类型: {} ({})", generationType.getValue(), generationType.getText());
            } catch (Exception e) {
                log.error("AI智能路由失败，使用默认HTML类型: {}", e.getMessage());
                generationType = CodeGenTypeEnum.HTML;
            }

            // 更新状态
            context.setCurrentStep("智能路由");
            context.setGenerationType(generationType);
            return WorkflowContext.saveContext(context);
        });
    }
}

