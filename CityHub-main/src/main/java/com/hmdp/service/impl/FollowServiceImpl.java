package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserServiceImpl userService;

    @Resource
    private IUserInfoService userInfoService;

    @Override
    @Transactional
    public Result follow(Long followUserId, Boolean isFollow) {
        //获取登录用户
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            return Result.fail("请先登录");
        }
        Long userId = currentUser.getId();
        if (userId.equals(followUserId)) {
            return Result.fail("不能关注自己");
        }
        if (userService.getById(followUserId) == null) {
            return Result.fail("目标用户不存在");
        }
        String key = "follows:" + userId;
        //1.判断关注还是取关
        if(isFollow) {
            //2.关注
            Follow follow = new Follow();
            follow.setFollowUserId(followUserId);
            follow.setUserId(userId);
            boolean inserted;
            try {
                inserted = save(follow);
            } catch (DuplicateKeyException e) {
                inserted = false;
            }
            if(inserted){
                //同步更新计数：本人关注数 +1，对方粉丝数 +1
                incrUserInfoCount(userId, true, true);
                incrUserInfoCount(followUserId, false, true);
            }
            afterCommit(() -> stringRedisTemplate.opsForSet().add(key, followUserId.toString()));
        }else {
            //3.取关
            boolean isSuccess = remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId)
                    .eq("follow_user_id", followUserId));
            if(isSuccess){
                //同步更新计数：本人关注数 -1，对方粉丝数 -1
                incrUserInfoCount(userId, true, false);
                incrUserInfoCount(followUserId, false, false);
            }
            afterCommit(() -> stringRedisTemplate.opsForSet().remove(key, followUserId.toString()));
        }
        return Result.ok();
    }

    /**
     * 更新用户关注数/粉丝数计数
     * @param userId 用户id
     * @param isFollowee true-更新关注数 followee，false-更新粉丝数 fans
     * @param isIncrement true-加一，false-减一
     */
    private void incrUserInfoCount(Long userId, boolean isFollowee, boolean isIncrement) {
        String column = isFollowee ? "followee" : "fans";
        // 确保 tb_user_info 存在该用户记录，避免 NULL 参与自增
        if (userInfoService.getById(userId) == null) {
            UserInfo info = new UserInfo();
            info.setUserId(userId);
            info.setFans(0);
            info.setFollowee(0);
            info.setCredits(0);
            try {
                userInfoService.save(info);
            } catch (DuplicateKeyException ignored) {
                // 并发初始化时由主键保证只有一条记录。
            }
        }
        // 原子更新，且结果不小于 0
        String op = isIncrement ? "+ 1" : "- 1";
        userInfoService.update(null, new UpdateWrapper<UserInfo>()
                .setSql(column + " = GREATEST(0, IFNULL(" + column + ", 0) " + op + ")")
                .eq("user_id", userId));
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        action.run();
                    } catch (RuntimeException e) {
                        log.error("关注缓存同步失败，将由后续重建或过期恢复", e);
                    }
                }
            });
        } else {
            action.run();
        }
    }

    @Override
    public Result isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        //1.查询是否关注select* from tb_follow where user_id=？ and follow_id=?
        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
            return Result.ok(count>0);

    }

    @Override
    public Result followCommons(Long id) {
        //获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;
        //求交集
        String key2 = "follows:" + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
        if(intersect==null||intersect.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        //解析出id
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());

        //查询用户
        List<UserDTO> userDTOS = userService
                .listByIds(ids).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(userDTOS);

    }

    @Override
    public Result queryMyFollows() {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        return queryFollowUsers(user.getId(), true);
    }

    @Override
    public Result queryFans(Long userId) {
        return queryFollowUsers(userId, false);
    }

    /**
     * 查询关注列表或粉丝列表
     * @param userId 目标用户id
     * @param isFollows true-查询该用户关注的人，false-查询该用户的粉丝
     * @return 用户列表
     */
    private Result queryFollowUsers(Long userId, boolean isFollows) {
        //1.查询关注关系
        String column = isFollows ? "user_id" : "follow_user_id";
        List<Follow> follows = query().eq(column, userId).list();
        if (follows == null || follows.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        //2.解析出目标用户id
        List<Long> ids = follows.stream()
                .map(f -> isFollows ? f.getFollowUserId() : f.getUserId())
                .collect(Collectors.toList());
        //3.查询用户
        List<UserDTO> users = userService.listByIds(ids).stream()
                .map(u -> BeanUtil.copyProperties(u, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(users);
    }
}
