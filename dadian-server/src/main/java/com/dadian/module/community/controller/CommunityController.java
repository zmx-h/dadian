package com.dadian.module.community.controller;

import com.dadian.common.ApiResponse;
import com.dadian.module.community.model.Follow;
import com.dadian.module.community.model.Message;
import com.dadian.module.community.service.FollowService;
import com.dadian.module.community.service.MessageService;
import com.dadian.module.community.service.ResonanceService;
import com.dadian.module.outing.model.Memory;
import com.dadian.module.outing.service.MemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommunityController {

    private final FollowService followService;
    private final MessageService messageService;
    private final ResonanceService resonanceService;
    private final MemoryService memoryService;

    // ─── Follows ───

    @PostMapping("/follows")
    public ApiResponse<?> follow(@RequestBody Map<String, String> body,
                                  @AuthenticationPrincipal String userId) {
        String followedId = body.get("followedId");
        followService.follow(userId, followedId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/follows/{userId}")
    public ApiResponse<?> unfollow(@PathVariable String userId,
                                    @AuthenticationPrincipal String currentUserId) {
        followService.unfollow(currentUserId, userId);
        return ApiResponse.ok();
    }

    @GetMapping("/users/me/followers")
    public ApiResponse<List<String>> myFollowers(@RequestParam(defaultValue = "20") int limit,
                                                   @AuthenticationPrincipal String userId) {
        List<Follow> followers = followService.getFollowers(userId, limit);
        List<String> ids = followers.stream().map(Follow::getFollowerId).collect(Collectors.toList());
        return ApiResponse.ok(ids);
    }

    @GetMapping("/users/me/following")
    public ApiResponse<List<String>> myFollowing(@RequestParam(defaultValue = "20") int limit,
                                                   @AuthenticationPrincipal String userId) {
        List<Follow> following = followService.getFollowing(userId, limit);
        List<String> ids = following.stream().map(Follow::getFollowedId).collect(Collectors.toList());
        return ApiResponse.ok(ids);
    }

    @GetMapping("/users/{uid}/followers")
    public ApiResponse<List<String>> userFollowers(@PathVariable String uid,
                                                     @RequestParam(defaultValue = "20") int limit) {
        List<Follow> followers = followService.getFollowers(uid, limit);
        List<String> ids = followers.stream().map(Follow::getFollowerId).collect(Collectors.toList());
        return ApiResponse.ok(ids);
    }

    @GetMapping("/users/{uid}/following")
    public ApiResponse<List<String>> userFollowing(@PathVariable String uid,
                                                     @RequestParam(defaultValue = "20") int limit) {
        List<Follow> following = followService.getFollowing(uid, limit);
        List<String> ids = following.stream().map(Follow::getFollowedId).collect(Collectors.toList());
        return ApiResponse.ok(ids);
    }

    // ─── Messages ───

    @PostMapping("/messages")
    public ApiResponse<Message> sendMessage(@RequestBody Map<String, String> body,
                                             @AuthenticationPrincipal String userId) {
        String receiverId = body.get("receiverId");
        String content = body.get("content");
        Message message = messageService.send(userId, receiverId, content);
        return ApiResponse.ok(message);
    }

    @GetMapping("/messages/conversation/{userId}")
    public ApiResponse<List<Message>> getConversation(@PathVariable String userId,
                                                        @RequestParam(defaultValue = "50") int limit,
                                                        @AuthenticationPrincipal String currentUserId) {
        return ApiResponse.ok(messageService.getConversation(currentUserId, userId, limit));
    }

    @GetMapping("/messages/inbox")
    public ApiResponse<List<Message>> getInbox(@AuthenticationPrincipal String userId) {
        return ApiResponse.ok(messageService.getInbox(userId));
    }

    @PostMapping("/messages/{id}/read")
    public ApiResponse<?> markRead(@PathVariable String id,
                                    @AuthenticationPrincipal String userId) {
        messageService.markRead(id, userId);
        return ApiResponse.ok();
    }

    // ─── Resonance / Discover ───

    @GetMapping("/community/discover")
    public ApiResponse<List<Map<String, Object>>> discover(@RequestParam(defaultValue = "10") int limit,
                                                             @AuthenticationPrincipal String userId) {
        return ApiResponse.ok(resonanceService.getResonantUsers(userId, limit));
    }

    @GetMapping("/community/resonance/{userId}")
    public ApiResponse<Map<String, Object>> resonanceScore(@PathVariable String userId,
                                                             @AuthenticationPrincipal String currentUserId) {
        return ApiResponse.ok(resonanceService.getMatchingScore(currentUserId, userId));
    }

    // ─── Community Feed ───

    @GetMapping("/community/feed")
    public ApiResponse<List<Memory>> communityFeed(@RequestParam(required = false) String cursor,
                                                     @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(memoryService.listPublic(cursor, limit));
    }
}
