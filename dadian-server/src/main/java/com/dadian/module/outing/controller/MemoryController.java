package com.dadian.module.outing.controller;

import com.dadian.common.ApiResponse;
import com.dadian.module.outing.model.*;
import com.dadian.module.outing.service.MemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MemoryController {
    private final MemoryService memoryService;

    @PostMapping("/memories/generate")
    public ApiResponse<Memory> generate(@RequestBody MemoryGenerateRequest req, @AuthenticationPrincipal String userId) {
        return ApiResponse.ok(memoryService.generate(req.getOutingId(), userId, req.getStyle(), req.getVisibility()));
    }

    @GetMapping("/memories")
    public ApiResponse<List<Memory>> list(
            @RequestParam(defaultValue = "private") String visibility,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal String userId) {
        return ApiResponse.ok(memoryService.listByUser(userId, visibility, cursor, limit));
    }

    @GetMapping("/memories/public")
    public ApiResponse<List<Memory>> publicFeed(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(memoryService.listPublic(cursor, limit));
    }

    @GetMapping("/memories/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable String id) {
        Memory m = memoryService.findById(id);
        List<MemoryPhoto> photos = memoryService.getPhotos(id);
        return ApiResponse.ok(Map.of("memory", m, "photos", photos));
    }

    @GetMapping("/memories/{id}/photos")
    public ApiResponse<List<MemoryPhoto>> photos(@PathVariable String id) {
        return ApiResponse.ok(memoryService.getPhotos(id));
    }

    @PatchMapping("/memories/{id}/visibility")
    public ApiResponse<?> updateVisibility(@PathVariable String id, @RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal String userId) {
        memoryService.updateVisibility(id, userId, body.get("visibility"));
        return ApiResponse.ok();
    }

    @DeleteMapping("/memories/{id}")
    public ApiResponse<?> delete(@PathVariable String id, @AuthenticationPrincipal String userId) {
        memoryService.delete(id, userId);
        return ApiResponse.ok();
    }

    @PostMapping("/memories/{id}/comments")
    public ApiResponse<Comment> addComment(@PathVariable String id, @RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal String userId) {
        return ApiResponse.ok(memoryService.addComment(id, userId, body.get("content")));
    }

    @GetMapping("/memories/{id}/comments")
    public ApiResponse<List<Comment>> comments(@PathVariable String id, @AuthenticationPrincipal String userId) {
        return ApiResponse.ok(memoryService.getComments(id, userId));
    }

    @PostMapping("/comments/{id}/charge")
    public ApiResponse<Map<String, Boolean>> toggleCharge(@PathVariable String id, @AuthenticationPrincipal String userId) {
        boolean charged = memoryService.toggleCharge(id, userId);
        return ApiResponse.ok(Map.of("charged", charged));
    }

    // ─── Collect & Replay ───

    @PostMapping("/outings/{id}/collect")
    public ApiResponse<Memory> collectOuting(@PathVariable String id,
                                              @AuthenticationPrincipal String userId) {
        return ApiResponse.ok(memoryService.collectOuting(id, userId));
    }

    @GetMapping("/users/me/scripts")
    public ApiResponse<List<Memory>> listScripts(@AuthenticationPrincipal String userId) {
        return ApiResponse.ok(memoryService.listCollectedScripts(userId));
    }
}
