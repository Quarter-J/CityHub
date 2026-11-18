package com.hmdp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    String key();

    long windowSeconds() default 60;

    long maxRequests() default 60;

    RateLimitDimension dimension() default RateLimitDimension.IP;

    String message() default "请求过于频繁，请稍后重试";

    enum RateLimitDimension {
        GLOBAL,
        USER,
        IP
    }
}
