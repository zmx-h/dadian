package com.dadian.module.team.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamInvitationResponse {
    private String outingId;
    private String inviterId;
    private String inviterName;
    private String outingTitle;
    private String inviteeId;
    private String status;
    private String createdAt;
}
