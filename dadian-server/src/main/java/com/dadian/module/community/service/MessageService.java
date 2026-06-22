package com.dadian.module.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dadian.common.BusinessException;
import com.dadian.common.ErrorCode;
import com.dadian.module.community.mapper.MessageMapper;
import com.dadian.module.community.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final FollowService followService;

    @Transactional
    public Message send(String senderId, String receiverId, String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "消息内容不能为空");
        }

        // Check mutual follow
        boolean isMutualFollow = followService.isFollowing(senderId, receiverId)
                && followService.isFollowing(receiverId, senderId);
        if (!isMutualFollow) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "需要互相关注才能发送私信");
        }

        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setCreatedAt(OffsetDateTime.now());
        messageMapper.insert(message);
        return message;
    }

    public List<Message> getConversation(String userId1, String userId2, int limit) {
        return messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .and(w -> w.eq(Message::getSenderId, userId1).eq(Message::getReceiverId, userId2)
                        .or(w2 -> w2.eq(Message::getSenderId, userId2).eq(Message::getReceiverId, userId1)))
                .orderByDesc(Message::getCreatedAt)
                .last("LIMIT " + Math.min(limit, 100)));
    }

    public List<Message> getInbox(String userId) {
        return messageMapper.findInbox(userId);
    }

    @Transactional
    public void markRead(String messageId, String userId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "消息不存在");
        }
        if (!message.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        }
        if (message.getReadAt() == null) {
            message.setReadAt(OffsetDateTime.now());
            messageMapper.updateById(message);
        }
    }
}
