package com.hmdp.interceptor;

import com.hmdp.dto.UserDTO;
import com.hmdp.service.UvStatisticsService;
import com.hmdp.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UvStatisticsInterceptor implements HandlerInterceptor {

    private final UvStatisticsService uvStatisticsService;

    public UvStatisticsInterceptor(UvStatisticsService uvStatisticsService) {
        this.uvStatisticsService = uvStatisticsService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        UserDTO user = UserHolder.getUser();
        String visitor = user == null
                ? "ip:" + clientIp(request)
                : "user:" + user.getId();
        uvStatisticsService.record("site", visitor);
        return true;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
