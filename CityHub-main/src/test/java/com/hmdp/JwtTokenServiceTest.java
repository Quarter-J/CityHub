package com.hmdp;

import com.hmdp.utils.JwtTokenService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtTokenServiceTest {

    private final JwtTokenService tokenService =
            new JwtTokenService("test-secret-with-enough-entropy-2026", 30);

    @Test
    void createsAndVerifiesSignedToken() {
        String token = tokenService.createToken(42L);

        assertEquals(42L, tokenService.parseUserId(token));
        assertEquals(42L, tokenService.parseUserId("Bearer " + token));
    }

    @Test
    void rejectsTamperedToken() {
        String token = tokenService.createToken(42L);
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertNull(tokenService.parseUserId(tampered));
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenService expiredService =
                new JwtTokenService("test-secret-with-enough-entropy-2026", -1);
        String token = expiredService.createToken(42L);

        assertNull(expiredService.parseUserId(token));
    }
}
