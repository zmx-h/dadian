package com.dadian.module.outing.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("dice_records")
public class DiceRecord {
    @TableId(type = IdType.INPUT)
    private String id;
    private String userId;
    private String outingId;
    private String scene;
    private String choice;
    private String resultSpotId;
    private OffsetDateTime rolledAt;
}
