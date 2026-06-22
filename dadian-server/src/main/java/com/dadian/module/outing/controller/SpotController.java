package com.dadian.module.outing.controller;

import com.dadian.common.ApiResponse;
import com.dadian.module.outing.model.Spot;
import com.dadian.module.outing.service.FootprintService;
import com.dadian.module.outing.service.SpotService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SpotController {
    private final SpotService spotService;

    @GetMapping("/spots")
    public ApiResponse<List<Spot>> list(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radius,
            @RequestParam(defaultValue = "12") int limit) {
        if (lat != null && lng != null && radius != null) {
            return ApiResponse.ok(spotService.findNearby(lat, lng, radius, limit));
        }
        return ApiResponse.ok(spotService.listAll());
    }

    @GetMapping("/spots/{id}")
    public ApiResponse<Spot> get(@PathVariable String id) {
        return ApiResponse.ok(spotService.findById(id));
    }
}
