package com.dadian.module.user.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dadian.common.BusinessException;
import com.dadian.common.ErrorCode;
import com.dadian.module.community.mapper.FollowMapper;
import com.dadian.module.community.model.Follow;
import com.dadian.module.outing.mapper.*;
import com.dadian.module.outing.model.*;
import com.dadian.module.user.mapper.UserMapper;
import com.dadian.module.user.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final OutingMapper outingMapper;
    private final FootprintMapper footprintMapper;
    private final MemoryMapper memoryMapper;
    private final DiceRecordMapper diceRecordMapper;
    private final ParticipantMapper participantMapper;
    private final FollowMapper followMapper;
    private final SpotMapper spotMapper;
    private final TokenService tokenService;

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
        tokenService.revokeAllRefreshTokens(user.getId());
        log.info("User soft-deleted: id={}", id);
    }

    public boolean existsByPhone(String phone) {
        return getByPhone(phone) != null;
    }

    // ─── Achievements ───

    public List<AchievementDTO> getAchievements(String userId) {
        List<AchievementDTO> achievements = new ArrayList<>();

        // "逃跑大师" — count dice_records (escape count)
        long escapeCount = diceRecordMapper.selectCount(
                new LambdaQueryWrapper<DiceRecord>().eq(DiceRecord::getUserId, userId));
        achievements.add(buildAchievement("escape_master", "逃跑大师",
                "使用骰子决定出行目的地", "🎲",
                (int) escapeCount, 50, escapeCount >= 50));

        // "特工" — count footprints
        long footprintCount = footprintMapper.selectCount(
                new LambdaQueryWrapper<Footprint>().eq(Footprint::getUserId, userId));
        achievements.add(buildAchievement("agent", "特工",
                "留下足迹打卡", "🖴",
                (int) footprintCount, 100, footprintCount >= 100));

        // "王家卫传人" — count memories with style=wangjiawei
        long wangjiaweiCount = memoryMapper.selectCount(
                new LambdaQueryWrapper<Memory>()
                        .eq(Memory::getUserId, userId)
                        .eq(Memory::getStyle, "wangjiawei"));
        achievements.add(buildAchievement("wangjiawei_heir", "王家卫传人",
                "用王家卫风格生成记忆", "🎬",
                (int) wangjiaweiCount, 20, wangjiaweiCount >= 20));

        // "佛系大师" — count outings where user participated as role=npc
        long npcCount = participantMapper.selectCount(
                new LambdaQueryWrapper<Participant>()
                        .eq(Participant::getUserId, userId)
                        .eq(Participant::getRole, "npc"));
        achievements.add(buildAchievement("zen_master", "佛系大师",
                "以NPC身份参与出行", "🧘",
                (int) npcCount, 30, npcCount >= 30));

        return achievements;
    }

    private AchievementDTO buildAchievement(String key, String name, String description, String icon,
                                             int progress, int max, boolean unlocked) {
        AchievementDTO dto = new AchievementDTO();
        dto.setKey(key);
        dto.setName(name);
        dto.setDescription(description);
        dto.setIcon(icon);
        dto.setProgress(Math.min(progress, max));
        dto.setMax(max);
        dto.setUnlocked(unlocked);
        return dto;
    }

    // ─── Stats ───

    public UserStatsDTO getStats(String userId) {
        long outingCount = outingMapper.selectCount(
                new LambdaQueryWrapper<Outing>().eq(Outing::getCreatorId, userId));
        long footprintCount = footprintMapper.selectCount(
                new LambdaQueryWrapper<Footprint>().eq(Footprint::getUserId, userId));
        long memoryCount = memoryMapper.selectCount(
                new LambdaQueryWrapper<Memory>().eq(Memory::getUserId, userId));
        long followingCount = followMapper.selectCount(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, userId));
        long followerCount = followMapper.selectCount(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowedId, userId));

        // Estimate total distance from footprints — each footprint ~500m (stub)
        double totalDistance = footprintCount * 0.5;

        // Favorite spot category — find most frequent category from footprints
        String favoriteSpotCategory = "未知";
        List<Footprint> footprints = footprintMapper.selectList(
                new LambdaQueryWrapper<Footprint>()
                        .eq(Footprint::getUserId, userId)
                        .isNotNull(Footprint::getSpotId));
        if (!footprints.isEmpty()) {
            Map<String, Long> categoryCount = new HashMap<>();
            for (Footprint fp : footprints) {
                if (fp.getSpotId() != null) {
                    Spot spot = spotMapper.selectById(fp.getSpotId());
                    if (spot != null && spot.getCategory() != null) {
                        categoryCount.merge(spot.getCategory(), 1L, Long::sum);
                    }
                }
            }
            favoriteSpotCategory = categoryCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("未知");
        }

        UserStatsDTO dto = new UserStatsDTO();
        dto.setOutingCount(outingCount);
        dto.setFootprintCount(footprintCount);
        dto.setMemoryCount(memoryCount);
        dto.setFollowingCount(followingCount);
        dto.setFollowerCount(followerCount);
        dto.setTotalDistance(totalDistance);
        dto.setFavoriteSpotCategory(favoriteSpotCategory);
        return dto;
    }

    // ─── Export ───

    public Map<String, Object> exportData(String userId) {
        User user = getById(userId);
        List<Outing> outings = outingMapper.selectList(
                new LambdaQueryWrapper<Outing>().eq(Outing::getCreatorId, userId));
        List<Memory> memories = memoryMapper.selectList(
                new LambdaQueryWrapper<Memory>().eq(Memory::getUserId, userId));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("profile", toProfileDTO(user));
        data.put("outings", outings);
        data.put("memoryIds", memories.stream().map(Memory::getId).collect(Collectors.toList()));

        log.info("EXPORT: user={} data generated, outings={}, memories={}",
                userId, outings.size(), memories.size());

        return Map.of("status", "processing", "estimatedMinutes", 5);
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
