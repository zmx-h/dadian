package com.dadian.module.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dadian.common.BusinessException;
import com.dadian.common.ErrorCode;
import com.dadian.module.community.mapper.FollowMapper;
import com.dadian.module.community.model.Follow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowMapper followMapper;

    public void follow(String followerId, String followedId) {
        if (followerId.equals(followedId)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "不能关注自己");
        }
        Follow exists = followMapper.selectOne(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFollowedId, followedId));
        if (exists != null) {
            return; // already following, idempotent
        }
        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFollowedId(followedId);
        follow.setCreatedAt(OffsetDateTime.now());
        followMapper.insert(follow);
    }

    public void unfollow(String followerId, String followedId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFollowedId, followedId);
        followMapper.delete(wrapper);
    }

    public boolean isFollowing(String followerId, String followedId) {
        if (followerId == null || followedId == null) return false;
        return followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFollowedId, followedId)) > 0;
    }

    public List<Follow> getFollowers(String userId, int limit) {
        return followMapper.selectList(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowedId, userId)
                .orderByDesc(Follow::getCreatedAt)
                .last("LIMIT " + Math.min(limit, 50)));
    }

    public List<Follow> getFollowing(String userId, int limit) {
        return followMapper.selectList(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId)
                .orderByDesc(Follow::getCreatedAt)
                .last("LIMIT " + Math.min(limit, 50)));
    }

    public long getFollowerCount(String userId) {
        return followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowedId, userId));
    }

    public long getFollowingCount(String userId) {
        return followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, userId));
    }
}
