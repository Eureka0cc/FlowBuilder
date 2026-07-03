package com.eureka.flowbuilder.controller;

import com.eureka.flowbuilder.common.BaseResponse;
import com.eureka.flowbuilder.common.ResultUtils;
import com.eureka.flowbuilder.exception.BusinessException;
import com.eureka.flowbuilder.exception.ErrorCode;
import com.eureka.flowbuilder.exception.ThrowUtils;
import com.eureka.flowbuilder.model.dto.captcha.CaptchaVerifyRequest;
import com.eureka.flowbuilder.model.vo.CaptchaVO;
import com.eureka.flowbuilder.service.CaptchaService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证码接口
 */
@RestController
@RequestMapping("/user/captcha")
public class CaptchaController {

    @Resource
    private CaptchaService captchaService;

    /**
     * 获取滑动验证码
     */
    @GetMapping
    public BaseResponse<CaptchaVO> getCaptcha() {
        return ResultUtils.success(captchaService.generateCaptcha());
    }

    /**
     * 验证滑动验证码
     */
    @PostMapping("/verify")
    public BaseResponse<Boolean> verifyCaptcha(@RequestBody CaptchaVerifyRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean verified = captchaService.verifyCaptcha(request);
        if (!verified) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证失败，请重新拖动滑块");
        }
        return ResultUtils.success(true);
    }
}
