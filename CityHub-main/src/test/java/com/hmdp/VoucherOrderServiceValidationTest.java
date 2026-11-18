package com.hmdp;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.IVoucherService;
import com.hmdp.service.impl.VoucherOrderServiceImpl;
import com.hmdp.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VoucherOrderServiceValidationTest {

    @AfterEach
    void clearUserHolder() {
        UserHolder.removeUser();
    }

    @Test
    void rejectsSeckillBeforeItBeginsWithoutTouchingRedis() {
        IVoucherOrderService service = serviceWithWindow(
                LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusMinutes(10)
        );
        StringRedisTemplate redisTemplate = (StringRedisTemplate) ReflectionTestUtils.getField(
                service, "stringRedisTemplate");

        UserHolder.saveUser(currentUser());
        Result result = service.seckillVoucher(10L);

        assertFalse(result.getSuccess());
        verify(redisTemplate, never()).execute(any(), any(), any());
    }

    @Test
    void rejectsSeckillAfterItEnds() {
        IVoucherOrderService service = serviceWithWindow(
                LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().minusMinutes(5)
        );
        UserHolder.saveUser(currentUser());

        Result result = service.seckillVoucher(10L);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMsg().contains("结束"));
    }

    private IVoucherOrderService serviceWithWindow(LocalDateTime begin, LocalDateTime end) {
        VoucherOrderServiceImpl service = new VoucherOrderServiceImpl();
        IVoucherService voucherService = mock(IVoucherService.class);
        ISeckillVoucherService seckillVoucherService = mock(ISeckillVoucherService.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(voucherService.getById(10L)).thenReturn(new Voucher().setType(1).setStatus(1));
        when(seckillVoucherService.getById(10L))
                .thenReturn(new SeckillVoucher().setBeginTime(begin).setEndTime(end));
        ReflectionTestUtils.setField(service, "voucherService", voucherService);
        ReflectionTestUtils.setField(service, "seckillVoucherService", seckillVoucherService);
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redisTemplate);
        return service;
    }

    private UserDTO currentUser() {
        UserDTO user = new UserDTO();
        user.setId(1L);
        return user;
    }
}
