package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.BlogComments;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogCommentsMapper;
import com.hmdp.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

    @Resource
    private IUserService userService;

    @Resource
    private IBlogService blogService;

    @Override
    public Result queryComments(Long blogId, Integer current) {
        //1.分页查询一级评论
        Page<BlogComments> page = query()
                .eq("blog_id", blogId)
                .eq("parent_id", 0)
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        //2.查询评论用户信息
        List<BlogComments> records = page.getRecords();
        records.forEach(this::queryCommentUser);
        return Result.ok(records, page.getTotal());
    }

    @Override
    @Transactional
    public Result addComment(BlogComments comment) {
        //1.校验登录
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        //2.校验参数
        if (comment.getBlogId() == null) {
            return Result.fail("笔记id不能为空");
        }
        if (StrUtil.isBlank(comment.getContent())) {
            return Result.fail("评论内容不能为空");
        }
        if (blogService.getById(comment.getBlogId()) == null) {
            return Result.fail("笔记不存在");
        }
        comment.setUserId(user.getId());
        comment.setLiked(0);
        if (comment.getParentId() == null) {
            comment.setParentId(0L);
        }
        if (comment.getAnswerId() == null) {
            comment.setAnswerId(0L);
        }
        //3.保存评论
        boolean isSuccess = save(comment);
        if (!isSuccess) {
            return Result.fail("评论失败");
        }
        //4.笔记评论数+1
        boolean counted = blogService.update()
                .setSql("comments = comments + 1")
                .eq("id", comment.getBlogId())
                .update();
        if (!counted) {
            throw new IllegalStateException("更新笔记评论数失败");
        }
        //5.补充用户信息返回
        queryCommentUser(comment);
        return Result.ok(comment);
    }

    @Override
    @Transactional
    public Result deleteComment(Long id) {
        //1.校验登录
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        //2.只能删除自己的评论
        BlogComments comment = getById(id);
        if (comment == null) {
            return Result.fail("评论不存在");
        }
        if (!comment.getUserId().equals(user.getId())) {
            return Result.fail("只能删除自己的评论");
        }
        //3.删除并维护评论数
        boolean isSuccess = removeById(id);
        if (isSuccess) {
            blogService.update().setSql("comments = comments - 1")
                    .eq("id", comment.getBlogId())
                    .gt("comments", 0)
                    .update();
        }
        return Result.ok();
    }

    private void queryCommentUser(BlogComments comment) {
        User user = userService.getById(comment.getUserId());
        if (user != null) {
            comment.setName(user.getNickName());
            comment.setIcon(user.getIcon());
        }
    }
}
