package com.hmdp;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.listener.SeckillVoucherListener;
import com.hmdp.service.IVoucherOrderService;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeckillVoucherListenerTest {

    @Test
    void processesOrderWhileHoldingUserLock() throws Exception {
        VoucherOrder order = order();
        IVoucherOrderService orderService = mock(IVoucherOrderService.class);
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(redisson.getLock("lock:order:7")).thenReturn(lock);
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        SeckillVoucherListener listener = listener(orderService, redisson);

        listener.receivedA(JSONUtil.toJsonStr(order));

        verify(orderService).createVoucherOrder(order);
        verify(orderService).markOrderProcessed(10L, 7L, 100L);
        verify(lock).unlock();
    }

    @Test
    void rejectsProcessingWhenLockCannotBeAcquired() throws Exception {
        VoucherOrder order = order();
        IVoucherOrderService orderService = mock(IVoucherOrderService.class);
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(redisson.getLock("lock:order:7")).thenReturn(lock);
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(false);
        SeckillVoucherListener listener = listener(orderService, redisson);

        assertThrows(IllegalStateException.class,
                () -> listener.receivedA(JSONUtil.toJsonStr(order)));
        verify(orderService, never()).createVoucherOrder(order);
    }

    private SeckillVoucherListener listener(IVoucherOrderService orderService,
                                             RedissonClient redisson) {
        SeckillVoucherListener listener = new SeckillVoucherListener();
        ReflectionTestUtils.setField(listener, "voucherOrderService", orderService);
        ReflectionTestUtils.setField(listener, "redissonClient", redisson);
        return listener;
    }

    private VoucherOrder order() {
        return new VoucherOrder()
                .setId(100L)
                .setUserId(7L)
                .setVoucherId(10L);
    }
}
