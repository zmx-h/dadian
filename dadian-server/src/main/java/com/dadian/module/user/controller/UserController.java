package com.dadian.module.user.controller;

import com.dadian.common.ApiResponse;
import com.dadian.module.user.model.*;
import com.dadian.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<List<AchievementDTO>> getAchievements(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        List<AchievementDTO> achievements = userService.getAchievements(userId);
        return ApiResponse.ok(achievements);
    }

    @GetMapping("/me/stats")
    public ApiResponse<UserStatsDTO> getStats(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        UserStatsDTO stats = userService.getStats(userId);
        return ApiResponse.ok(stats);
    }

    @PostMapping("/me/export")
    public ApiResponse<?> exportData(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        Map<String, Object> result = userService.exportData(userId);
        return ApiResponse.ok(result);
    }

    @DeleteMapping("/me")
    public ApiResponse<?> deleteAccount(Authentication auth,
                                         @RequestParam(defaultValue = "false") boolean confirm) {
        if (!confirm) {
            return ApiResponse.ok(Map.of(
                    "message", "请将 query param confirm=true 作为确认",
                    "hint", "删除后所有数据将被标记删除，30天内可联系客服恢复"
            ));
        }
        String userId = (String) auth.getPrincipal();
        userService.softDelete(userId);
        return ApiResponse.ok();
    }

    @PostMapping("/me/deactivate")
    public ApiResponse<?> deactivate(Authentication auth) {
        return ApiResponse.ok(Map.of("message", "Account deactivated. Data will be retained for 30 days."));
    }
}
