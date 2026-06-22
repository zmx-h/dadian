package com.dadian.module.outing.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("missions")
public class Mission {
    @TableId(type = IdType.INPUT)
    private String id;
    private String outingId;
    private String waypointId;
    private String type;
    private String title;
    private String description;
    private String reward;
    private String assignedRole;
    private Integer triggerRadiusM;
    private Boolean requiredPhoto;
    private String status;
    private OffsetDateTime completedAt;
    private OffsetDateTime createdAt;
}
