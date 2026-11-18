package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenService {

    private final byte[] signingKey;
    private final long ttlMinutes;

    public JwtTokenService(@Value("${auth.jwt.secret}") String secret,
                           @Value("${auth.jwt.ttl-minutes:30}") long ttlMinutes) {
        this.signingKey = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlMinutes = ttlMinutes;
    }

    public String createToken(Long userId) {
        Date now = new Date();
        JWT jwt = JWT.create()
                .setPayload("userId", userId)
                .setPayload("jti", UUID.randomUUID().toString(true))
                .setIssuedAt(now)
                .setExpiresAt(new Date(now.getTime() + ttlMinutes * 60_000L));
        jwt.setKey(signingKey);
        return jwt.sign();
    }

    public Long parseUserId(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        String rawToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        try {
            JWT jwt = JWT.of(rawToken).setKey(signingKey);
            if (!jwt.verify()) {
                return null;
            }
            JWTValidator.of(jwt).validateDate();
            Object userId = jwt.getPayload("userId");
            return userId == null ? null : Long.valueOf(String.valueOf(userId));
        } catch (RuntimeException e) {
            return null;
        }
    }

    public long getTtlMinutes() {
        return ttlMinutes;
    }
}
