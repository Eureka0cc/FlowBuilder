package com.eureka.flowbuilder.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Vue 工程预览构建状态
 */
@Data
public class VueBuildStatusVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 构建状态：none / building / success / failed
     */
    private String status;

    /**
     * 预览是否就绪（dist 中已包含打包后的 JS 资源）
     */
    private Boolean ready;

    /**
     * 状态说明或错误信息
     */
    private String message;

    /**
     * 状态更新时间戳（毫秒）
     */
    private Long timestamp;
}
