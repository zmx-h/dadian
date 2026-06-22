package com.dadian.module.user.model;

import lombok.Data;

@Data
public class UserStatsDTO {
    private long outingCount;
    private long footprintCount;
    private long memoryCount;
    private long followingCount;
    private long followerCount;
    private double totalDistance;
    private String favoriteSpotCategory;
}
