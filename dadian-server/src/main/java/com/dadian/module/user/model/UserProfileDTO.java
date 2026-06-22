package com.dadian.module.user.model;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class UserProfileDTO {
    private String id;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private Integer socialTrait;
    private Integer weekendStyle;
    private Integer crowdFeeling;
    private String companionTone;
    private Integer companionIntensity;
    private Integer humorLevel;
    private String achievementVisibility;
    private String locationRetention;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
