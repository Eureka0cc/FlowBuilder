package com.eureka.flowbuilder.ai.model.message;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 工具调用消息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolRequestMessage extends StreamMessage {

    private String id;

    private String name;

    private String arguments;

    /**
     * 同一次工具调用在流式分片中的序号，用于去重展示
     */
    private Integer requestIndex;

    public ToolRequestMessage(ToolExecutionRequest toolExecutionRequest) {
        this(toolExecutionRequest, null);
    }

    public ToolRequestMessage(ToolExecutionRequest toolExecutionRequest, Integer requestIndex) {
        super(StreamMessageTypeEnum.TOOL_REQUEST.getValue());
        this.id = toolExecutionRequest.id();
        this.name = toolExecutionRequest.name();
        this.arguments = toolExecutionRequest.arguments();
        this.requestIndex = requestIndex;
    }
}
