package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result sign();

    Result signCount();

    /**
     * 查询今日是否已签到
     * @return data 为 true 表示今日已签到
     */
    Result signStatus();

    /**
     * 登出：删除redis中的token
     * @param token 请求头中的authorization
     * @return 无
     */
    Result logout(String token);

    /**
     * 修改当前用户的昵称、头像
     * @param user 新的昵称、头像
     * @param token 请求头中的authorization
     * @return 无
     */
    Result updateUser(User user, String token);

    /**
     * 修改当前用户的个人资料（城市、简介、性别、生日）
     * @param userInfo 新的资料
     * @return 无
     */
    Result updateUserInfo(UserInfo userInfo);
}
