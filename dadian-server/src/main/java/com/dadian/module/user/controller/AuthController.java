package com.dadian.module.user.controller;

import com.dadian.common.ApiResponse;
import com.dadian.module.user.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-code")
    public ApiResponse<?> sendCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || phone.isBlank()) {
            return ApiResponse.fail(1002, "Phone is required");
        }
        authService.sendSmsCode(phone);
        return ApiResponse.ok();
    }

    @PostMapping("/verify")
    public ApiResponse<Map<String, Object>> verify(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String code = body.get("code");
        if (phone == null || phone.isBlank() || code == null || code.isBlank()) {
            return ApiResponse.fail(1002, "Phone and code are required");
        }
        Map<String, Object> tokens = authService.verifyAndLogin(phone, code);
        return ApiResponse.ok(tokens);
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ApiResponse.fail(1002, "refreshToken is required");
        }
        Map<String, Object> tokens = authService.refreshToken(refreshToken);
        return ApiResponse.ok(tokens);
    }

    @DeleteMapping("/logout")
    public ApiResponse<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ApiResponse.ok();
    }
}
