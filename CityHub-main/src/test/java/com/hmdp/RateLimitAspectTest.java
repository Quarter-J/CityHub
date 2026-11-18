package com.hmdp;

import com.hmdp.annotation.RateLimit;
import com.hmdp.aspect.RateLimitAspect;
import com.hmdp.exception.RateLimitException;
import com.hmdp.service.RateLimitService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitAspectTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void rejectsRequestWhenWindowIsFull() throws Throwable {
        RateLimit rateLimit = rateLimit();
        RateLimitService service = mock(RateLimitService.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/voucher-order/seckill/1");
        request.setRemoteAddr("10.0.0.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(service.tryAcquire(anyString(), eq(10_000L), eq(5L))).thenReturn(false);
        RateLimitAspect aspect = aspect(service);

        assertThrows(RateLimitException.class, () -> aspect.enforce(joinPoint, rateLimit));
        verify(joinPoint, never()).proceed();
    }

    @Test
    void proceedsWhenRequestIsAllowed() throws Throwable {
        RateLimit rateLimit = rateLimit();
        RateLimitService service = mock(RateLimitService.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/voucher-order/seckill/1");
        request.setRemoteAddr("10.0.0.8");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(service.tryAcquire(anyString(), eq(10_000L), eq(5L))).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");
        RateLimitAspect aspect = aspect(service);

        assertEquals("ok", aspect.enforce(joinPoint, rateLimit));
        verify(joinPoint).proceed();
    }

    private RateLimit rateLimit() {
        RateLimit rateLimit = mock(RateLimit.class);
        when(rateLimit.key()).thenReturn("rate:test");
        when(rateLimit.windowSeconds()).thenReturn(10L);
        when(rateLimit.maxRequests()).thenReturn(5L);
        when(rateLimit.dimension()).thenReturn(RateLimit.RateLimitDimension.IP);
        when(rateLimit.message()).thenReturn("请求过于频繁");
        return rateLimit;
    }

    private RateLimitAspect aspect(RateLimitService service) {
        RateLimitAspect aspect = new RateLimitAspect();
        ReflectionTestUtils.setField(aspect, "rateLimitService", service);
        return aspect;
    }
}
