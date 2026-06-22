package com.dadian.module.outing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dadian.common.BusinessException;
import com.dadian.common.ErrorCode;
import com.dadian.module.outing.mapper.FootprintMapper;
import com.dadian.module.outing.mapper.SpotMapper;
import com.dadian.module.outing.model.Footprint;
import com.dadian.module.outing.model.FootprintRequest;
import com.dadian.module.outing.model.Spot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FootprintService {
    private final FootprintMapper footprintMapper;
    private final SpotMapper spotMapper;

    public Footprint checkin(FootprintRequest req, String userId) {
        Spot spot = spotMapper.selectById(req.getSpotId());
        if (spot == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "地点不存在");

        long existingCount = footprintMapper.selectCount(new LambdaQueryWrapper<Footprint>()
            .eq(Footprint::getSpotId, req.getSpotId()));
        Footprint fp = new Footprint();
        fp.setUserId(userId);
        fp.setOutingId(req.getOutingId());
        fp.setSpotId(req.getSpotId());
        fp.setPhotoUrl(req.getPhotoUrl());
        fp.setComment(req.getComment());
        fp.setDailySeq((int) existingCount + 1);
        fp.setId(java.util.UUID.randomUUID().toString());
        footprintMapper.insert(fp);
        return fp;
    }

    public List<Footprint> getBySpot(String spotId, int limit) {
        return footprintMapper.selectList(new LambdaQueryWrapper<Footprint>()
            .eq(Footprint::getSpotId, spotId)
            .orderByDesc(Footprint::getCreatedAt)
            .last("LIMIT " + Math.min(limit, 20)));
    }

    public List<String> friendsHere(String spotId, String outingId) {
        return List.of(); // MVP returns empty per spec
    }
}
