package com.dadian.module.outing.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MissionResponse {
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
    private String participantStatus;
    private OffsetDateTime completedAt;
}
