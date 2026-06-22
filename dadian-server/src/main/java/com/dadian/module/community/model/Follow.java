package com.dadian.module.community.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("follows")
public class Follow {

    @TableField("follower_id")
    private String followerId;

    @TableField("followed_id")
    private String followedId;

    @TableField("created_at")
    private OffsetDateTime createdAt;
}
