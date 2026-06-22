package com.dadian.module.outing.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("comments")
public class Comment {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String userId;
    private String memoryId;
    private String content;
    private OffsetDateTime createdAt;
    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private Integer chargeCount;
    @TableField(exist = false)
    private Boolean chargedByMe;
}
