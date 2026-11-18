package com.hmdp;

import com.hmdp.service.UvStatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HyperLogLogOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UvStatisticsServiceTest {

    @SuppressWarnings("unchecked")
    @Test
    void recordsVisitorsInHyperLogLogWithTwoDayTtl() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        HyperLogLogOperations<String, String> hll = mock(HyperLogLogOperations.class);
        when(redis.opsForHyperLogLog()).thenReturn(hll);
        when(hll.size(anyString())).thenReturn(12L);
        UvStatisticsService service = new UvStatisticsService();
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redis);

        service.record("site", "user:7");

        verify(hll).add(anyString(), eq("user:7"));
        verify(redis).expire(anyString(), eq(2L), eq(TimeUnit.DAYS));
        assertEquals(12L, service.todayCount("site"));
    }
}
