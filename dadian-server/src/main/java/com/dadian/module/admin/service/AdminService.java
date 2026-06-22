package com.dadian.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dadian.module.admin.model.AdminStatsDTO;
import com.dadian.module.admin.model.DiceConfigDTO;
import com.dadian.module.outing.mapper.CommentMapper;
import com.dadian.module.outing.mapper.MemoryMapper;
import com.dadian.module.outing.mapper.OutingMapper;
import com.dadian.module.outing.model.Comment;
import com.dadian.module.outing.model.Memory;
import com.dadian.module.outing.model.Outing;
import com.dadian.module.user.mapper.UserMapper;
import com.dadian.module.user.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final OutingMapper outingMapper;
    private final MemoryMapper memoryMapper;
    private final CommentMapper commentMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String AI_TOKEN_KEY = "admin:ai:tokens:used";
    private static final String DICE_CONFIG_KEY = "admin:dice:configs";

    public AdminStatsDTO getStats() {
        OffsetDateTime todayStart = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime todayEnd = todayStart.plusDays(1);

        long dau = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .ge(User::getCreatedAt, todayStart)
                        .lt(User::getCreatedAt, todayEnd)
                        .isNull(User::getDeletedAt));

        long outingCount = outingMapper.selectCount(
                new LambdaQueryWrapper<Outing>()
                        .ge(Outing::getCreatedAt, todayStart)
                        .lt(Outing::getCreatedAt, todayEnd));

        long memoryCount = memoryMapper.selectCount(
                new LambdaQueryWrapper<Memory>()
                        .ge(Memory::getGeneratedAt, todayStart)
                        .lt(Memory::getGeneratedAt, todayEnd));

        String tokenStr = redisTemplate.opsForValue().get(AI_TOKEN_KEY);
        long aiTokenUsed = tokenStr != null ? Long.parseLong(tokenStr) : 0L;

        long smsCount = 0L;

        AdminStatsDTO dto = new AdminStatsDTO();
        dto.setDau(dau);
        dto.setOutingCount(outingCount);
        dto.setMemoryCount(memoryCount);
        dto.setAiTokenUsed(aiTokenUsed);
        dto.setSmsCount(smsCount);
        dto.setPeriod("today");
        return dto;
    }

    public List<Memory> getPendingMemories() {
        return memoryMapper.selectList(
                new LambdaQueryWrapper<Memory>()
                        .eq(Memory::getVisibility, "public")
                        .orderByDesc(Memory::getGeneratedAt)
                        .last("LIMIT 20"));
    }

    public void approveMemory(String memoryId, String adminUserId) {
        log.info("ADMIN: memory {} approved by user {}", memoryId, adminUserId);
    }

    public void rejectMemory(String memoryId, String adminUserId) {
        log.info("ADMIN: memory {} rejected by user {}", memoryId, adminUserId);
    }

    public List<Comment> getPendingComments() {
        return commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .orderByDesc(Comment::getCreatedAt)
                        .last("LIMIT 20"));
    }

    public void approveComment(String commentId, String adminUserId) {
        log.info("ADMIN: comment {} approved by user {}", commentId, adminUserId);
    }

    public void rejectComment(String commentId, String adminUserId) {
        log.info("ADMIN: comment {} rejected by user {}", commentId, adminUserId);
    }

    public List<DiceConfigDTO> getDiceConfigs() {
        String json = redisTemplate.opsForValue().get(DICE_CONFIG_KEY);
        if (json != null && !json.isBlank()) {
            try {
                return objectMapper.readValue(json, new TypeReference<List<DiceConfigDTO>>() {});
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse dice configs from Redis, returning defaults", e);
            }
        }
        return getDefaultDiceConfigs();
    }

    public void saveDiceConfigs(List<DiceConfigDTO> configs) {
        try {
            String json = objectMapper.writeValueAsString(configs);
            redisTemplate.opsForValue().set(DICE_CONFIG_KEY, json);
            log.info("ADMIN: dice configs updated, count={}", configs.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize dice configs", e);
            throw new RuntimeException("Failed to save dice configs", e);
        }
    }

    private List<DiceConfigDTO> getDefaultDiceConfigs() {
        DiceConfigDTO c1 = new DiceConfigDTO();
        c1.setKey("persuasion_default");
        c1.setLabel("默认社交能量");
        c1.setContent("来吧，今晚的剧本已经写好了，就差你这一笔。");

        DiceConfigDTO c2 = new DiceConfigDTO();
        c2.setKey("persuasion_wangjiawei");
        c2.setLabel("王家卫风格");
        c2.setContent("有些地方，一个人去太安静了，带上我，一起去热闹一下。");

        DiceConfigDTO c3 = new DiceConfigDTO();
        c3.setKey("persuasion_npc");
        c3.setLabel("NPC风格");
        c3.setContent("系统检测到今晚出门概率87.6%，建议立即执行。");

        return List.of(c1, c2, c3);
    }
}
