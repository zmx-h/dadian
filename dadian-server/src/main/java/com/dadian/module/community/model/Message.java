package com.dadian.module.community.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("messages")
public class Message {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("sender_id")
    private String senderId;

    @TableField("receiver_id")
    private String receiverId;

    private String content;

    @TableField("read_at")
    private OffsetDateTime readAt;

    @TableField("created_at")
    private OffsetDateTime createdAt;
}
