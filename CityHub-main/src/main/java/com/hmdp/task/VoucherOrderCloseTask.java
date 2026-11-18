package com.hmdp.task;

import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class VoucherOrderCloseTask {

    @Resource
    private VoucherOrderMapper voucherOrderMapper;

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Value("${order.timeout-minutes:15}")
    private long orderTimeoutMinutes;

    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    public void closeExpiredOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(orderTimeoutMinutes);
        List<Long> orderIds = voucherOrderMapper.selectExpiredOrderIds(deadline, 100);
        for (Long orderId : orderIds) {
            try {
                voucherOrderService.closeExpiredOrder(orderId);
            } catch (RuntimeException e) {
                log.error("关闭超时订单 {} 失败，下轮继续重试", orderId, e);
            }
        }
    }
}
