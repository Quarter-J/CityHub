package com.hmdp.mapper;

import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {

    @Update("UPDATE tb_voucher_order SET status = 2, pay_type = #{payType}, "
            + "pay_time = #{payTime}, update_time = #{payTime} "
            + "WHERE id = #{id} AND user_id = #{userId} AND status = 1")
    int markPaid(@Param("id") Long id,
                 @Param("userId") Long userId,
                 @Param("payType") Integer payType,
                 @Param("payTime") LocalDateTime payTime);

    @Update("UPDATE tb_voucher_order SET status = 4, update_time = NOW() "
            + "WHERE id = #{id} AND status = 1 AND create_time <= #{deadline}")
    int cancelExpired(@Param("id") Long id,
                      @Param("deadline") LocalDateTime deadline);

    @Select("SELECT id FROM tb_voucher_order "
            + "WHERE status = 1 AND create_time <= #{deadline} "
            + "ORDER BY create_time LIMIT #{limit}")
    List<Long> selectExpiredOrderIds(@Param("deadline") LocalDateTime deadline,
                                     @Param("limit") int limit);
}
