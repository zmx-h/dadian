package com.dadian.module.outing.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("outings")
public class Outing {
    @TableId(type = IdType.INPUT)
    private String id;
    private String creatorId;
    private String mode;
    private String status;
    private String title;
    private String diceResultId;
    private String destinationSpotId;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
