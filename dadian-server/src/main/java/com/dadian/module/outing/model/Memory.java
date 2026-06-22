package com.dadian.module.outing.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("memories")
public class Memory {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String outingId;
    private String userId;
    private String title;
    private String style;
    private String coverUrl;
    private String summary;
    private String visibility;
    private Boolean isSynthetic;
    private String stats;
    private OffsetDateTime generatedAt;
}
