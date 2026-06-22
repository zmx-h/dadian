package com.dadian.module.outing.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("comment_charges")
public class CommentCharge {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String commentId;
    private String userId;
    private OffsetDateTime createdAt;
}
