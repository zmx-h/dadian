package com.dadian.module.admin.model;

import lombok.Data;

import java.util.List;

@Data
public class DiceConfigsSaveRequest {
    private List<DiceConfigDTO> configs;
}
