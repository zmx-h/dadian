package com.dadian.module.team.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeammateInfo {
    private String userId;
    private String displayName;
    private String avatarUrl;
    private String role;
    private Integer socialEnergy;
    private boolean isOnline;
}
