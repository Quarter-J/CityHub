package com.hmdp;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.impl.VoucherOrderServiceImpl;
import com.hmdp.utils.UserHolder;
import com.hmdp.utils.VoucherOrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VoucherOrderStateTest {

    @AfterEach
    void clearUser() {
        UserHolder.removeUser();
    }

    @Test
    void conditionalPaymentSucceedsForUnpaidOrder() {
        VoucherOrderMapper mapper = mock(VoucherOrderMapper.class);
        when(mapper.markPaid(eq(100L), eq(7L), eq(1), any())).thenReturn(1);
        VoucherOrderServiceImpl service = service(mapper);
        loginAs(7L);

        Result result = service.payOrder(100L, 1, null);

        assertTrue(result.getSuccess());
    }

    @Test
    void paymentLosesRaceWhenOrderWasClosed() {
        VoucherOrderMapper mapper = mock(VoucherOrderMapper.class);
        when(mapper.markPaid(eq(100L), eq(7L), eq(1), any())).thenReturn(0);
        when(mapper.selectById(100L)).thenReturn(new VoucherOrder()
                .setId(100L)
                .setUserId(7L)
                .setStatus(VoucherOrderStatus.CANCELLED));
        VoucherOrderServiceImpl service = service(mapper);
        loginAs(7L);

        Result result = service.payOrder(100L, 1, null);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMsg().contains("退款"));
    }

    @Test
    void closeDoesNothingWhenConditionalUpdateLosesRace() {
        VoucherOrderMapper mapper = mock(VoucherOrderMapper.class);
        ISeckillVoucherService seckillService = mock(ISeckillVoucherService.class);
        when(mapper.cancelExpired(eq(100L), any())).thenReturn(0);
        VoucherOrderServiceImpl service = service(mapper);
        ReflectionTestUtils.setField(service, "seckillVoucherService", seckillService);

        assertFalse(service.closeExpiredOrder(100L));
        verify(seckillService, never()).update();
    }

    private VoucherOrderServiceImpl service(VoucherOrderMapper mapper) {
        VoucherOrderServiceImpl service = new VoucherOrderServiceImpl();
        ReflectionTestUtils.setField(service, "voucherOrderMapper", mapper);
        ReflectionTestUtils.setField(service, "orderTimeoutMinutes", 15L);
        return service;
    }

    private void loginAs(Long userId) {
        UserDTO user = new UserDTO();
        user.setId(userId);
        UserHolder.saveUser(user);
    }
}
