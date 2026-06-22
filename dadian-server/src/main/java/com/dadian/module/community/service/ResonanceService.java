package com.dadian.module.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dadian.common.BusinessException;
import com.dadian.common.ErrorCode;
import com.dadian.module.user.mapper.UserMapper;
import com.dadian.module.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ResonanceService {

    private final UserMapper userMapper;

    public List<Map<String, Object>> getResonantUsers(String userId, int limit) {
        User currentUser = userMapper.selectById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }

        List<User> allUsers = userMapper.selectList(new LambdaQueryWrapper<User>()
                .ne(User::getId, userId)
                .isNull(User::getDeletedAt));

        List<Map<String, Object>> results = new ArrayList<>();
        for (User other : allUsers) {
            int score = computeScore(currentUser, other);
            if (score > 0) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("userId", other.getId());
                entry.put("displayName", other.getDisplayName());
                entry.put("avatarUrl", other.getAvatarUrl());
                entry.put("bio", other.getBio());
                entry.put("score", score);
                results.add(entry);
            }
        }

        results.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));

        if (results.size() > limit) {
            return results.subList(0, limit);
        }
        return results;
    }

    public Map<String, Object> getMatchingScore(String user1Id, String user2Id) {
        User user1 = userMapper.selectById(user1Id);
        User user2 = userMapper.selectById(user2Id);
        if (user1 == null || user2 == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }

        int score = computeScore(user1, user2);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user1Id", user1Id);
        result.put("user2Id", user2Id);
        result.put("score", score);
        return result;
    }

    private int computeScore(User u1, User u2) {
        int score = 0;

        // social_trait match (int values)
        if (u1.getSocialTrait() != null && u2.getSocialTrait() != null) {
            int diff = Math.abs(u1.getSocialTrait() - u2.getSocialTrait());
            if (diff == 0) {
                score += 25;
            } else if (diff <= 1) {
                score += 15;
            }
        }

        // weekend_style match
        if (u1.getWeekendStyle() != null && u2.getWeekendStyle() != null) {
            int diff = Math.abs(u1.getWeekendStyle() - u2.getWeekendStyle());
            if (diff == 0) {
                score += 25;
            } else if (diff <= 1) {
                score += 15;
            }
        }

        // crowd_feeling match
        if (u1.getCrowdFeeling() != null && u2.getCrowdFeeling() != null) {
            int diff = Math.abs(u1.getCrowdFeeling() - u2.getCrowdFeeling());
            if (diff == 0) {
                score += 25;
            } else if (diff <= 1) {
                score += 15;
            }
        }

        // companion_tone similarity
        if (u1.getCompanionTone() != null && u2.getCompanionTone() != null) {
            if (u1.getCompanionTone().equals(u2.getCompanionTone())) {
                score += 25;
            }
        }

        return score;
    }
}
