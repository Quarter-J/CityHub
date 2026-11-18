package com.hmdp.listener;

import cn.hutool.json.JSONUtil;
import com.hmdp.config.QueueConfig;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class SeckillVoucherListener {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @RabbitListener(queues = QueueConfig.QUEUE_A)
    public void receivedA(String message) {
        process(message, false);
    }

    @RabbitListener(queues = QueueConfig.DEAD_LETTER_QUEUE_D)
    public void receivedD(String message) {
        process(message, true);
    }

    private void process(String message, boolean finalAttempt) {
        VoucherOrder order = JSONUtil.toBean(message, VoucherOrder.class);
        try {
            voucherOrderService.createVoucherOrder(order);
            voucherOrderService.markOrderProcessed(order.getVoucherId(), order.getUserId(), order.getId());
        } catch (RuntimeException e) {
            if (finalAttempt) {
                // Redis pending 记录保留，对账任务会重新投递。
                log.error("死信队列处理订单 {} 失败，等待对账任务重试", order.getId(), e);
                return;
            }
            throw e;
        }
    }
}
