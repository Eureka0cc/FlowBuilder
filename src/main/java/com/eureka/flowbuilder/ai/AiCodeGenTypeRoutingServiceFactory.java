package com.eureka.flowbuilder.ai;

import com.eureka.flowbuilder.utils.SpringContextUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * AI代码生成类型路由服务工厂
 * <p>
 * 每次调用 createAiCodeGenTypeRoutingService() 都返回全新的实例，
 * 确保多线程并发调用时不会因 langchain4j 内部的非线程安全 HashMap 而报 ConcurrentModificationException。
 *
 * @author yupi
 */
@Slf4j
@Component
@DependsOn("springContextUtil")
public class AiCodeGenTypeRoutingServiceFactory {

    /**
     * 创建全新的 AI 代码生成类型路由服务实例（非单例）
     */
    public AiCodeGenTypeRoutingService createAiCodeGenTypeRoutingService() {
        ChatModel chatModel = SpringContextUtil.getBean("routingChatModelPrototype", ChatModel.class);
        return AiServices.builder(AiCodeGenTypeRoutingService.class)
                .chatModel(chatModel)
                .build();
    }

}
