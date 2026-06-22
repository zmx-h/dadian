package com.dadian.module.outing.controller;

import com.dadian.common.ApiResponse;
import com.dadian.module.outing.mapper.DiceRecordMapper;
import com.dadian.module.outing.mapper.SpotMapper;
import com.dadian.module.outing.model.DiceRecord;
import com.dadian.module.outing.model.Spot;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DiceController {
    private final DiceRecordMapper diceRecordMapper;
    private final SpotMapper spotMapper;
    private final StringRedisTemplate redis;

    @PostMapping("/dice/roll")
    public ApiResponse<Map<String, Object>> roll(@RequestBody Map<String, Object> body, @AuthenticationPrincipal String userId) {
        String outingId = (String) body.get("outing_id");
        double lat = body.get("lat") != null ? ((Number) body.get("lat")).doubleValue() : 31.22;
        double lng = body.get("lng") != null ? ((Number) body.get("lng")).doubleValue() : 121.46;

        List<Spot> nearby = spotMapper.findNearby(lat, lng, 5, 20);
        Spot picked = nearby.get(ThreadLocalRandom.current().nextInt(nearby.size()));

        DiceRecord dr = new DiceRecord();
        dr.setId(java.util.UUID.randomUUID().toString());
        dr.setUserId(userId);
        dr.setOutingId(outingId);
        dr.setScene("departure");
        dr.setChoice("accepted");
        dr.setResultSpotId(picked.getId());
        dr.setRolledAt(OffsetDateTime.now());
        diceRecordMapper.insert(dr);

        redis.opsForValue().set("dice:lock:" + userId, picked.getId(), 300, TimeUnit.SECONDS);

        return ApiResponse.ok(Map.of("spot", picked, "dice_record_id", dr.getId(), "lock_seconds", 300));
    }

    @PostMapping("/dice/{id}/accept")
    public ApiResponse<?> accept(@PathVariable String id) {
        DiceRecord dr = diceRecordMapper.selectById(id);
        if (dr != null) { dr.setChoice("accepted"); diceRecordMapper.updateById(dr); }
        return ApiResponse.ok();
    }

    @PostMapping("/dice/{id}/escape")
    public ApiResponse<Map<String, Object>> escape(@PathVariable String id) {
        DiceRecord dr = diceRecordMapper.selectById(id);
        if (dr != null) { dr.setChoice("escaped"); diceRecordMapper.updateById(dr); }
        return ApiResponse.ok(Map.of("persuasion_text", "这次我听自己的！", "escape_count", 1, "achievement_progress", 25));
    }

    @GetMapping("/users/me/dice-history")
    public ApiResponse<List<DiceRecord>> history(@AuthenticationPrincipal String userId) {
        return ApiResponse.ok(diceRecordMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DiceRecord>()
                .eq(DiceRecord::getUserId, userId).orderByDesc(DiceRecord::getRolledAt).last("LIMIT 50")));
    }
}
