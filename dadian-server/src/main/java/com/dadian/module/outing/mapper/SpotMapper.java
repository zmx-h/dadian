package com.dadian.module.outing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dadian.module.outing.model.Spot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SpotMapper extends BaseMapper<Spot> {

    default List<Spot> findNearby(double lat, double lng, double radiusKm, int limit) {
        double dlat = radiusKm / 111.0;
        double dlng = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));
        Page<Spot> page = new Page<>(1, limit);
        return selectPage(page,
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Spot>()
                .between(Spot::getLat, lat - dlat, lat + dlat)
                .between(Spot::getLng, lng - dlng, lng + dlng)
                .orderByDesc(Spot::getRating)
        ).getRecords();
    }
}
