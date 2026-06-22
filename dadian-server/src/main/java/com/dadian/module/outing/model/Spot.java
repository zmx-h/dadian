package com.dadian.module.outing.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("spots")
public class Spot {
    @TableId(type = IdType.INPUT)
    private String id;
    private String name;
    private String category;
    private Double lat;
    private Double lng;
    private String address;
    private String city;
    private String crowdLevel;
    private Double rating;
    private String[] tags;
    private String highlight;
    private String imageUrl;
    private String sourceAmap;
    private String[] dadianTags;
    private String dadianHighlight;
    private Integer dailyCheckinCount;
    private OffsetDateTime lastSyncedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
