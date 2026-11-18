package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.BlogLike;
import com.hmdp.entity.Follow;
import com.hmdp.entity.FeedOutbox;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.mapper.BlogLikeMapper;
import com.hmdp.mapper.FeedOutboxMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.hmdp.utils.RedisConstants.FEED_KEY;
import static com.hmdp.utils.RedisConstants.USER_LIKED_KEY;

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
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private IFollowService followService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private BlogLikeMapper blogLikeMapper;

    @Resource
    private FeedOutboxMapper feedOutboxMapper;
    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.isBlogLiked(blog);
            this.queryBlogUser(blog);
        });
        return Result.ok(records, page.getTotal());
    }

    @Override
    @Transactional
    public Result updateLike(Long id){
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        Long userId = user.getId();
        String key = BLOG_LIKED_KEY + id;
        BlogLike existing = blogLikeMapper.selectOne(new LambdaQueryWrapper<BlogLike>()
                .eq(BlogLike::getBlogId, id)
                .eq(BlogLike::getUserId, userId));

        if (existing == null) {
            BlogLike like = new BlogLike().setBlogId(id).setUserId(userId);
            try {
                blogLikeMapper.insert(like);
            } catch (DuplicateKeyException e) {
                return Result.ok();
            }
            boolean updated = update().setSql("liked = liked + 1").eq("id", id).update();
            if (!updated) {
                throw new IllegalStateException("笔记不存在，点赞失败");
            }
            afterCommit(() -> {
                long now = System.currentTimeMillis();
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), now);
                stringRedisTemplate.opsForZSet().add(USER_LIKED_KEY + userId, id.toString(), now);
            });
        } else {
            int deleted = blogLikeMapper.delete(new LambdaQueryWrapper<BlogLike>()
                    .eq(BlogLike::getBlogId, id)
                    .eq(BlogLike::getUserId, userId));
            if (deleted > 0) {
                update().setSql("liked = liked - 1")
                        .eq("id", id)
                        .gt("liked", 0)
                        .update();
            }
            afterCommit(() -> {
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
                stringRedisTemplate.opsForZSet().remove(USER_LIKED_KEY + userId, id.toString());
            });
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        //1.查询top5的点赞用户
        String key=BLOG_LIKED_KEY+id;
        Set<String> top5 = stringRedisTemplate.opsForZSet().reverseRange(key, 0, 4);
        if(top5==null||top5.isEmpty()){
            List<BlogLike> databaseLikes = blogLikeMapper.selectList(
                    new LambdaQueryWrapper<BlogLike>()
                            .eq(BlogLike::getBlogId, id)
                            .orderByDesc(BlogLike::getCreateTime)
                            .last("limit 5"));
            if (databaseLikes.isEmpty()) {
                return Result.ok(Collections.emptyList());
            }
            List<Long> userIds = databaseLikes.stream()
                    .map(BlogLike::getUserId)
                    .collect(Collectors.toList());
            return Result.ok(userService.listByIds(userIds).stream()
                    .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                    .collect(Collectors.toList()));
        }
        //2.解析出其中的id
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",",ids);
        //3.根据用户id查询用户
        //SELECT *from tb_user where id IN(5,1) ORDER BY FIELD(id,5,1)
        List<UserDTO> userDTOS = userService.query()
                .in("id",ids).last("order by field(id,"+idStr+")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        //4.返回
        return Result.ok(userDTOS);

    }

    @Override
    @Transactional
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean isSuccess = save(blog);
        if(!isSuccess){
           return Result.fail("新增笔记失败");
        }
        FeedOutbox outbox = new FeedOutbox();
        outbox.setBlogId(blog.getId());
        feedOutboxMapper.insert(outbox);
        //查询笔记作者的所有粉丝 select* from tb_follow where follow_user_id=?
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        // 事务提交后再写收件箱，避免数据库回滚但粉丝已经收到 Feed。
        afterCommit(() -> {
            long score = blog.getCreateTime() == null
                    ? System.currentTimeMillis()
                    : blog.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            for (Follow follow : follows) {
                String key = FEED_KEY + follow.getUserId();
                stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), score);
            }
            feedOutboxMapper.deleteById(blog.getId());
        });
        // 返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result quertBlogOfFollow(Long max, Integer offset) {
        //1.获取当前用户
        Long userId = UserHolder.getUser().getId();
        //2.查询收件箱  zrevrangebyscore key  min max limit offset count
        String key=FEED_KEY+userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        if(typedTuples==null||typedTuples.isEmpty()){
            return Result.ok();
        }
        //3.解析数据：blogId,minTime时间戳),offset
        List<Long> ids=new ArrayList<>(typedTuples.size());
        long minTime=0;
        int os=1;
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            //3.1.获取id
            ids.add(Long.valueOf( typedTuple.getValue()));
            //3.2.获取分数（时间戳）
            long time=typedTuple.getScore().longValue();
            if(time==minTime){
                os++;
            }else {
                minTime = time;
                os = 1;
            }

        }
        //4.根据id查询blog
        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query()
                .in("id", ids).last("order by field(id," + idStr + ")").list();

        for (Blog blog : blogs) {
           //4.1.查询blog有关的用户
            queryBlogUser(blog);
            //4.2查询blog是否点赞
            isBlogLiked(blog);
        }
        //5.封装并返回
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(os);
        scrollResult.setMinTime(minTime);
        return Result.ok(scrollResult);

    }

    @Override
    public Result  queryBlogById(Long id) {
        //1.查询blog
        Blog blog = getById(id);
        //2.查询blog关联的对象
        if(blog==null){
            return Result.fail("博客不存在");
        }
        queryBlogUser(blog);
        //3.查询blog是否点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    @Override
    public Result queryLikedBlog(Integer current) {
        //1.获取当前用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        Long userId = user.getId();
        Page<BlogLike> likePage = blogLikeMapper.selectPage(
                new Page<>(current, SystemConstants.MAX_PAGE_SIZE),
                new LambdaQueryWrapper<BlogLike>()
                        .eq(BlogLike::getUserId, userId)
                        .orderByDesc(BlogLike::getCreateTime));
        if (likePage.getRecords().isEmpty()) {
            return Result.ok(Collections.emptyList(), 0L);
        }
        List<Long> pageIds = likePage.getRecords().stream()
                .map(BlogLike::getBlogId)
                .collect(Collectors.toList());
        String idStr = StrUtil.join(",", pageIds);
        List<Blog> blogs = query()
                .in("id", pageIds).last("order by field(id," + idStr + ")").list();
        blogs.forEach(blog -> {
            queryBlogUser(blog);
            isBlogLiked(blog);
        });
        return Result.ok(blogs, likePage.getTotal());
    }

    @Override
    public Result queryBlogOfShop(Long shopId, Integer current) {
        //1.分页查询商铺下的笔记
        Page<Blog> page = query()
                .eq("shop_id", shopId)
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        //2.查询用户信息与点赞状态
        page.getRecords().forEach(blog -> {
            queryBlogUser(blog);
            isBlogLiked(blog);
        });
        return Result.ok(page.getRecords(), page.getTotal());
    }

    private void isBlogLiked(Blog blog) {
        //1.获取当前用户
        UserDTO user = UserHolder.getUser();
        if(user==null){
            //用户未登录，无需查询是否点赞
            return;
        }
        Long userId = user.getId();

        //2.判断当前用户有没有点赞
        String key=BLOG_LIKED_KEY+blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        boolean liked = score != null || blogLikeMapper.selectCount(
                new LambdaQueryWrapper<BlogLike>()
                        .eq(BlogLike::getBlogId, blog.getId())
                        .eq(BlogLike::getUserId, userId)) > 0;
        blog.setIsLike(liked);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        action.run();
                    } catch (RuntimeException e) {
                        log.error("Redis 缓存或 Feed 同步失败，将由过期或后续重建恢复", e);
                    }
                }
            });
        } else {
            action.run();
        }
    }

}
