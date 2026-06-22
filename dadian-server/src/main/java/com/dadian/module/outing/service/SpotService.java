package com.dadian.module.outing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dadian.common.BusinessException;
import com.dadian.common.ErrorCode;
import com.dadian.module.outing.mapper.SpotMapper;
import com.dadian.module.outing.model.Spot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpotService {
    private final SpotMapper spotMapper;

    public Spot findById(String id) {
        Spot spot = spotMapper.selectById(id);
        if (spot == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "地点不存在");
        return spot;
    }

    public List<Spot> findNearby(double lat, double lng, double radiusKm, int limit) {
        return spotMapper.findNearby(lat, lng, radiusKm, limit);
    }

    public List<Spot> listAll() {
        return spotMapper.selectList(new LambdaQueryWrapper<Spot>()
            .orderByDesc(Spot::getRating).last("LIMIT 50"));
    }
}
