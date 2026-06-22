package com.dadian.module.admin.controller;

import com.dadian.common.ApiResponse;
import com.dadian.module.admin.model.AdminStatsDTO;
import com.dadian.module.admin.model.DiceConfigDTO;
import com.dadian.module.admin.model.DiceConfigsSaveRequest;
import com.dadian.module.admin.service.AdminService;
import com.dadian.module.outing.model.Comment;
import com.dadian.module.outing.model.Memory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ApiResponse<AdminStatsDTO> getStats(@AuthenticationPrincipal String userId) {
        AdminStatsDTO stats = adminService.getStats();
        return ApiResponse.ok(stats);
    }

    @GetMapping("/memories/pending")
    public ApiResponse<List<Memory>> getPendingMemories(@AuthenticationPrincipal String userId) {
        List<Memory> memories = adminService.getPendingMemories();
        return ApiResponse.ok(memories);
    }

    @PostMapping("/memories/{id}/approve")
    public ApiResponse<?> approveMemory(@PathVariable String id, @AuthenticationPrincipal String userId) {
        adminService.approveMemory(id, userId);
        return ApiResponse.ok();
    }

    @PostMapping("/memories/{id}/reject")
    public ApiResponse<?> rejectMemory(@PathVariable String id, @AuthenticationPrincipal String userId) {
        adminService.rejectMemory(id, userId);
        return ApiResponse.ok();
    }

    @GetMapping("/comments/pending")
    public ApiResponse<List<Comment>> getPendingComments(@AuthenticationPrincipal String userId) {
        List<Comment> comments = adminService.getPendingComments();
        return ApiResponse.ok(comments);
    }

    @PostMapping("/comments/{id}/approve")
    public ApiResponse<?> approveComment(@PathVariable String id, @AuthenticationPrincipal String userId) {
        adminService.approveComment(id, userId);
        return ApiResponse.ok();
    }

    @PostMapping("/comments/{id}/reject")
    public ApiResponse<?> rejectComment(@PathVariable String id, @AuthenticationPrincipal String userId) {
        adminService.rejectComment(id, userId);
        return ApiResponse.ok();
    }

    @GetMapping("/dice-configs")
    public ApiResponse<List<DiceConfigDTO>> getDiceConfigs(@AuthenticationPrincipal String userId) {
        List<DiceConfigDTO> configs = adminService.getDiceConfigs();
        return ApiResponse.ok(configs);
    }

    @PutMapping("/dice-configs")
    public ApiResponse<?> updateDiceConfigs(@RequestBody DiceConfigsSaveRequest body, @AuthenticationPrincipal String userId) {
        adminService.saveDiceConfigs(body.getConfigs());
        return ApiResponse.ok();
    }
}
