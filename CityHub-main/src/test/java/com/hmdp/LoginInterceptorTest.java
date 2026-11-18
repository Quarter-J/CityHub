package com.hmdp;

import com.hmdp.dto.UserDTO;
import com.hmdp.interceptor.LoginInterceptor;
import com.hmdp.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginInterceptorTest {

    private final LoginInterceptor interceptor = new LoginInterceptor();

    @AfterEach
    void clearUserHolder() {
        UserHolder.removeUser();
    }

    @Test
    void permitsPublicReadRequests() {
        assertTrue(interceptor.preHandle(
                new MockHttpServletRequest("GET", "/shop/1"),
                new MockHttpServletResponse(),
                new Object()
        ));
        assertTrue(interceptor.preHandle(
                new MockHttpServletRequest("GET", "/blog/12"),
                new MockHttpServletResponse(),
                new Object()
        ));
    }

    @Test
    void rejectsAnonymousWrites() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(
                new MockHttpServletRequest("POST", "/upload/blog"),
                response,
                new Object()
        ));
        assertEquals(401, response.getStatus());
    }

    @Test
    void rejectsPrivateFeedReads() {
        assertFalse(interceptor.preHandle(
                new MockHttpServletRequest("GET", "/blog/of/follow"),
                new MockHttpServletResponse(),
                new Object()
        ));
    }

    @Test
    void permitsAuthenticatedWrites() {
        UserDTO user = new UserDTO();
        user.setId(1L);
        user.setNickName("user");
        UserHolder.saveUser(user);
        assertTrue(interceptor.preHandle(
                new MockHttpServletRequest("POST", "/upload/blog"),
                new MockHttpServletResponse(),
                new Object()
        ));
    }
}
