package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

    void createVoucherOrder(VoucherOrder voucherOrder);

    void markOrderProcessed(Long voucherId, Long userId, Long orderId);

    Result payOrder(Long orderId, Integer payType, Long voucherId);

    Result queryOrder(Long orderId);

    boolean closeExpiredOrder(Long orderId);
}
