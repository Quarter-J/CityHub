package com.hmdp;

import com.hmdp.dto.UserDTO;
import com.hmdp.interceptor.RefreshTokenInterceptor;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RefreshTokenInterceptorTest {

    @AfterEach
    void clearUserHolder() {
        UserHolder.removeUser();
    }

    @Test
    void clearsStaleUserWhenRequestHasNoToken() {
        UserHolder.saveUser(user(1L, "stale"));
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RefreshTokenInterceptor interceptor = new RefreshTokenInterceptor(redisTemplate);

        assertTrue(interceptor.preHandle(
                new MockHttpServletRequest("GET", "/shop/hot"),
                new MockHttpServletResponse(),
                new Object()
        ));
        assertNull(UserHolder.getUser());
    }

    @Test
    void clearsUserAfterRequestCompletion() {
        UserHolder.saveUser(user(2L, "current"));
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RefreshTokenInterceptor interceptor = new RefreshTokenInterceptor(redisTemplate);

        interceptor.afterCompletion(
                new MockHttpServletRequest("GET", "/user/me"),
                new MockHttpServletResponse(),
                new Object(),
                null
        );

        assertNull(UserHolder.getUser());
    }

    @SuppressWarnings("unchecked")
    @Test
    void loadsUserAndRefreshesTokenTtl() {
        String token = "token-1";
        String key = RedisConstants.LOGIN_USER_KEY + token;
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(key)).thenReturn(Collections.singletonMap("id", "3"));
        when(redisTemplate.expire(key, 30, TimeUnit.MINUTES)).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user/me");
        request.addHeader("authorization", token);
        RefreshTokenInterceptor interceptor = new RefreshTokenInterceptor(redisTemplate);

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertTrue(UserHolder.getUser() != null);
        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
        assertNull(UserHolder.getUser());
    }

    private UserDTO user(Long id, String name) {
        UserDTO user = new UserDTO();
        user.setId(id);
        user.setNickName(name);
        return user;
    }
}
