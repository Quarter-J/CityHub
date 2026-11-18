package com.hmdp.task;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.mq.SeckillOrderProducer;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class SeckillOrderRecoveryTask {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private SeckillOrderProducer orderProducer;

    @Scheduled(initialDelay = 30_000, fixedDelay = 60_000)
    public void recoverPendingOrders() {
        Set<String> keys = stringRedisTemplate.keys(RedisConstants.SECKILL_PENDING_KEY + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            String voucherId = key.substring(RedisConstants.SECKILL_PENDING_KEY.length());
            Map<Object, Object> pending = stringRedisTemplate.opsForHash().entries(key);
            for (Map.Entry<Object, Object> entry : pending.entrySet()) {
                Long userId = parseLong(entry.getKey());
                Long orderId = parseLong(entry.getValue());
                if (userId == null || orderId == null) {
                    log.error("秒杀待处理订单格式错误：key={}, field={}, value={}",
                            key, entry.getKey(), entry.getValue());
                    continue;
                }
                VoucherOrder order = new VoucherOrder()
                        .setId(orderId)
                        .setUserId(userId)
                        .setVoucherId(Long.valueOf(voucherId));
                if (voucherOrderService.getById(orderId) != null) {
                    voucherOrderService.markOrderProcessed(order.getVoucherId(), userId, orderId);
                } else if (orderProducer.send(order) == SeckillOrderProducer.SendResult.CONFIRMED) {
                    log.info("已重新投递未落库秒杀订单 {}", orderId);
                }
            }
        }
    }

    private Long parseLong(Object value) {
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
