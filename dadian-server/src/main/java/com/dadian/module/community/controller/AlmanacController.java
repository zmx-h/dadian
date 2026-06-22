package com.dadian.module.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dadian.common.ApiResponse;
import com.dadian.module.outing.mapper.DiceRecordMapper;
import com.dadian.module.outing.mapper.FootprintMapper;
import com.dadian.module.outing.mapper.MemoryMapper;
import com.dadian.module.outing.mapper.ParticipantMapper;
import com.dadian.module.outing.mapper.OutingMapper;
import com.dadian.module.outing.model.DiceRecord;
import com.dadian.module.outing.model.Footprint;
import com.dadian.module.outing.model.Memory;
import com.dadian.module.outing.model.Participant;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/almanac")
@RequiredArgsConstructor
public class AlmanacController {

    private final FootprintMapper footprintMapper;
    private final ParticipantMapper participantMapper;
    private final OutingMapper outingMapper;
    private final DiceRecordMapper diceRecordMapper;
    private final MemoryMapper memoryMapper;

    @GetMapping("/leaderboard")
    public ApiResponse<List<Map<String, Object>>> leaderboard(
            @RequestParam(defaultValue = "agent") String type,
            @RequestParam(defaultValue = "50") int limit) {

        List<Map<String, Object>> result = switch (type) {
            case "agent" -> agentLeaderboard(limit);
            case "foodie" -> foodieLeaderboard(limit);
            case "npc" -> npcLeaderboard(limit);
            case "escape" -> escapeLeaderboard(limit);
            case "wkw" -> wkwLeaderboard(limit);
            default -> List.of();
        };

        return ApiResponse.ok(result);
    }

    // agent: count footprints where user role=agent
    private List<Map<String, Object>> agentLeaderboard(int limit) {
        // Get participants with role=agent
        List<Participant> agents = participantMapper.selectList(
                new LambdaQueryWrapper<Participant>().eq(Participant::getRole, "agent"));

        // Count footprints per user for agent outings
        Map<String, Long> counts = new HashMap<>();
        Set<String> agentOutingIds = agents.stream().map(Participant::getOutingId).collect(Collectors.toSet());

        for (String outingId : agentOutingIds) {
            List<Footprint> fps = footprintMapper.selectList(
                    new LambdaQueryWrapper<Footprint>().eq(Footprint::getOutingId, outingId));
            for (Footprint fp : fps) {
                counts.merge(fp.getUserId(), 1L, Long::sum);
            }
        }

        return buildLeaderboard(counts, limit);
    }

    // foodie: count outings where role=foodie
    private List<Map<String, Object>> foodieLeaderboard(int limit) {
        List<Participant> foodies = participantMapper.selectList(
                new LambdaQueryWrapper<Participant>().eq(Participant::getRole, "foodie"));

        Map<String, Long> counts = foodies.stream()
                .collect(Collectors.groupingBy(Participant::getUserId, Collectors.counting()));

        return buildLeaderboard(counts, limit);
    }

    // npc: count outings where role=npc AND social_energy<30
    private List<Map<String, Object>> npcLeaderboard(int limit) {
        List<Participant> npcs = participantMapper.selectList(
                new LambdaQueryWrapper<Participant>()
                        .eq(Participant::getRole, "npc")
                        .lt(Participant::getSocialEnergy, 30));

        Map<String, Long> counts = npcs.stream()
                .collect(Collectors.groupingBy(Participant::getUserId, Collectors.counting()));

        return buildLeaderboard(counts, limit);
    }

    // escape: count dice_records where choice='escaped'
    private List<Map<String, Object>> escapeLeaderboard(int limit) {
        List<DiceRecord> escapes = diceRecordMapper.selectList(
                new LambdaQueryWrapper<DiceRecord>().eq(DiceRecord::getChoice, "escaped"));

        Map<String, Long> counts = escapes.stream()
                .collect(Collectors.groupingBy(DiceRecord::getUserId, Collectors.counting()));

        return buildLeaderboard(counts, limit);
    }

    // wkw: count memories where style='wangjiawei'
    private List<Map<String, Object>> wkwLeaderboard(int limit) {
        List<Memory> wkwMemories = memoryMapper.selectList(
                new LambdaQueryWrapper<Memory>().eq(Memory::getStyle, "wangjiawei"));

        Map<String, Long> counts = wkwMemories.stream()
                .collect(Collectors.groupingBy(Memory::getUserId, Collectors.counting()));

        return buildLeaderboard(counts, limit);
    }

    private List<Map<String, Object>> buildLeaderboard(Map<String, Long> counts, int limit) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("userId", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }
}
