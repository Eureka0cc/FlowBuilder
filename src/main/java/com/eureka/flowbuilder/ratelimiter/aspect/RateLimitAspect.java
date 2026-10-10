package com.eureka.flowbuilder.ratelimiter.aspect;

import com.eureka.flowbuilder.config.BillingProperties;
import com.eureka.flowbuilder.exception.BusinessException;
import com.eureka.flowbuilder.exception.ErrorCode;
import com.eureka.flowbuilder.model.entity.User;
import com.eureka.flowbuilder.ratelimiter.annotation.RateLimit;
import com.eureka.flowbuilder.service.CreditService;
import com.eureka.flowbuilder.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;

@Aspect
@Component
@Slf4j
public class RateLimitAspect {
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private UserService userService;
    @Resource
    private CreditService creditService;

    @Before("@annotation(rateLimit)")
    public void doBefore(JoinPoint point, RateLimit rateLimit) {
        RateParams rateParams = resolveRateParams(point, rateLimit);
        String key = rateParams.key();
        // 使用Redisson的分布式限流器
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.expire(Duration.ofHours(1)); // 1 小时后过期
        // 设置限流器参数：每个时间窗口允许的请求数和时间窗口
        rateLimiter.trySetRate(RateType.OVERALL, rateParams.rate(), rateParams.rateInterval(), RateIntervalUnit.SECONDS);
        // 尝试获取令牌，如果获取失败则限流
        if (!rateLimiter.tryAcquire(1)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS_ERROR, rateLimit.message());
        }
    }

    private RateParams resolveRateParams(JoinPoint point, RateLimit rateLimit) {
        int rate = rateLimit.rate();
        int rateInterval = rateLimit.rateInterval();
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("rate_limit:");
        if (!rateLimit.key().isEmpty()) {
            keyBuilder.append(rateLimit.key()).append(":");
        }
        switch (rateLimit.limitType()) {
            case API -> {
                MethodSignature signature = (MethodSignature) point.getSignature();
                Method method = signature.getMethod();
                keyBuilder.append("api:").append(method.getDeclaringClass().getSimpleName())
                        .append(".").append(method.getName());
            }
            case USER -> {
                try {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        HttpServletRequest request = attributes.getRequest();
                        User loginUser = userService.getLoginUser(request);
                        BillingProperties.PlanConfig plan = creditService.resolvePlan(loginUser.getPlanCode());
                        rate = plan.getChatRate();
                        rateInterval = plan.getChatRateIntervalSeconds();
                        keyBuilder.append("user:").append(loginUser.getId())
                                .append(":plan:").append(plan.getChatRate());
                    } else {
                        keyBuilder.append("ip:").append(getClientIP());
                    }
                } catch (BusinessException e) {
                    keyBuilder.append("ip:").append(getClientIP());
                }
            }
            case IP -> keyBuilder.append("ip:").append(getClientIP());
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的限流类型");
        }
        return new RateParams(keyBuilder.toString(), rate, rateInterval);
    }

    private String getClientIP() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }

    private record RateParams(String key, int rate, int rateInterval) {
    }
}
