package com.eureka.flowbuilder.service;

import com.eureka.flowbuilder.model.dto.captcha.CaptchaVerifyRequest;
import com.eureka.flowbuilder.model.vo.CaptchaVO;

/**
 * 验证码服务
 */
public interface CaptchaService {

    /**
     * 生成验证码
     *
     * @return 验证码信息
     */
    CaptchaVO generateCaptcha();

    /**
     * 验证验证码
     *
     * @param request 验证请求
     * @return 是否验证通过
     */
    boolean verifyCaptcha(CaptchaVerifyRequest request);
}
