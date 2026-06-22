package com.dadian.module.outing.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {
    private String id;
    private String neonColor;
    private Integer totalDistanceM;
    private String polyline;
    private List<WaypointInfo> waypoints;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaypointInfo {
        private String id;
        private String spotId;
        private String name;
        private String type;
        private Integer seq;
        private Double lat;
        private Double lng;
    }
}
