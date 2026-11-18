package com.hmdp.config;

import com.hmdp.interceptor.LoginInterceptor;
import com.hmdp.interceptor.RefreshTokenInterceptor;
import com.hmdp.interceptor.UvStatisticsInterceptor;
import com.hmdp.service.UvStatisticsService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.JwtTokenService;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private JwtTokenService jwtTokenService;
    @Resource
    private UvStatisticsService uvStatisticsService;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //登录拦截器
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**").order(1);
        registry.addInterceptor(new UvStatisticsInterceptor(uvStatisticsService))
                .addPathPatterns("/**").order(2);
        //token刷新的拦截器
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate, jwtTokenService))
                .addPathPatterns("/**").order(0);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将上传目录映射为 /uploads/**，供前端直接访问上传的图片
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + SystemConstants.IMAGE_UPLOAD_DIR.replace("\\", "/") + "/");
    }
}
