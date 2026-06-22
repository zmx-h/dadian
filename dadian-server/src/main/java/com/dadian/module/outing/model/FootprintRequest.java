package com.dadian.module.outing.model;

import lombok.Data;

@Data
public class FootprintRequest {
    private String outingId;
    private String spotId;
    private Double lat;
    private Double lng;
    private String photoUrl;
    private String comment;
}
