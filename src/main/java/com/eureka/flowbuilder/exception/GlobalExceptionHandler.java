package com.eureka.flowbuilder.exception;

import cn.hutool.json.JSONUtil;
import com.eureka.flowbuilder.common.BaseResponse;
import com.eureka.flowbuilder.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Map;

@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return handleSseError(e.getCode(), e.getMessage())
                ? null : ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return handleSseError(ErrorCode.SYSTEM_ERROR.getCode(), e.getMessage())
                ? null : ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    private boolean handleSseError(int errorCode, String errorMessage) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return false;
        }
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();
        String accept = request.getHeader("Accept");
        if ((accept == null || !accept.contains("text/event-stream"))
                && !request.getRequestURI().contains("/chat/gen/code")) {
            return false;
        }
        if (response == null || response.isCommitted()) {
            log.warn("SSE 响应已提交，跳过全局异常写入: {}", errorMessage);
            return true;
        }
        try {
            response.setContentType("text/event-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
            Map<String, Object> data = Map.of(
                    "error", true, "code", errorCode, "message", errorMessage);
            response.getWriter().write(
                    "event: business-error\ndata: " + JSONUtil.toJsonStr(data) + "\n\n");
            response.getWriter().write("event: done\ndata: {}\n\n");
            response.getWriter().flush();
        } catch (IOException ioException) {
            log.error("Failed to write SSE error response", ioException);
        }
        return true;
    }
}
