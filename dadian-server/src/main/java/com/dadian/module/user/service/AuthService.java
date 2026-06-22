package com.dadian.module.user.service;

import com.dadian.common.BusinessException;
import com.dadian.common.ErrorCode;
import com.dadian.module.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final TokenService tokenService;
    private final StringRedisTemplate redisTemplate;

    @Value("${dadian.sms.provider:aliyun-stub}")
    private String smsProvider;

    @Value("${dadian.sms.sign-name:搭电}")
    private String smsSignName;

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final long SMS_CODE_TTL = 5;

    public void sendSmsCode(String phone) {
        String code = String.format("%06d", (int) (Math.random() * 1_000_000));
        String redisKey = SMS_CODE_PREFIX + phone;

        redisTemplate.opsForValue().set(redisKey, code, SMS_CODE_TTL, TimeUnit.MINUTES);

        log.info("[{}] SMS code sent to {}: code={}, sign={}", smsProvider, phone, code, smsSignName);
    }

    public Map<String, Object> verifyAndLogin(String phone, String code) {
        String redisKey = SMS_CODE_PREFIX + phone;
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            throw new BusinessException(ErrorCode.AUTH_INVALID, "Verification code expired or not sent");
        }
        if (!storedCode.equals(code)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID, "Verification code incorrect");
        }

        redisTemplate.delete(redisKey);

        User user = userService.getByPhone(phone);
        if (user == null) {
            String displayName = "User_" + phone.substring(phone.length() - 4);
            user = userService.create(phone, displayName);
        }

        if (user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.ACCOUNT_DEACTIVATED, "Account has been deactivated");
        }

        String accessToken = tokenService.generateAccessToken(user.getId());
        String refreshToken = tokenService.generateRefreshToken(user.getId());

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "expiresIn", 900
        );
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        String userId = tokenService.validateAndConsumeRefreshToken(refreshToken);

        String newAccessToken = tokenService.generateAccessToken(userId);
        String newRefreshToken = tokenService.generateRefreshToken(userId);

        return Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken,
                "expiresIn", 900
        );
    }

    public void logout(String accessToken) {
        try {
            String userId = tokenService.getUserIdFromToken(accessToken);
            tokenService.revokeAllRefreshTokens(userId);
            log.info("User logged out: id={}", userId);
        } catch (Exception e) {
            log.warn("Logout with invalid token ignored");
        }
    }
}
