package com.hmdp.mq;

import cn.hutool.json.JSONUtil;
import com.hmdp.config.QueueConfig;
import com.hmdp.entity.VoucherOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class SeckillOrderProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        rabbitTemplate.setMandatory(true);
    }

    public enum SendResult {
        CONFIRMED,
        REJECTED,
        UNKNOWN
    }

    public SendResult send(VoucherOrder order) {
        CorrelationData correlationData = new CorrelationData(order.getId().toString());
        try {
            rabbitTemplate.convertAndSend(
                    QueueConfig.X_EXCHANGE,
                    "XA",
                    JSONUtil.toJsonStr(order),
                    correlationData
            );
            CorrelationData.Confirm confirm = correlationData.getFuture().get(3, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                log.error("RabbitMQ 未确认秒杀订单 {}：{}", order.getId(), confirm.getReason());
                return SendResult.REJECTED;
            }
            if (correlationData.getReturnedMessage() != null) {
                log.error("RabbitMQ 秒杀订单 {} 无法路由，message={}",
                        order.getId(), correlationData.getReturnedMessage());
                return SendResult.REJECTED;
            }
            return SendResult.CONFIRMED;
        } catch (TimeoutException e) {
            // 超时属于结果未知，不能贸然恢复库存，否则已入队消息可能继续落库。
            log.warn("等待 RabbitMQ 确认超时，订单 {} 由 pending 对账继续处理", order.getId());
            return SendResult.UNKNOWN;
        } catch (AmqpException e) {
            log.error("RabbitMQ 拒绝秒杀订单 {}", order.getId(), e);
            return SendResult.REJECTED;
        } catch (Exception e) {
            log.error("发送秒杀订单 {} 失败", order.getId(), e);
            return SendResult.UNKNOWN;
        }
    }
}
