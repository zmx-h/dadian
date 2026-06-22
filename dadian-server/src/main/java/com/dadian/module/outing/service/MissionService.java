package com.dadian.module.outing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dadian.common.BusinessException;
import com.dadian.common.ErrorCode;
import com.dadian.module.outing.mapper.*;
import com.dadian.module.outing.model.*;
import com.dadian.module.user.mapper.UserMapper;
import com.dadian.module.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MissionService {
    private final MissionMapper missionMapper;
    private final ParticipantMissionMapper pmMapper;
    private final ParticipantMapper participantMapper;
    private final UserMapper userMapper;

    public void generateForOuting(String outingId) {
        List<Mission> templates = List.of(
            buildMission(outingId, "anchor", "到达途经点", "前往路线上的途经点", "50 经验"),
            buildMission(outingId, "footprint", "留下足迹", "在途经点拍照打卡", "1 张照片"),
            buildMission(outingId, "easter_egg", "彩蛋：发现隐藏小店", "在途经点半径 100m 内发现一家隐藏小店", "神秘奖励")
        );
        for (Mission m : templates) {
            m.setId(java.util.UUID.randomUUID().toString());
            missionMapper.insert(m);
        }
    }

    private Mission buildMission(String outingId, String type, String title, String desc, String reward) {
        Mission m = new Mission();
        m.setOutingId(outingId);
        m.setType(type);
        m.setTitle(title);
        m.setDescription(desc);
        m.setReward(reward);
        m.setAssignedRole(null);
        m.setTriggerRadiusM(50);
        m.setRequiredPhoto(!"anchor".equals(type));
        m.setStatus("available");
        m.setCreatedAt(OffsetDateTime.now());
        return m;
    }

    public List<Mission> getByOuting(String outingId) {
        return missionMapper.selectList(new LambdaQueryWrapper<Mission>()
            .eq(Mission::getOutingId, outingId));
    }

    public ParticipantMission accept(String missionId, String participantId) {
        Mission mission = missionMapper.selectById(missionId);
        if (mission == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "任务不存在");
        ParticipantMission pm = new ParticipantMission();
        pm.setParticipantId(participantId);
        pm.setMissionId(missionId);
        pm.setStatus("active");
        pm.setId(java.util.UUID.randomUUID().toString());
        pmMapper.insert(pm);
        pm.setId(java.util.UUID.randomUUID().toString());
        return pm;
    }

    public void skip(String missionId, String participantId) {
        ParticipantMission pm = pmMapper.selectOne(new LambdaQueryWrapper<ParticipantMission>()
            .eq(ParticipantMission::getMissionId, missionId)
            .eq(ParticipantMission::getParticipantId, participantId));
        if (pm == null) {
            pm = new ParticipantMission();
            pm.setParticipantId(participantId);
            pm.setMissionId(missionId);
            pm.setStatus("skipped");
        pm.setId(java.util.UUID.randomUUID().toString());
            pmMapper.insert(pm);
        pm.setId(java.util.UUID.randomUUID().toString());
        } else {
            pm.setStatus("skipped");
            pmMapper.updateById(pm);
        }
    }

    public ParticipantMission complete(String missionId, String participantId, String proofPhotoUrl) {
        ParticipantMission pm = pmMapper.selectOne(new LambdaQueryWrapper<ParticipantMission>()
            .eq(ParticipantMission::getMissionId, missionId)
            .eq(ParticipantMission::getParticipantId, participantId));
        if (pm == null) throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "请先接受任务");
        if ("completed".equals(pm.getStatus())) throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "任务已完成");
        pm.setStatus("completed");
        pm.setCompletedAt(OffsetDateTime.now());
        if (proofPhotoUrl != null) pm.setProofPhotoUrl(proofPhotoUrl);
        pmMapper.updateById(pm);

        Mission mission = missionMapper.selectById(missionId);
        if (mission != null) {
            mission.setStatus("completed");
            mission.setCompletedAt(OffsetDateTime.now());
            missionMapper.updateById(mission);
        }
        return pm;
    }

    public List<ParticipantMission> getParticipantMissions(String participantId) {
        return pmMapper.selectList(new LambdaQueryWrapper<ParticipantMission>()
            .eq(ParticipantMission::getParticipantId, participantId));
    }

    public Map<String, Mission> getMissionMap(String outingId) {
        List<Mission> missions = getByOuting(outingId);
        Map<String, Mission> map = new LinkedHashMap<>();
        for (Mission m : missions) map.put(m.getId(), m);
        return map;
    }
}
