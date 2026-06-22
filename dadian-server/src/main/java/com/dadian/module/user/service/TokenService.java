package com.dadian.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dadian.common.BusinessException;
import com.dadian.common.ErrorCode;
import com.dadian.module.user.mapper.RefreshTokenMapper;
import com.dadian.module.user.model.RefreshToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    @Value("${dadian.jwt.secret}")
    private String jwtSecret;

    private static final long ACCESS_TOKEN_TTL = 15 * 60 * 1000L;
    private static final long REFRESH_TOKEN_TTL = 7 * 24 * 60 * 60 * 1000L;

    private final RefreshTokenMapper refreshTokenMapper;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String userId) {
        String subject = userId.toString();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_TTL);

        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(String userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + REFRESH_TOKEN_TTL);

        String token = Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getKey())
                .compact();

        String tokenHash = hashToken(token);
        RefreshToken rt = new RefreshToken();
        rt.setId(java.util.UUID.randomUUID().toString());
        rt.setUserId(userId);
        rt.setTokenHash(tokenHash);
        rt.setExpiresAt(expiry.toInstant().atOffset(ZoneOffset.UTC));
        refreshTokenMapper.insert(rt);

        return token;
    }

    public Claims validateAccessToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.AUTH_INVALID, "Invalid or expired access token");
        }
    }

    public String getUserIdFromToken(String token) {
        Claims claims = validateAccessToken(token);
        return claims.getSubject();
    }

    public String validateAndConsumeRefreshToken(String refreshToken) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.AUTH_INVALID, "Invalid or expired refresh token");
        }

        String userId = claims.getSubject();
        String tokenHash = hashToken(refreshToken);

        RefreshToken stored = refreshTokenMapper.selectOne(
                new LambdaQueryWrapper<RefreshToken>()
                        .eq(RefreshToken::getTokenHash, tokenHash)
                        .eq(RefreshToken::getUserId, userId)
        );

        if (stored == null) {
            throw new BusinessException(ErrorCode.AUTH_INVALID, "Refresh token not found or already used");
        }

        refreshTokenMapper.deleteById(stored.getId());
        return userId;
    }

    public void revokeAllRefreshTokens(String userId) {
        refreshTokenMapper.delete(
                new LambdaQueryWrapper<RefreshToken>()
                        .eq(RefreshToken::getUserId, userId)
        );
    }

    private String hashToken(String token) {
        return cn.hutool.crypto.digest.DigestUtil.sha256Hex(token);
    }
}
