package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addVoucher(Voucher voucher);

    void addSeckillVoucher(Voucher voucher);

    /**
     * 查询优惠券详情（秒杀券会补充库存和起止时间）
     * @param id 优惠券id
     * @return 优惠券详情
     */
    Result queryVoucherById(Long id);

    /**
     * 分页查询全部在售优惠券
     * @param type 优惠券类型，null-全部
     * @param current 页码
     * @return 优惠券列表
     */
    Result queryAllVouchers(Integer type, Integer current);
}
