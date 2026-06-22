package com.dadian.module.outing.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("routes")
public class Route {
    @TableId(type = IdType.INPUT)
    private String id;
    private String outingId;
    private String neonColor;
    private Integer totalDistanceM;
    private String polyline;
    private OffsetDateTime createdAt;
}
