package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.BlogComments;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogCommentsService extends IService<BlogComments> {

    /**
     * 分页查询笔记的一级评论
     * @param blogId 笔记id
     * @param current 页码
     * @return 评论列表（含用户昵称、头像）
     */
    Result queryComments(Long blogId, Integer current);

    /**
     * 发表评论，并维护笔记的评论数
     * @param comment 评论内容
     * @return 评论id
     */
    Result addComment(BlogComments comment);

    /**
     * 删除自己的评论，并维护笔记的评论数
     * @param id 评论id
     * @return 无
     */
    Result deleteComment(Long id);
}
