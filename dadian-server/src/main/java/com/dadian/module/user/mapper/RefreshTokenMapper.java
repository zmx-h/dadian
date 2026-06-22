package com.dadian.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dadian.module.user.model.RefreshToken;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {
}
