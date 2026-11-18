package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.mq.SeckillOrderProducer;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import com.hmdp.utils.VoucherOrderStatus;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder>
        implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private IVoucherService voucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SeckillOrderProducer orderProducer;

    @Resource
    private VoucherOrderMapper voucherOrderMapper;

    @Value("${order.timeout-minutes:15}")
    private long orderTimeoutMinutes;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    private static final DefaultRedisScript<Long> COMPENSATE_SCRIPT;
    private static final DefaultRedisScript<Long> MARK_PROCESSED_SCRIPT;
    private static final DefaultRedisScript<Long> RELEASE_ORDER_SCRIPT;

    static {
        SECKILL_SCRIPT = loadScript("seckill.lua");
        COMPENSATE_SCRIPT = loadScript("seckill-compensate.lua");
        MARK_PROCESSED_SCRIPT = loadScript("seckill-mark-processed.lua");
        RELEASE_ORDER_SCRIPT = loadScript("release-order.lua");
    }

    private static DefaultRedisScript<Long> loadScript(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }

        Result validation = validateSeckillVoucher(voucherId);
        if (!validation.getSuccess()) {
            return validation;
        }

        Long userId = user.getId();
        long orderId = redisIdWorker.nextId("order");
        VoucherOrder order = new VoucherOrder()
                .setId(orderId)
                .setUserId(userId)
                .setVoucherId(voucherId);

        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString(),
                String.valueOf(orderId)
        );
        int code = result == null ? -1 : result.intValue();
        if (code == -1) {
            return Result.fail("秒杀库存尚未初始化");
        }
        if (code == 1) {
            return Result.fail("库存不足");
        }
        if (code == 2) {
            return Result.fail("不能重复下单");
        }
        if (code != 0) {
            return Result.fail("秒杀请求处理失败");
        }

        SeckillOrderProducer.SendResult sendResult = orderProducer.send(order);
        if (sendResult == SeckillOrderProducer.SendResult.REJECTED) {
            compensateReservation(order);
            return Result.fail("下单消息发送失败，请重试");
        }
        if (sendResult == SeckillOrderProducer.SendResult.UNKNOWN) {
            log.warn("秒杀订单 {} 的 MQ 确认结果未知，保留 Redis 预占等待对账", orderId);
        }
        return Result.ok(orderId);
    }

    private Result validateSeckillVoucher(Long voucherId) {
        Voucher voucher = voucherService.getById(voucherId);
        if (voucher == null || voucher.getStatus() == null || voucher.getStatus() != 1) {
            return Result.fail("优惠券不存在或已下架");
        }
        if (voucher.getType() == null || voucher.getType() != 1) {
            return Result.fail("该优惠券不是秒杀券");
        }

        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if (seckillVoucher == null) {
            return Result.fail("秒杀信息不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (seckillVoucher.getBeginTime() != null && now.isBefore(seckillVoucher.getBeginTime())) {
            return Result.fail("秒杀尚未开始");
        }
        if (seckillVoucher.getEndTime() != null && now.isAfter(seckillVoucher.getEndTime())) {
            return Result.fail("秒杀已经结束");
        }
        stringRedisTemplate.opsForValue().setIfAbsent(
                RedisConstants.SECKILL_STOCK_KEY + voucherId,
                seckillVoucher.getStock().toString()
        );
        return Result.ok();
    }

    private void compensateReservation(VoucherOrder order) {
        String voucherId = order.getVoucherId().toString();
        List<String> keys = Arrays.asList(
                RedisConstants.SECKILL_STOCK_KEY + voucherId,
                RedisConstants.SECKILL_ORDER_KEY + voucherId,
                RedisConstants.SECKILL_PENDING_KEY + voucherId
        );
        try {
            stringRedisTemplate.execute(
                    COMPENSATE_SCRIPT,
                    keys,
                    order.getUserId().toString(),
                    order.getId().toString()
            );
        } catch (RuntimeException e) {
            log.error("秒杀预占补偿失败，订单 {} 需要由对账任务恢复", order.getId(), e);
        }
    }

    @Override
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        if (getById(voucherOrder.getId()) != null) {
            return;
        }

        boolean stockUpdated = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .gt("stock", 0)
                .update();
        if (!stockUpdated) {
            throw new IllegalStateException("数据库秒杀库存不足，订单 " + voucherOrder.getId());
        }

        // 主键保证同一消息只写一次，(user_id, voucher_id) 唯一索引保证一人一单。
        save(voucherOrder);
    }

    @Override
    public void markOrderProcessed(Long voucherId, Long userId, Long orderId) {
        String key = RedisConstants.SECKILL_PENDING_KEY + voucherId;
        stringRedisTemplate.execute(
                MARK_PROCESSED_SCRIPT,
                Collections.singletonList(key),
                userId.toString(),
                orderId.toString()
        );
    }

    @Override
    @Transactional
    public Result payOrder(Long orderId, Integer payType, Long voucherId) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        if (payType == null || payType < 1 || payType > 3) {
            return Result.fail("支付方式不合法");
        }

        LocalDateTime payTime = LocalDateTime.now();
        int updated = voucherOrderMapper.markPaid(orderId, user.getId(), payType, payTime);
        if (updated > 0) {
            return Result.ok();
        }

        VoucherOrder current = voucherOrderMapper.selectById(orderId);
        if (current == null) {
            return recoverPaidOrder(orderId, user.getId(), payType, voucherId, payTime);
        }
        if (!user.getId().equals(current.getUserId())) {
            return Result.fail("订单不存在");
        }
        if (current.getStatus() != null && current.getStatus() == VoucherOrderStatus.PAID) {
            return Result.ok();
        }
        if (current.getStatus() != null && current.getStatus() == VoucherOrderStatus.CANCELLED) {
            return Result.fail("订单已关闭，支付回调需要进入退款流程");
        }
        return Result.fail("订单状态已变化，支付未生效");
    }

    private Result recoverPaidOrder(Long orderId, Long userId, Integer payType,
                                    Long voucherId, LocalDateTime payTime) {
        if (voucherId == null) {
            return Result.fail("订单尚未落库，请携带优惠券 ID 重试支付回调");
        }
        String orderKey = RedisConstants.SECKILL_ORDER_KEY + voucherId;
        Object reservedOrderId = stringRedisTemplate.opsForHash().get(orderKey, userId.toString());
        if (!String.valueOf(orderId).equals(String.valueOf(reservedOrderId))) {
            return Result.fail("订单不存在或支付回调与 Redis 预占不匹配");
        }

        boolean stockUpdated = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!stockUpdated) {
            throw new IllegalStateException("数据库秒杀库存不足，无法补建支付订单 " + orderId);
        }

        VoucherOrder order = new VoucherOrder()
                .setId(orderId)
                .setUserId(userId)
                .setVoucherId(voucherId)
                .setPayType(payType)
                .setStatus(VoucherOrderStatus.PAID)
                .setPayTime(payTime);
        save(order);
        afterCommit(() -> markOrderProcessed(voucherId, userId, orderId));
        return Result.ok();
    }

    @Override
    public Result queryOrder(Long orderId) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        VoucherOrder order = voucherOrderMapper.selectById(orderId);
        if (order == null || !user.getId().equals(order.getUserId())) {
            return Result.fail("订单不存在");
        }
        return Result.ok(order);
    }

    @Override
    @Transactional
    public boolean closeExpiredOrder(Long orderId) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(orderTimeoutMinutes);
        int updated = voucherOrderMapper.cancelExpired(orderId, deadline);
        if (updated == 0) {
            return false;
        }

        VoucherOrder order = voucherOrderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalStateException("关闭订单后无法读取订单 " + orderId);
        }
        boolean stockReleased = seckillVoucherService.update()
                .setSql("stock = stock + 1")
                .eq("voucher_id", order.getVoucherId())
                .update();
        if (!stockReleased) {
            throw new IllegalStateException("释放数据库库存失败，订单 " + orderId);
        }
        afterCommit(() -> releaseRedisReservation(order));
        return true;
    }

    private void releaseRedisReservation(VoucherOrder order) {
        String voucherId = order.getVoucherId().toString();
        try {
            stringRedisTemplate.execute(
                    RELEASE_ORDER_SCRIPT,
                    Arrays.asList(
                            RedisConstants.SECKILL_STOCK_KEY + voucherId,
                            RedisConstants.SECKILL_ORDER_KEY + voucherId,
                            RedisConstants.SECKILL_PENDING_KEY + voucherId
                    ),
                    order.getUserId().toString(),
                    order.getId().toString()
            );
        } catch (RuntimeException e) {
            log.error("订单 {} 关闭后释放 Redis 库存失败，需要人工对账", order.getId(), e);
        }
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
