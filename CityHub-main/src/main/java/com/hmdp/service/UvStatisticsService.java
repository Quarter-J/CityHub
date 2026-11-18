package com.hmdp.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
public class UvStatisticsService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void record(String scene, String visitor) {
        String key = key(scene, LocalDate.now());
        stringRedisTemplate.opsForHyperLogLog().add(key, visitor);
        stringRedisTemplate.expire(key, 2, TimeUnit.DAYS);
    }

    public long todayCount(String scene) {
        String key = key(scene, LocalDate.now());
        Long count = stringRedisTemplate.opsForHyperLogLog().size(key);
        return count == null ? 0L : count;
    }

    private String key(String scene, LocalDate date) {
        return "uv:" + DATE_FORMAT.format(date) + ":" + scene;
    }
}
