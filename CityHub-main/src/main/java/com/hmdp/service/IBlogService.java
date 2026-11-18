package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    Result queryBlogById(Long id);

    Result queryHotBlog(Integer current);

    Result updateLike(Long id);

    Result queryBlogLikes(Long id);

    Result saveBlog(Blog blog);

    Result quertBlogOfFollow(Long max, Integer offset);

    /**
     * 分页查询当前用户点过赞的笔记
     * @param current 页码
     * @return 笔记列表
     */
    Result queryLikedBlog(Integer current);

    /**
     * 分页查询商铺下的笔记
     * @param shopId 商铺id
     * @param current 页码
     * @return 笔记列表
     */
    Result queryBlogOfShop(Long shopId, Integer current);
}
