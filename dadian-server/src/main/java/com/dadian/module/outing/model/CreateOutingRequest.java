package com.dadian.module.outing.model;

import lombok.Data;

@Data
public class CreateOutingRequest {
    private String mode = "solo";
    private Integer energy = 50;
    private String role = "agent";
    private String spotId;
    private Double lat;
    private Double lng;
}
