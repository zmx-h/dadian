package com.dadian.module.admin.model;

import lombok.Data;

@Data
public class AdminStatsDTO {
    private long dau;
    private long outingCount;
    private long memoryCount;
    private long aiTokenUsed;
    private long smsCount;
    private String period;
}
