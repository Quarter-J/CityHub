package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {

    Result follow(Long followUserId, Boolean isFollow);

    Result isFollow(Long followUserId);

    Result followCommons(Long id);

    /**
     * 查询当前用户的关注列表
     * @return 用户列表
     */
    Result queryMyFollows();

    /**
     * 查询某个用户的粉丝列表
     * @param userId 用户id
     * @return 用户列表
     */
    Result queryFans(Long userId);
}
