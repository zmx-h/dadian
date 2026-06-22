package com.dadian.module.user.model;

import lombok.Data;

@Data
public class AchievementDTO {
    private String key;
    private String name;
    private String description;
    private String icon;
    private int progress;
    private int max;
    private boolean unlocked;
}
