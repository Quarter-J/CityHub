package com.hmdp;

import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.task.VoucherOrderCloseTask;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VoucherOrderCloseTaskTest {

    @Test
    void closesEveryExpiredOrderFromBoundedBatch() {
        VoucherOrderMapper mapper = mock(VoucherOrderMapper.class);
        IVoucherOrderService service = mock(IVoucherOrderService.class);
        when(mapper.selectExpiredOrderIds(any(LocalDateTime.class), eq(100)))
                .thenReturn(Arrays.asList(100L, 101L));
        VoucherOrderCloseTask task = new VoucherOrderCloseTask();
        ReflectionTestUtils.setField(task, "voucherOrderMapper", mapper);
        ReflectionTestUtils.setField(task, "voucherOrderService", service);
        ReflectionTestUtils.setField(task, "orderTimeoutMinutes", 15L);

        task.closeExpiredOrders();

        verify(service).closeExpiredOrder(100L);
        verify(service).closeExpiredOrder(101L);
    }
}
