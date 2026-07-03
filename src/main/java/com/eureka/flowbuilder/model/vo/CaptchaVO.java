package com.eureka.flowbuilder.model.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 验证码响应
 */
@Data
@Builder
public class CaptchaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 验证码 ID
     */
    private String captchaId;

    /**
     * 背景图（Base64，带 data:image/png;base64, 前缀）
     */
    private String backgroundImage;

    /**
     * 滑块图（Base64，带 data:image/png;base64, 前缀）
     */
    private String sliderImage;

    /**
     * 滑块 Y 坐标（前端展示时需要用）
     */
    private Integer sliderY;

    /**
     * 有效期（秒）
     */
    private Long expiresIn;
}
