package com.dadian.module.outing.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutingResponse {
    private String id;
    private String creatorId;
    private String mode;
    private String status;
    private String title;
    private String destinationSpotId;
    private String destinationSpotName;
    private List<ParticipantInfo> participants;
    private RouteResponse route;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private OffsetDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantInfo {
        private String id;
        private String userId;
        private String displayName;
        private String role;
        private Integer socialEnergy;
    }
}
