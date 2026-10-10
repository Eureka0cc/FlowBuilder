package com.eureka.flowbuilder.service;

import com.eureka.flowbuilder.model.entity.AiUsageLog;
import com.mybatisflex.core.service.IService;
import dev.langchain4j.model.output.TokenUsage;

public interface AiUsageLogService extends IService<AiUsageLog> {

    void recordSuccess(Long appId, Long userId, String modelName, String codeGenType,
                       String invocationType, TokenUsage tokenUsage, long durationMs);

    void recordFailure(Long appId, Long userId, String modelName, String codeGenType,
                       String invocationType, long durationMs, Throwable error);
}
