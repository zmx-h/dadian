package com.dadian.module.team.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dadian.common.BusinessException;
import com.dadian.common.ErrorCode;
import com.dadian.module.community.service.FollowService;
import com.dadian.module.outing.mapper.OutingMapper;
import com.dadian.module.outing.mapper.ParticipantMapper;
import com.dadian.module.outing.model.Outing;
import com.dadian.module.outing.model.Participant;
import com.dadian.module.team.model.TeamInvitationResponse;
import com.dadian.module.team.model.TeammateInfo;
import com.dadian.module.user.mapper.UserMapper;
import com.dadian.module.user.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private static final String INVITE_KEY_PREFIX = "team:invite:";
    private static final Duration INVITE_TTL = Duration.ofMinutes(30);
    private static final Duration DECLINED_TTL = Duration.ofMinutes(5);

    private final OutingMapper outingMapper;
    private final ParticipantMapper participantMapper;
    private final UserMapper userMapper;
    private final FollowService followService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Invite a user to join a team outing.
     */
    @Transactional
    public TeamInvitationResponse invite(String outingId, String inviterId, String inviteeId) {
        // 1. Validate outing exists
        Outing outing = outingMapper.selectById(outingId);
        if (outing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "出行记录不存在");
        }

        // 2. Check mode is team mode
        if (!"team_local".equals(outing.getMode()) && !"team_remote".equals(outing.getMode())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "仅组队出行支持邀请队友");
        }

        // 3. Check inviter is a participant
        Participant inviterParticipant = getParticipant(outingId, inviterId);
        if (inviterParticipant == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "你不是本次出行的参与者，无法邀请");
        }

        // 4. Check invitee is not already a participant
        Participant existing = getParticipant(outingId, inviteeId);
        if (existing != null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "该用户已在队伍中");
        }

        // 5. Check mutual follow (for team_local mode)
        if ("team_local".equals(outing.getMode())) {
            boolean mutual = checkMutualFollow(inviterId, inviteeId);
            if (!mutual) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "仅互相关注的用户可邀请线下组队");
            }
        }

        // 6. Build invitation and store in Redis
        User inviter = userMapper.selectById(inviterId);
        String inviterName = inviter != null ? inviter.getDisplayName() : "未知用户";

        Map<String, Object> inviteData = new LinkedHashMap<>();
        inviteData.put("inviterId", inviterId);
        inviteData.put("inviterName", inviterName);
        inviteData.put("outingTitle", outing.getTitle());
        inviteData.put("status", "pending");
        inviteData.put("createdAt", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        String redisKey = INVITE_KEY_PREFIX + outingId + ":" + inviteeId;
        try {
            String json = objectMapper.writeValueAsString(inviteData);
            redisTemplate.opsForValue().set(redisKey, json, INVITE_TTL);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化邀请数据失败", e);
        }

        // 7. Return response
        TeamInvitationResponse resp = new TeamInvitationResponse();
        resp.setOutingId(outingId);
        resp.setInviterId(inviterId);
        resp.setInviterName(inviterName);
        resp.setOutingTitle(outing.getTitle());
        resp.setInviteeId(inviteeId);
        resp.setStatus("pending");
        resp.setCreatedAt(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return resp;
    }

    /**
     * Accept or decline a team invitation.
     */
    @Transactional
    public TeamInvitationResponse respond(String outingId, String inviteeId, boolean accept) {
        String redisKey = INVITE_KEY_PREFIX + outingId + ":" + inviteeId;
        String json = redisTemplate.opsForValue().get(redisKey);

        if (json == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "邀请不存在或已过期");
        }

        Map<String, Object> inviteData;
        try {
            inviteData = objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("反序列化邀请数据失败", e);
        }

        String status = (String) inviteData.get("status");
        if (!"pending".equals(status)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "该邀请已处理");
        }

        if (accept) {
            // Add participant row
            Outing outing = outingMapper.selectById(outingId);
            if (outing == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "出行记录不存在");
            }

            String inviterId = (String) inviteData.get("inviterId");
            Participant inviter = getParticipant(outingId, inviterId);

            Participant participant = new Participant();
            participant.setId(UUID.randomUUID().toString());
            participant.setOutingId(outingId);
            participant.setUserId(inviteeId);
            participant.setRole("companion");
            participant.setSocialEnergy(inviter != null ? inviter.getSocialEnergy() : 50);
            participant.setIsCompleted(false);
            participant.setCreatedAt(OffsetDateTime.now());
            participantMapper.insert(participant);

            inviteData.put("status", "accepted");
            // Delete the Redis key — invitation is fulfilled
            redisTemplate.delete(redisKey);
        } else {
            inviteData.put("status", "declined");
            // Keep declined keys for 5 min so client can see the result
            try {
                json = objectMapper.writeValueAsString(inviteData);
                redisTemplate.opsForValue().set(redisKey, json, DECLINED_TTL);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("序列化邀请数据失败", e);
            }
        }

        TeamInvitationResponse resp = new TeamInvitationResponse();
        resp.setOutingId(outingId);
        resp.setInviterId((String) inviteData.get("inviterId"));
        resp.setInviterName((String) inviteData.get("inviterName"));
        resp.setOutingTitle((String) inviteData.get("outingTitle"));
        resp.setInviteeId(inviteeId);
        resp.setStatus((String) inviteData.get("status"));
        resp.setCreatedAt((String) inviteData.get("createdAt"));
        return resp;
    }

    /**
     * Get all pending invitations for a user.
     */
    public List<TeamInvitationResponse> getPendingInvitations(String userId) {
        List<TeamInvitationResponse> list = new ArrayList<>();

        // Scan Redis keys matching team:invite:*:userId
        Set<String> keys = redisTemplate.keys(INVITE_KEY_PREFIX + "*:" + userId);
        if (keys == null || keys.isEmpty()) {
            return list;
        }

        for (String key : keys) {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) continue;

            try {
                Map<String, Object> data = objectMapper.readValue(json, Map.class);
                if (!"pending".equals(data.get("status"))) continue;

                // Extract outingId from key: team:invite:{outingId}:{inviteeId}
                String suffix = key.substring(INVITE_KEY_PREFIX.length());
                String[] parts = suffix.split(":", 2);
                String outingId = parts[0];

                TeamInvitationResponse resp = new TeamInvitationResponse();
                resp.setOutingId(outingId);
                resp.setInviterId((String) data.get("inviterId"));
                resp.setInviterName((String) data.get("inviterName"));
                resp.setOutingTitle((String) data.get("outingTitle"));
                resp.setInviteeId(userId);
                resp.setStatus((String) data.get("status"));
                resp.setCreatedAt((String) data.get("createdAt"));
                list.add(resp);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse invitation data for key: {}", key, e);
            }
        }

        return list;
    }

    /**
     * Get all teammates (participants) in an outing.
     */
    public List<TeammateInfo> getTeammates(String outingId) {
        List<Participant> participants = participantMapper.selectList(
                new LambdaQueryWrapper<Participant>()
                        .eq(Participant::getOutingId, outingId));

        List<TeammateInfo> list = new ArrayList<>();
        for (Participant p : participants) {
            User user = userMapper.selectById(p.getUserId());
            TeammateInfo info = new TeammateInfo();
            info.setUserId(p.getUserId());
            info.setDisplayName(user != null ? user.getDisplayName() : "未知用户");
            info.setAvatarUrl(user != null ? user.getAvatarUrl() : null);
            info.setRole(p.getRole());
            info.setSocialEnergy(p.getSocialEnergy());
            info.setOnline(false); // will be set by controller or WebSocket
            list.add(info);
        }

        return list;
    }

    /**
     * Leave an outing (remove self as participant).
     * The creator cannot leave; they must cancel the outing instead.
     */
    @Transactional
    public void leave(String outingId, String userId) {
        Outing outing = outingMapper.selectById(outingId);
        if (outing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "出行记录不存在");
        }

        if (outing.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "发起人无法离队，请取消出行");
        }

        Participant participant = getParticipant(outingId, userId);
        if (participant == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "你不是该出行的参与者");
        }

        participantMapper.deleteById(participant.getId());
    }

    /**
     * Check if two users follow each other (mutual follow).
     */
    public boolean checkMutualFollow(String user1Id, String user2Id) {
        return followService.isFollowing(user1Id, user2Id)
                && followService.isFollowing(user2Id, user1Id);
    }

    private Participant getParticipant(String outingId, String userId) {
        return participantMapper.selectOne(new LambdaQueryWrapper<Participant>()
                .eq(Participant::getOutingId, outingId)
                .eq(Participant::getUserId, userId));
    }
}
