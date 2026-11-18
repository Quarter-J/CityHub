package com.hmdp.aspect;

import com.hmdp.annotation.RateLimit;
import com.hmdp.dto.UserDTO;
import com.hmdp.exception.RateLimitException;
import com.hmdp.service.RateLimitService;
import com.hmdp.utils.UserHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class RateLimitAspect {

    @Resource
    private RateLimitService rateLimitService;

    @Around("@annotation(rateLimit)")
    public Object enforce(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        if (rateLimit.windowSeconds() <= 0 || rateLimit.maxRequests() <= 0) {
            throw new IllegalArgumentException("限流窗口和阈值必须大于 0");
        }
        HttpServletRequest request = currentRequest();
        String subject = resolveSubject(rateLimit.dimension(), request);
        String path = request == null ? "unknown" : request.getRequestURI();
        String key = rateLimit.key() + ":" + path + ":" + subject;
        long windowMillis = TimeUnit.SECONDS.toMillis(rateLimit.windowSeconds());

        if (!rateLimitService.tryAcquire(key, windowMillis, rateLimit.maxRequests())) {
            throw new RateLimitException(rateLimit.message());
        }
        return joinPoint.proceed();
    }

    private String resolveSubject(RateLimit.RateLimitDimension dimension,
                                  HttpServletRequest request) {
        if (dimension == RateLimit.RateLimitDimension.GLOBAL) {
            return "global";
        }
        if (dimension == RateLimit.RateLimitDimension.USER) {
            UserDTO user = UserHolder.getUser();
            if (user != null) {
                return "user:" + user.getId();
            }
        }
        return "ip:" + clientIp(request);
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
