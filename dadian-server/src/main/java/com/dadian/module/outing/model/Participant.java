package com.dadian.module.outing.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("participants")
public class Participant {
    @TableId(type = IdType.INPUT)
    private String id;
    private String outingId;
    private String userId;
    private String role;
    private Integer socialEnergy;
    private Integer roleScore;
    private Boolean isCompleted;
    private OffsetDateTime createdAt;
}
