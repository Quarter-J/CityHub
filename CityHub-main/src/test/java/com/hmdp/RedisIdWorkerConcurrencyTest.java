package com.hmdp;

import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisIdWorkerConcurrencyTest {

    @SuppressWarnings("unchecked")
    @Test
    void generatesUniqueIdsUnderConcurrency() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        AtomicLong counter = new AtomicLong();
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(anyString())).thenAnswer(invocation -> counter.incrementAndGet());
        RedisIdWorker worker = new RedisIdWorker();
        ReflectionTestUtils.setField(worker, "stringRedisTemplate", redis);

        int threads = 16;
        int idsPerThread = 500;
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            executor.execute(() -> {
                ready.countDown();
                try {
                    start.await();
                    for (int j = 0; j < idsPerThread; j++) {
                        ids.add(worker.nextId("order"));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS));
        executor.shutdownNow();

        assertEquals(threads * idsPerThread, ids.size());
    }
}
