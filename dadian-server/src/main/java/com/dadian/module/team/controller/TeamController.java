package com.dadian.module.team.controller;

import com.dadian.common.ApiResponse;
import com.dadian.module.team.model.TeamInvitationResponse;
import com.dadian.module.team.model.TeammateInfo;
import com.dadian.module.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    /**
     * Invite a user to join a team outing.
     */
    @PostMapping("/outings/{id}/invite")
    public ApiResponse<TeamInvitationResponse> invite(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal String userId) {
        String inviteeId = body.get("userId");
        TeamInvitationResponse resp = teamService.invite(id, userId, inviteeId);
        return ApiResponse.ok(resp);
    }

    /**
     * Accept or decline a team invitation.
     */
    @PostMapping("/outings/{id}/invitations/respond")
    public ApiResponse<TeamInvitationResponse> respond(
            @PathVariable String id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal String userId) {
        boolean accept = Boolean.TRUE.equals(body.get("accept"));
        TeamInvitationResponse resp = teamService.respond(id, userId, accept);
        return ApiResponse.ok(resp);
    }

    /**
     * Get all pending invitations for the current user.
     */
    @GetMapping("/users/me/invitations")
    public ApiResponse<List<TeamInvitationResponse>> myInvitations(
            @AuthenticationPrincipal String userId) {
        List<TeamInvitationResponse> list = teamService.getPendingInvitations(userId);
        return ApiResponse.ok(list);
    }

    /**
     * Get all teammates in an outing.
     */
    @GetMapping("/outings/{id}/teammates")
    public ApiResponse<List<TeammateInfo>> getTeammates(
            @PathVariable String id) {
        List<TeammateInfo> list = teamService.getTeammates(id);
        return ApiResponse.ok(list);
    }

    /**
     * Leave an outing (remove self as participant).
     */
    @PostMapping("/outings/{id}/leave")
    public ApiResponse<?> leave(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        teamService.leave(id, userId);
        return ApiResponse.ok();
    }

    /**
     * Check mutual follow status between current user and another user.
     */
    @GetMapping("/users/{uid}/mutual-follow")
    public ApiResponse<Map<String, Boolean>> checkMutualFollow(
            @PathVariable String uid,
            @AuthenticationPrincipal String userId) {
        boolean mutual = teamService.checkMutualFollow(userId, uid);
        return ApiResponse.ok(Map.of("mutual", mutual));
    }
}
