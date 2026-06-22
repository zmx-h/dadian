package com.dadian.module.outing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dadian.common.BusinessException;
import com.dadian.common.ErrorCode;
import com.dadian.module.outing.mapper.*;
import com.dadian.module.outing.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutingService {
    private final OutingMapper outingMapper;
    private final ParticipantMapper participantMapper;
    private final RouteMapper routeMapper;
    private final WaypointMapper waypointMapper;
    private final SpotMapper spotMapper;

    @Transactional
    public Outing create(CreateOutingRequest req, String creatorId) {
        Spot spot = spotMapper.selectById(req.getSpotId());
        Outing outing = new Outing();
        outing.setId(java.util.UUID.randomUUID().toString());
        outing.setCreatorId(creatorId);
        outing.setMode(req.getMode() != null ? req.getMode() : "solo");
        outing.setStatus("draft");
        outing.setDestinationSpotId(req.getSpotId());
        outing.setTitle(spot != null ? "前往 " + spot.getName() : "未命名出行");
        outingMapper.insert(outing);

        Participant participant = new Participant();
        participant.setId(java.util.UUID.randomUUID().toString());
        participant.setOutingId(outing.getId());
        participant.setUserId(creatorId);
        participant.setRole(req.getRole() != null ? req.getRole() : "agent");
        participant.setSocialEnergy(req.getEnergy() != null ? req.getEnergy() : 50);
        participantMapper.insert(participant);

        if (spot != null && req.getLat() != null && req.getLng() != null) {
            Route route = new Route();
        route.setId(java.util.UUID.randomUUID().toString());
            route.setOutingId(outing.getId());
            route.setNeonColor("amber");
            routeMapper.insert(route);

            double midLat = (req.getLat() + spot.getLat()) / 2;
            double midLng = (req.getLng() + spot.getLng()) / 2;

            Waypoint start = new Waypoint(); start.setId(java.util.UUID.randomUUID().toString()); start.setRouteId(route.getId()); start.setType("start"); start.setSeq(0); start.setLat(req.getLat()); start.setLng(req.getLng());
            Waypoint mid   = new Waypoint(); mid.setId(java.util.UUID.randomUUID().toString()); mid.setRouteId(route.getId());   mid.setType("branch"); mid.setSeq(1); mid.setLat(midLat); mid.setLng(midLng);
            Waypoint end   = new Waypoint(); end.setId(java.util.UUID.randomUUID().toString()); end.setRouteId(route.getId());   end.setType("destination"); end.setSeq(2); end.setLat(spot.getLat()); end.setLng(spot.getLng());
            waypointMapper.insert(start);
            waypointMapper.insert(mid);
            waypointMapper.insert(end);
        }
        return outing;
    }

    public Outing activate(String outingId, String userId) {
        Outing outing = getById(outingId);
        if (!outing.getCreatorId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        if (!"draft".equals(outing.getStatus()) && !"paused".equals(outing.getStatus()))
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "当前状态不允许开始出行");
        outing.setStatus("active");
        outing.setStartedAt(OffsetDateTime.now());
        outingMapper.updateById(outing);
        return outing;
    }

    public void pause(String outingId, String userId) {
        Outing outing = getById(outingId);
        if (!outing.getCreatorId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        if (!"active".equals(outing.getStatus())) throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "当前状态不允许暂停");
        outing.setStatus("paused");
        outingMapper.updateById(outing);
    }

    public void resume(String outingId, String userId) {
        Outing outing = getById(outingId);
        if (!outing.getCreatorId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        if (!"paused".equals(outing.getStatus())) throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "当前状态不允许恢复");
        outing.setStatus("active");
        outingMapper.updateById(outing);
    }

    public void complete(String outingId, String userId) {
        Outing outing = getById(outingId);
        if (!outing.getCreatorId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        if (!"active".equals(outing.getStatus()) && !"paused".equals(outing.getStatus()))
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "当前状态不允许完成");
        outing.setStatus("completed");
        outing.setEndedAt(OffsetDateTime.now());
        outingMapper.updateById(outing);
    }

    public void cancel(String outingId, String userId) {
        Outing outing = getById(outingId);
        if (!outing.getCreatorId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        if (!"draft".equals(outing.getStatus())) throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "只能取消草稿状态的出行");
        outing.setStatus("cancelled");
        outingMapper.updateById(outing);
    }

    public Outing getById(String id) {
        Outing outing = outingMapper.selectById(id);
        if (outing == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "出行记录不存在");
        return outing;
    }

    public List<Outing> listByUser(String userId, String status, String cursor, int limit) {
        LambdaQueryWrapper<Outing> q = new LambdaQueryWrapper<Outing>()
            .eq(Outing::getCreatorId, userId)
            .orderByDesc(Outing::getCreatedAt);
        if (status != null && !status.isBlank()) q.eq(Outing::getStatus, status);
        if (cursor != null && !cursor.isBlank()) q.lt(Outing::getId, cursor);
        q.last("LIMIT " + Math.min(limit, 50));
        return outingMapper.selectList(q);
    }

    public Participant getParticipant(String outingId, String userId) {
        return participantMapper.selectOne(new LambdaQueryWrapper<Participant>()
            .eq(Participant::getOutingId, outingId).eq(Participant::getUserId, userId));
    }

    public Route getRoute(String outingId) {
        return routeMapper.selectOne(new LambdaQueryWrapper<Route>().eq(Route::getOutingId, outingId));
    }

    public List<Waypoint> getWaypoints(String routeId) {
        return waypointMapper.selectList(new LambdaQueryWrapper<Waypoint>()
            .eq(Waypoint::getRouteId, routeId).orderByAsc(Waypoint::getSeq));
    }

    public List<Participant> getParticipants(String outingId) {
        return participantMapper.selectList(new LambdaQueryWrapper<Participant>()
            .eq(Participant::getOutingId, outingId));
    }
}
