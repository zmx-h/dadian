package com.dadian.module.outing.controller;

import com.dadian.common.ApiResponse;
import com.dadian.module.outing.model.Footprint;
import com.dadian.module.outing.model.FootprintRequest;
import com.dadian.module.outing.service.FootprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FootprintController {
    private final FootprintService footprintService;

    @PostMapping("/footprints")
    public ApiResponse<Footprint> checkin(@RequestBody FootprintRequest req, @AuthenticationPrincipal String userId) {
        Footprint fp = footprintService.checkin(req, userId);
        return ApiResponse.ok(fp);
    }

    @GetMapping("/spots/{spotId}/footprints")
    public ApiResponse<List<Footprint>> getBySpot(@PathVariable String spotId, @RequestParam(defaultValue = "3") int limit) {
        return ApiResponse.ok(footprintService.getBySpot(spotId, limit));
    }

    @PostMapping("/footprints/{id}/charge")
    public ApiResponse<Map<String, Boolean>> charge(@PathVariable String id, @AuthenticationPrincipal String userId) {
        return ApiResponse.ok(Map.of("charged", true));
    }
}
