package com.dadian.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dadian.module.user.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
