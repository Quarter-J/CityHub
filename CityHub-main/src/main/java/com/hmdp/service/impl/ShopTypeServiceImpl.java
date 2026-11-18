package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.hmdp.utils.RedisConstants;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType>
        implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public Result querySort() {
        List<ShopType> cached = readCachedTypes();
        if (cached != null) {
            return Result.ok(cached);
        }

        RLock lock = redissonClient.getLock("lock:shop-type");
        boolean locked = false;
        try {
            locked = lock.tryLock(2, 10, TimeUnit.SECONDS);
            if (!locked) {
                return Result.ok(loadFromDatabase());
            }
            cached = readCachedTypes();
            if (cached != null) {
                return Result.ok(cached);
            }

            List<ShopType> shopTypes = loadFromDatabase();
            if (shopTypes.isEmpty()) {
                return Result.fail("没有分类数据");
            }
            List<String> values = shopTypes.stream()
                    .map(JSONUtil::toJsonStr)
                    .collect(Collectors.toList());
            stringRedisTemplate.delete(RedisConstants.SHOP_TYPE_KEY);
            stringRedisTemplate.opsForList().rightPushAll(RedisConstants.SHOP_TYPE_KEY, values);
            stringRedisTemplate.expire(RedisConstants.SHOP_TYPE_KEY,
                    RedisConstants.SHOP_TYPE_LONG, TimeUnit.MINUTES);
            return Result.ok(shopTypes);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.ok(loadFromDatabase());
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private List<ShopType> readCachedTypes() {
        List<String> values = stringRedisTemplate.opsForList()
                .range(RedisConstants.SHOP_TYPE_KEY, 0, -1);
        if (values == null || values.isEmpty()) {
            return null;
        }
        stringRedisTemplate.expire(RedisConstants.SHOP_TYPE_KEY,
                RedisConstants.SHOP_TYPE_LONG, TimeUnit.MINUTES);
        return values.stream()
                .map(value -> JSONUtil.toBean(value, ShopType.class))
                .distinct()
                .collect(Collectors.toList());
    }

    private List<ShopType> loadFromDatabase() {
        return query().orderByAsc("sort").list();
    }
}
