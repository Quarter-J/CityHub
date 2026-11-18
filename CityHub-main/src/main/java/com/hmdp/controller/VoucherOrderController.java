package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.annotation.RateLimit;
import com.hmdp.service.IVoucherOrderService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    @Resource
    private IVoucherOrderService voucherOrderService;
    @RateLimit(key = "rate:seckill", windowSeconds = 10, maxRequests = 5,
            dimension = RateLimit.RateLimitDimension.IP)
    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }

    @PostMapping("/pay/{id}")
    public Result payOrder(@PathVariable("id") Long orderId,
                           @RequestParam(value = "payType", defaultValue = "1") Integer payType) {
        return voucherOrderService.payOrder(orderId, payType);
    }

    @GetMapping("/{id}")
    public Result queryOrder(@PathVariable("id") Long orderId) {
        return voucherOrderService.queryOrder(orderId);
    }
}
