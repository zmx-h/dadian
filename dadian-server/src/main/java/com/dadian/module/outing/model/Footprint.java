package com.dadian.module.outing.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("footprints")
public class Footprint {
    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String outingId;
    private String spotId;
    private String photoUrl;
    private String comment;
    private Integer dailySeq;
    private OffsetDateTime createdAt;
}
