package com.eureka.flowbuilder.model.dto.app;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppAddRequest implements Serializable {

    /**
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 所属团队；为空时使用或创建个人团队
     */
    private Long teamId;

    private static final long serialVersionUID = 1L;
}
