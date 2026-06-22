package com.dadian.module.user.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dadian.common.BusinessException;
import com.dadian.common.ErrorCode;
import com.dadian.module.user.mapper.UserMapper;
import com.dadian.module.user.model.User;
import com.dadian.module.user.model.UserOnboardingDTO;
import com.dadian.module.user.model.UserPrivacyDTO;
import com.dadian.module.user.model.UserProfileDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public User getById(String id) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getId, id)
                        .isNull(User::getDeletedAt)
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found");
        }
        return user;
    }

    public User getByPhone(String phone) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone)
                        .isNull(User::getDeletedAt)
        );
    }

    @Transactional
    public User create(String phone, String displayName) {
        User existing = getByPhone(phone);
        if (existing != null) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED, "Phone already registered");
        }

        User user = new User();
        user.setId(java.util.UUID.randomUUID().toString());
        user.setPhone(phone);
        user.setPhoneHash(DigestUtil.sha256Hex(phone));
        user.setDisplayName(displayName);
        userMapper.insert(user);
        log.info("User created: id={}, phone={}", user.getId(), user.getPhone());
        return user;
    }

    @Transactional
    public User updateProfile(String id, UserProfileDTO dto) {
        User user = getById(id);

        if (dto.getDisplayName() != null) {
            user.setDisplayName(dto.getDisplayName());
        }
        if (dto.getAvatarUrl() != null) {
            user.setAvatarUrl(dto.getAvatarUrl());
        }
        if (dto.getBio() != null) {
            user.setBio(dto.getBio());
        }

        user.setUpdatedAt(OffsetDateTime.now());
        userMapper.updateById(user);
        return user;
    }

    @Transactional
    public User updateOnboarding(String id, UserOnboardingDTO dto) {
        User user = getById(id);
        user.setSocialTrait(dto.getSocialTrait());
        user.setWeekendStyle(dto.getWeekendStyle());
        user.setCrowdFeeling(dto.getCrowdFeeling());
        user.setUpdatedAt(OffsetDateTime.now());
        userMapper.updateById(user);
        return user;
    }

    @Transactional
    public User updatePrivacy(String id, UserPrivacyDTO dto) {
        User user = getById(id);
        if (dto.getAchievementVisibility() != null) {
            user.setAchievementVisibility(dto.getAchievementVisibility());
        }
        if (dto.getLocationRetention() != null) {
            user.setLocationRetention(dto.getLocationRetention());
        }
        user.setUpdatedAt(OffsetDateTime.now());
        userMapper.updateById(user);
        return user;
    }

    @Transactional
    public void softDelete(String id) {
        User user = getById(id);
        user.setDeletedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        userMapper.updateById(user);
        log.info("User soft-deleted: id={}", id);
    }

    public boolean existsByPhone(String phone) {
        return getByPhone(phone) != null;
    }

    public UserProfileDTO toProfileDTO(User user) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(user.getId());
        dto.setDisplayName(user.getDisplayName());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setBio(user.getBio());
        dto.setSocialTrait(user.getSocialTrait());
        dto.setWeekendStyle(user.getWeekendStyle());
        dto.setCrowdFeeling(user.getCrowdFeeling());
        dto.setCompanionTone(user.getCompanionTone());
        dto.setCompanionIntensity(user.getCompanionIntensity());
        dto.setHumorLevel(user.getHumorLevel());
        dto.setAchievementVisibility(user.getAchievementVisibility());
        dto.setLocationRetention(user.getLocationRetention());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
