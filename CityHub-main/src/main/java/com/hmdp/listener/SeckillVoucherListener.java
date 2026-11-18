package com.hmdp.listener;

import cn.hutool.json.JSONUtil;
import com.hmdp.config.QueueConfig;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SeckillVoucherListener {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private RedissonClient redissonClient;

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
        RLock lock = redissonClient.getLock("lock:order:" + order.getUserId());
        boolean locked;
        try {
            locked = lock.tryLock(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待订单锁时被中断", e);
        }
        if (!locked) {
            throw new IllegalStateException("获取订单锁失败，订单 " + order.getId());
        }
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
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
