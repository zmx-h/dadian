package com.dadian.module.outing.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("waypoints")
public class Waypoint {
    @TableId(type = IdType.INPUT)
    private String id;
    private String routeId;
    private String spotId;
    private String type;
    private Integer seq;
    private Double lat;
    private Double lng;
}
