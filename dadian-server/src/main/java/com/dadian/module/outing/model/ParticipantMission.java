package com.dadian.module.outing.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@TableName("participant_missions")
public class ParticipantMission {
    @TableId(type = IdType.INPUT)
    private String id;
    private String participantId;
    private String missionId;
    private String status;
    private OffsetDateTime completedAt;
    private String proofPhotoUrl;
}
