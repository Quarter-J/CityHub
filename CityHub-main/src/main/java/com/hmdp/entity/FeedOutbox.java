package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_feed_outbox")
public class FeedOutbox {

    @TableId(value = "blog_id", type = IdType.INPUT)
    private Long blogId;

    private LocalDateTime createTime;
}
