package com.dadian.module.outing.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("memory_photos")
public class MemoryPhoto {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String memoryId;
    private String url;
    private String caption;
    private String style;
    private String vlmComment;
    private String spotName;
    private OffsetDateTime takenAt;
    private Integer seq;
}
