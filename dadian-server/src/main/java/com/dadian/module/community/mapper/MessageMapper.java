package com.dadian.module.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dadian.module.community.model.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT DISTINCT ON (CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END) " +
            "CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END AS other_user_id, " +
            "id, sender_id, receiver_id, content, read_at, created_at " +
            "FROM messages " +
            "WHERE sender_id = #{userId} OR receiver_id = #{userId} " +
            "ORDER BY CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END, created_at DESC")
    List<Message> findInbox(String userId);
}
