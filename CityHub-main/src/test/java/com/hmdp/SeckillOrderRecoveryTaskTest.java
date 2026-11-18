package com.hmdp;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.mq.SeckillOrderProducer;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.task.SeckillOrderRecoveryTask;
import com.hmdp.utils.RedisConstants;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeckillOrderRecoveryTaskTest {

    @SuppressWarnings("unchecked")
    @Test
    void republishesPendingOrderMissingFromDatabase() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hash = mock(HashOperations.class);
        IVoucherOrderService orderService = mock(IVoucherOrderService.class);
        SeckillOrderProducer producer = mock(SeckillOrderProducer.class);
        String pendingKey = RedisConstants.SECKILL_PENDING_KEY + "10";
        when(redis.keys(RedisConstants.SECKILL_PENDING_KEY + "*"))
                .thenReturn(Collections.singleton(pendingKey));
        when(redis.opsForHash()).thenReturn(hash);
        when(hash.entries(pendingKey)).thenReturn(Collections.singletonMap("7", "100"));
        when(orderService.getById(100L)).thenReturn(null);
        when(producer.send(any(VoucherOrder.class))).thenReturn(SeckillOrderProducer.SendResult.CONFIRMED);
        SeckillOrderRecoveryTask task = task(redis, orderService, producer);

        task.recoverPendingOrders();

        verify(producer).send(any(VoucherOrder.class));
    }

    @Test
    void clearsPendingMarkerWhenDatabaseOrderAlreadyExists() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hash = mock(HashOperations.class);
        IVoucherOrderService orderService = mock(IVoucherOrderService.class);
        SeckillOrderProducer producer = mock(SeckillOrderProducer.class);
        String pendingKey = RedisConstants.SECKILL_PENDING_KEY + "10";
        when(redis.keys(RedisConstants.SECKILL_PENDING_KEY + "*"))
                .thenReturn(Collections.singleton(pendingKey));
        when(redis.opsForHash()).thenReturn(hash);
        when(hash.entries(pendingKey)).thenReturn(Collections.singletonMap("7", "100"));
        when(orderService.getById(100L)).thenReturn(new VoucherOrder());
        SeckillOrderRecoveryTask task = task(redis, orderService, producer);

        task.recoverPendingOrders();

        verify(orderService).markOrderProcessed(10L, 7L, 100L);
    }

    private SeckillOrderRecoveryTask task(StringRedisTemplate redis,
                                           IVoucherOrderService orderService,
                                           SeckillOrderProducer producer) {
        SeckillOrderRecoveryTask task = new SeckillOrderRecoveryTask();
        ReflectionTestUtils.setField(task, "stringRedisTemplate", redis);
        ReflectionTestUtils.setField(task, "voucherOrderService", orderService);
        ReflectionTestUtils.setField(task, "orderProducer", producer);
        return task;
    }
}
