package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;
    private final Cache<String, Object> caffeineCache;

    public CacheClient(StringRedisTemplate stringRedisTemplate, Cache<String, Object> caffeineCache) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.caffeineCache = caffeineCache;
    }

    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    // ==================== 单级缓存（Redis） ====================

    public <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }
        if (json != null) {
            return null;
        }
        R r = dbFallback.apply(id);
        if (r == null) {
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        this.set(key, r, time, unit);
        return r;
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)) {
            return null;
        }
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R shop = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        if (expireTime.isAfter(LocalDateTime.now())) {
            return shop;
        }
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        if (isLock) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    R r1 = dbFallback.apply(id);
                    this.setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(key);
                }
            });
        }
        return shop;
    }

    // ==================== 两级缓存（Caffeine L1 + Redis L2） ====================

    /**
     * 两级缓存查询（缓存穿透模式）
     * 查询顺序：Caffeine -> Redis -> DB
     * 回填策略：DB结果回填 Redis 和 Caffeine，空值回填 Caffeine 短期缓存防穿透
     */
    public <R, ID> R queryTwoLevel(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long redisTime, TimeUnit unit) {
        String key = keyPrefix + id;

        // 1. 查 L1 Caffeine
        Object caffeineValue = caffeineCache.getIfPresent(key);
        if (caffeineValue != null) {
            // 用空字符串标记空值
            if ("".equals(caffeineValue)) {
                return null;
            }
            return type.cast(caffeineValue);
        }

        // 2. 查 L2 Redis
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            R r = JSONUtil.toBean(json, type);
            // 回填 L1
            caffeineCache.put(key, r);
            return r;
        }
        if (json != null) {
            // Redis 空值标记，回填 L1 短期标记
            caffeineCache.put(key, "");
            return null;
        }

        // 3. 查 DB
        R r = dbFallback.apply(id);
        if (r == null) {
            // 空值回填 Redis（2分钟）+ Caffeine（1分钟）
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            caffeineCache.put(key, "");
            return null;
        }

        // 4. 回填两级缓存
        this.set(key, r, redisTime, unit);
        caffeineCache.put(key, r);
        return r;
    }

    /**
     * 两级缓存查询（逻辑过期模式）
     * 查询顺序：Caffeine -> Redis -> DB（异步重建）
     */
    public <R, ID> R queryTwoLevelWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;

        // 1. 查 L1 Caffeine（逻辑过期数据直接存对象）
        Object caffeineValue = caffeineCache.getIfPresent(key);
        if (caffeineValue != null) {
            if ("".equals(caffeineValue)) {
                return null;
            }
            return type.cast(caffeineValue);
        }

        // 2. 查 L2 Redis
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)) {
            return null;
        }

        // 3. 反序列化逻辑过期数据
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R result = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();

        // 回填 L1（用剩余 TTL 时间）
        caffeineCache.put(key, result);

        // 4. 判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            return result;
        }

        // 5. 已过期，异步重建
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        if (isLock) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    R r1 = dbFallback.apply(id);
                    this.setWithLogicalExpire(key, r1, time, unit);
                    caffeineCache.put(key, r1);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(key);
                }
            });
        }
        return result;
    }

    /**
     * 两级缓存失效（更新/删除数据时调用）
     */
    public void invalidateTwoLevel(String keyPrefix, Object id) {
        String key = keyPrefix + id;
        // 先删 Redis，再清 Caffeine
        stringRedisTemplate.delete(key);
        caffeineCache.invalidate(key);
    }

    // ==================== 锁工具 ====================

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }
}