package com.dadian.module.user.controller;

import com.dadian.common.ApiResponse;
import com.dadian.module.user.model.*;
import com.dadian.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileDTO> getProfile(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        User user = userService.getById(userId);
        return ApiResponse.ok(userService.toProfileDTO(user));
    }

    @PatchMapping("/me")
    public ApiResponse<UserProfileDTO> updateProfile(Authentication auth,
                                                      @RequestBody Map<String, String> body) {
        String userId = (String) auth.getPrincipal();
        UserProfileDTO dto = new UserProfileDTO();
        dto.setDisplayName(body.get("displayName"));
        dto.setAvatarUrl(body.get("avatarUrl"));
        dto.setBio(body.get("bio"));
        User user = userService.updateProfile(userId, dto);
        return ApiResponse.ok(userService.toProfileDTO(user));
    }

    @PutMapping("/me/onboarding")
    public ApiResponse<UserProfileDTO> updateOnboarding(Authentication auth,
                                                         @Valid @RequestBody UserOnboardingDTO dto) {
        String userId = (String) auth.getPrincipal();
        User user = userService.updateOnboarding(userId, dto);
        return ApiResponse.ok(userService.toProfileDTO(user));
    }

    @PutMapping("/me/privacy")
    public ApiResponse<UserProfileDTO> updatePrivacy(Authentication auth,
                                                      @RequestBody UserPrivacyDTO dto) {
        String userId = (String) auth.getPrincipal();
        User user = userService.updatePrivacy(userId, dto);
        return ApiResponse.ok(userService.toProfileDTO(user));
    }

    @GetMapping("/me/achievements")
    public ApiResponse<List<Object>> getAchievements() {
        return ApiResponse.ok(Collections.emptyList());
    }

    @PostMapping("/me/export")
    public ApiResponse<?> exportData(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return ApiResponse.ok(Map.of(
                "message", "Data export initiated. A download link will be sent to your registered phone.",
                "requestId", java.util.UUID.randomUUID().toString()
        ));
    }

    @DeleteMapping("/me")
    public ApiResponse<?> deleteAccount(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        userService.softDelete(userId);
        return ApiResponse.ok();
    }

    @PostMapping("/me/deactivate")
    public ApiResponse<?> deactivate(Authentication auth) {
        return ApiResponse.ok(Map.of("message", "Account deactivated. Data will be retained for 30 days."));
    }
}
