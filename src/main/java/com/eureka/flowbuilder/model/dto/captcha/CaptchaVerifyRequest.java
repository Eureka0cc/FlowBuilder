package com.eureka.flowbuilder.model.dto.captcha;

import lombok.Data;

import java.io.Serializable;

/**
 * 验证码验证请求
 */
@Data
public class CaptchaVerifyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 验证码 ID
     */
    private String captchaId;

    /**
     * 滑块 X 坐标
     */
    private Integer sliderX;
}
