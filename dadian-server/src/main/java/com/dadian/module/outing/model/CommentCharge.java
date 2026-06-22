package com.dadian.module.outing.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("comment_charges")
public class CommentCharge {
    private String commentId;
    private String userId;
    private OffsetDateTime createdAt;
}
