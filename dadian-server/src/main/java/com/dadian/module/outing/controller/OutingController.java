package com.dadian.module.outing.controller;

import com.dadian.common.ApiResponse;
import com.dadian.common.BusinessException;
import com.dadian.common.ErrorCode;
import com.dadian.module.outing.model.*;
import com.dadian.module.outing.service.*;
import com.dadian.module.user.mapper.UserMapper;
import com.dadian.module.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/outings")
@RequiredArgsConstructor
public class OutingController {
    private final OutingService outingService;
    private final MissionService missionService;
    private final SpotService spotService;
    private final UserMapper userMapper;

    @PostMapping
    public ApiResponse<OutingResponse> create(@RequestBody CreateOutingRequest req, @AuthenticationPrincipal String userId) {
        Outing outing = outingService.create(req, userId);
        return ApiResponse.ok(buildResponse(outing));
    }

    @GetMapping
    public ApiResponse<List<OutingResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal String userId) {
        List<Outing> outings = outingService.listByUser(userId, status, cursor, limit);
        return ApiResponse.ok(outings.stream().map(this::buildResponse).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<OutingResponse> get(@PathVariable String id) {
        Outing outing = outingService.getById(id);
        return ApiResponse.ok(buildResponse(outing));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<OutingResponse> start(@PathVariable String id, @AuthenticationPrincipal String userId) {
        Outing outing = outingService.activate(id, userId);
        missionService.generateForOuting(id);
        return ApiResponse.ok(buildResponse(outing));
    }

    @PostMapping("/{id}/pause")
    public ApiResponse<?> pause(@PathVariable String id, @AuthenticationPrincipal String userId) {
        outingService.pause(id, userId);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/resume")
    public ApiResponse<?> resume(@PathVariable String id, @AuthenticationPrincipal String userId) {
        outingService.resume(id, userId);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<?> complete(@PathVariable String id, @AuthenticationPrincipal String userId) {
        outingService.complete(id, userId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> cancel(@PathVariable String id, @AuthenticationPrincipal String userId) {
        outingService.cancel(id, userId);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/route")
    public ApiResponse<RouteResponse> getRoute(@PathVariable String id) {
        Route route = outingService.getRoute(id);
        if (route == null) return ApiResponse.ok(null);
        List<Waypoint> waypoints = outingService.getWaypoints(route.getId());
        RouteResponse r = new RouteResponse();
        r.setId(route.getId());
        r.setNeonColor(route.getNeonColor());
        r.setTotalDistanceM(route.getTotalDistanceM());
        r.setPolyline(route.getPolyline());
        r.setWaypoints(waypoints.stream().map(w -> {
            RouteResponse.WaypointInfo wi = new RouteResponse.WaypointInfo();
            wi.setId(w.getId()); wi.setSpotId(w.getSpotId()); wi.setType(w.getType());
            wi.setSeq(w.getSeq()); wi.setLat(w.getLat()); wi.setLng(w.getLng());
            return wi;
        }).toList());
        return ApiResponse.ok(r);
    }

    @GetMapping("/{id}/missions")
    public ApiResponse<List<MissionResponse>> getMissions(@PathVariable String id, @AuthenticationPrincipal String userId) {
        Participant participant = outingService.getParticipant(id, userId);
        Map<String, Mission> missionMap = missionService.getMissionMap(id);
        List<ParticipantMission> pms = participant != null ? missionService.getParticipantMissions(participant.getId()) : List.of();
        Map<String, ParticipantMission> pmMap = new HashMap<>();
        for (ParticipantMission pm : pms) pmMap.put(pm.getMissionId(), pm);

        List<MissionResponse> list = new ArrayList<>();
        for (Mission m : missionMap.values()) {
            MissionResponse mr = new MissionResponse();
            mr.setId(m.getId()); mr.setOutingId(m.getOutingId()); mr.setWaypointId(m.getWaypointId());
            mr.setType(m.getType()); mr.setTitle(m.getTitle()); mr.setDescription(m.getDescription());
            mr.setReward(m.getReward()); mr.setAssignedRole(m.getAssignedRole());
            mr.setTriggerRadiusM(m.getTriggerRadiusM()); mr.setRequiredPhoto(m.getRequiredPhoto());
            mr.setStatus(m.getStatus()); mr.setCompletedAt(m.getCompletedAt());
            ParticipantMission pm = pmMap.get(m.getId());
            if (pm != null) mr.setParticipantStatus(pm.getStatus());
            else mr.setParticipantStatus("available");
            list.add(mr);
        }
        return ApiResponse.ok(list);
    }

    @PostMapping("/{id}/missions/{mid}/accept")
    public ApiResponse<?> acceptMission(@PathVariable String id, @PathVariable String mid, @AuthenticationPrincipal String userId) {
        Participant p = getRequireParticipant(id, userId);
        missionService.accept(mid, p.getId());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/missions/{mid}/skip")
    public ApiResponse<?> skipMission(@PathVariable String id, @PathVariable String mid, @AuthenticationPrincipal String userId) {
        Participant p = getRequireParticipant(id, userId);
        missionService.skip(mid, p.getId());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/missions/{mid}/complete")
    public ApiResponse<?> completeMission(@PathVariable String id, @PathVariable String mid,
                                          @AuthenticationPrincipal String userId,
                                          @RequestBody(required = false) MissionCompleteRequest body) {
        Participant p = getRequireParticipant(id, userId);
        String photoUrl = body != null ? body.getProofPhotoUrl() : null;
        missionService.complete(mid, p.getId(), photoUrl);
        return ApiResponse.ok();
    }

    private Participant getRequireParticipant(String outingId, String userId) {
        Participant p = outingService.getParticipant(outingId, userId);
        if (p == null) throw new BusinessException(ErrorCode.FORBIDDEN, "你不是本次出行的参与者");
        return p;
    }

    private OutingResponse buildResponse(Outing outing) {
        OutingResponse r = new OutingResponse();
        r.setId(outing.getId()); r.setCreatorId(outing.getCreatorId());
        r.setMode(outing.getMode()); r.setStatus(outing.getStatus()); r.setTitle(outing.getTitle());
        r.setDestinationSpotId(outing.getDestinationSpotId());
        if (outing.getDestinationSpotId() != null) {
            Spot spot = spotService.findById(outing.getDestinationSpotId());
            r.setDestinationSpotName(spot.getName());
        }
        List<Participant> participants = outingService.getParticipants(outing.getId());
        r.setParticipants(participants.stream().map(p -> {
            OutingResponse.ParticipantInfo pi = new OutingResponse.ParticipantInfo();
            pi.setId(p.getId()); pi.setUserId(p.getUserId()); pi.setRole(p.getRole()); pi.setSocialEnergy(p.getSocialEnergy());
            User u = userMapper.selectById(p.getUserId());
            if (u != null) pi.setDisplayName(u.getDisplayName());
            return pi;
        }).toList());
        r.setStartedAt(outing.getStartedAt()); r.setEndedAt(outing.getEndedAt()); r.setCreatedAt(outing.getCreatedAt());
        return r;
    }
}
