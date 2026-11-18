package com.hmdp.task;

import com.hmdp.entity.Blog;
import com.hmdp.entity.FeedOutbox;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FeedOutboxMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IFollowService;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
public class FeedOutboxRecoveryTask {

    @Resource
    private FeedOutboxMapper feedOutboxMapper;

    @Resource
    private IBlogService blogService;

    @Resource
    private IFollowService followService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Scheduled(initialDelay = 20_000, fixedDelay = 30_000)
    public void recoverFeedEvents() {
        List<FeedOutbox> events = feedOutboxMapper.selectList(null);
        for (FeedOutbox event : events) {
            Blog blog = blogService.getById(event.getBlogId());
            if (blog == null) {
                feedOutboxMapper.deleteById(event.getBlogId());
                continue;
            }
            try {
                List<Follow> follows = followService.query()
                        .eq("follow_user_id", blog.getUserId())
                        .list();
                long score = blog.getCreateTime() == null
                        ? System.currentTimeMillis()
                        : blog.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                for (Follow follow : follows) {
                    stringRedisTemplate.opsForZSet().add(
                            RedisConstants.FEED_KEY + follow.getUserId(),
                            blog.getId().toString(),
                            score
                    );
                }
                feedOutboxMapper.deleteById(event.getBlogId());
            } catch (RuntimeException e) {
                log.error("恢复 Feed 事件 {} 失败，等待下次重试", event.getBlogId(), e);
            }
        }
    }
}
