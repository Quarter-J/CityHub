package com.hmdp.interceptor;

import com.hmdp.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (UserHolder.getUser() != null || isPublicRequest(request)) {
            return true;
        }
        response.setStatus(401);
        return false;
    }

    private boolean isPublicRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method) || path.startsWith("/uploads/")) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method)) {
            return "/user/login".equals(path) || "/user/code".equals(path);
        }
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }

        return "/user/me".equals(path)
                || path.startsWith("/user/info/")
                || path.matches("/user/\\d+")
                || path.startsWith("/shop/")
                || "/shop-type/list".equals(path)
                || path.startsWith("/voucher/")
                || path.startsWith("/uv/")
                || "/blog/hot".equals(path)
                || path.matches("/blog/\\d+")
                || path.matches("/blog/likes/\\d+")
                || "/blog/of/user".equals(path)
                || "/blog/of/shop".equals(path)
                || path.startsWith("/blog-comments/of/blog/");
    }
}
