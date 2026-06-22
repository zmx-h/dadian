package com.dadian.module.user.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("refresh_tokens")
public class RefreshToken {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("user_id")
    private String userId;

    @TableField("token_hash")
    private String tokenHash;

    @TableField("expires_at")
    private OffsetDateTime expiresAt;

    @TableField("created_at")
    private OffsetDateTime createdAt;
}
