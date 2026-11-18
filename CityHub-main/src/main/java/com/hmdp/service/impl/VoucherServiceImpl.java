package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IShopService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IShopService shopService;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addVoucher(Voucher voucher) {
        voucher.setType(0);
        if (!save(voucher)) {
            throw new IllegalStateException("新增普通优惠券失败");
        }
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        if (voucher.getStock() == null || voucher.getStock() < 0) {
            throw new IllegalArgumentException("秒杀库存不能为空且不能小于 0");
        }
        if (voucher.getBeginTime() == null || voucher.getEndTime() == null) {
            throw new IllegalArgumentException("秒杀开始和结束时间不能为空");
        }
        if (!voucher.getBeginTime().isBefore(voucher.getEndTime())) {
            throw new IllegalArgumentException("秒杀开始时间必须早于结束时间");
        }
        voucher.setType(1);
        // 保存优惠券
        if (!save(voucher)) {
            throw new IllegalStateException("新增秒杀优惠券失败");
        }
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        if (!seckillVoucherService.save(seckillVoucher)) {
            throw new IllegalStateException("新增秒杀信息失败");
        }
        Runnable seedStock = () -> stringRedisTemplate.opsForValue()
                .set(SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    seedStock.run();
                }
            });
        } else {
            seedStock.run();
        }
    }

    @Override
    public Result queryVoucherById(Long id) {
        Voucher voucher = getById(id);
        if (voucher == null || voucher.getStatus() == null || voucher.getStatus() != 1) {
            return Result.fail("优惠券不存在或已下架");
        }
        fillSeckillAndShop(voucher);
        return Result.ok(voucher);
    }

    @Override
    public Result queryAllVouchers(Integer type, Integer current) {
        Page<Voucher> page = query()
                .eq("status", 1)
                .eq(type != null, "type", type)
                .orderByDesc("id")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        page.getRecords().forEach(this::fillSeckillAndShop);
        return Result.ok(page.getRecords(), page.getTotal());
    }

    /**
     * 补充秒杀券的库存、起止时间，以及商铺名称
     */
    private void fillSeckillAndShop(Voucher voucher) {
        if (voucher.getType() != null && voucher.getType() == 1) {
            SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucher.getId());
            if (seckillVoucher != null) {
                voucher.setStock(seckillVoucher.getStock());
                voucher.setBeginTime(seckillVoucher.getBeginTime());
                voucher.setEndTime(seckillVoucher.getEndTime());
            }
        }
        Shop shop = shopService.getById(voucher.getShopId());
        if (shop != null) {
            voucher.setShopName(shop.getName());
        }
    }
}
