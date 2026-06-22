package com.dadian.module.outing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dadian.common.BusinessException;
import com.dadian.common.ErrorCode;
import com.dadian.module.outing.mapper.*;
import com.dadian.module.outing.model.*;
import com.dadian.module.user.mapper.UserMapper;
import com.dadian.module.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MemoryService {
    private final MemoryMapper memoryMapper;
    private final MemoryPhotoMapper memoryPhotoMapper;
    private final CommentMapper commentMapper;
    private final CommentChargeMapper commentChargeMapper;
    private final OutingMapper outingMapper;
    private final FootprintMapper footprintMapper;
    private final WaypointMapper waypointMapper;
    private final RouteMapper routeMapper;
    private final UserMapper userMapper;
    private final SpotMapper spotMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String[] WANGJIAWEI_TEMPLATES = {
        "那个下午的%s，阳光穿过梧桐叶，碎成了%s的样子。",
        "我以为%s只是一个地点，后来才知道，那是%s的开始。",
        "如果%d分钟后我还记得%s的温度，那一定是%s。",
        "走在%s的路上，周围很热闹，但%s只有自己知道。",
        "所有的相遇都是久别重逢，就像你和%s，隔着%s。",
    };

    @Transactional
    public Memory generate(String outingId, String userId, String style, String visibility) {
        Outing outing = outingMapper.selectById(outingId);
        if (outing == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "出行不存在");
        if (!"completed".equals(outing.getStatus()))
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "出行尚未完成");

        OutingResponse response = buildOutingResponse(outing);
        List<Footprint> footprints = footprintMapper.selectList(
            new LambdaQueryWrapper<Footprint>().eq(Footprint::getOutingId, outingId));
        Spot spot = outing.getDestinationSpotId() != null ?
            spotMapper.selectById(outing.getDestinationSpotId()) : null;

        String title = spot != null ? spot.getName() + " · 记忆碎片" : "记忆碎片";
        String summary = generateSummary(spot, footprints, response);

        Memory memory = new Memory();
        memory.setOutingId(outingId);
        memory.setUserId(userId);
        memory.setTitle(title);
        memory.setStyle(style != null ? style : "wangjiawei");
        memory.setSummary(summary);
        memory.setVisibility(visibility != null ? visibility : "private");
        memory.setIsSynthetic(false);
        memory.setGeneratedAt(OffsetDateTime.now());
        memoryMapper.insert(memory);

        int seq = 0;
        for (Footprint fp : footprints) {
            MemoryPhoto photo = new MemoryPhoto();
            photo.setMemoryId(memory.getId());
            photo.setUrl(fp.getPhotoUrl() != null ? fp.getPhotoUrl() : "");
            photo.setCaption("第" + (seq + 1) + "张记忆");
            photo.setStyle(memory.getStyle());
            photo.setSpotName(fp.getSpotId() != null ? fp.getSpotId() : "");
            photo.setSeq(seq++);
            photo.setTakenAt(fp.getCreatedAt());
            memoryPhotoMapper.insert(photo);
        }

        return memory;
    }

    private String generateSummary(Spot spot, List<Footprint> footprints, OutingResponse r) {
        if (spot == null && footprints.isEmpty()) return "这一天，我出发了，又回来了。";
        String spotName = spot != null ? spot.getName() : "某个角落";
        int photoCount = footprints.size();
        return String.format("在那条通往%s的路上，留下了%d个瞬间。%s的故事，就这样被记住了。",
            spotName, photoCount, placeWord());
    }

    private String placeWord() {
        String[] words = {"梧桐树下", "黄昏时分", "霓虹灯里", "老街上", "咖啡馆的角落", "江边"};
        return words[ThreadLocalRandom.current().nextInt(words.length)];
    }

    public Memory findById(String id) {
        Memory m = memoryMapper.selectById(id);
        if (m == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "回忆不存在");
        return m;
    }

    public List<Memory> listByUser(String userId, String visibility, String cursor, int limit) {
        LambdaQueryWrapper<Memory> q = new LambdaQueryWrapper<Memory>()
            .eq(Memory::getUserId, userId).orderByDesc(Memory::getGeneratedAt);
        if (visibility != null && !visibility.isBlank()) q.eq(Memory::getVisibility, visibility);
        if (cursor != null && !cursor.isBlank()) q.lt(Memory::getId, cursor);
        q.last("LIMIT " + Math.min(limit, 20));
        return memoryMapper.selectList(q);
    }

    public List<Memory> listPublic(String cursor, int limit) {
        LambdaQueryWrapper<Memory> q = new LambdaQueryWrapper<Memory>()
            .eq(Memory::getVisibility, "public").orderByDesc(Memory::getGeneratedAt);
        if (cursor != null && !cursor.isBlank()) q.lt(Memory::getId, cursor);
        q.last("LIMIT " + Math.min(limit, 20));
        return memoryMapper.selectList(q);
    }

    public List<MemoryPhoto> getPhotos(String memoryId) {
        return memoryPhotoMapper.selectList(
            new LambdaQueryWrapper<MemoryPhoto>().eq(MemoryPhoto::getMemoryId, memoryId).orderByAsc(MemoryPhoto::getSeq));
    }

    public void updateVisibility(String memoryId, String userId, String visibility) {
        Memory m = findById(memoryId);
        if (!m.getUserId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        m.setVisibility(visibility);
        memoryMapper.updateById(m);
    }

    public void delete(String memoryId, String userId) {
        Memory m = findById(memoryId);
        if (!m.getUserId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        memoryPhotoMapper.delete(new LambdaQueryWrapper<MemoryPhoto>().eq(MemoryPhoto::getMemoryId, memoryId));
        memoryMapper.deleteById(memoryId);
    }

    // ─── Comments ───

    public Comment addComment(String memoryId, String userId, String content) {
        findById(memoryId);
        Comment c = new Comment();
        c.setMemoryId(memoryId);
        c.setUserId(userId);
        c.setContent(content);
        c.setCreatedAt(OffsetDateTime.now());
        commentMapper.insert(c);
        return c;
    }

    public List<Comment> getComments(String memoryId, String userId) {
        List<Comment> list = commentMapper.selectList(
            new LambdaQueryWrapper<Comment>().eq(Comment::getMemoryId, memoryId).orderByDesc(Comment::getCreatedAt));
        for (Comment c : list) {
            User u = userMapper.selectById(c.getUserId());
            c.setUserName(u != null ? u.getDisplayName() : "已注销用户");
            long count = commentChargeMapper.selectCount(
                new LambdaQueryWrapper<CommentCharge>().eq(CommentCharge::getCommentId, c.getId()));
            c.setChargeCount((int) count);
            if (userId != null) {
                boolean charged = commentChargeMapper.exists(
                    new LambdaQueryWrapper<CommentCharge>().eq(CommentCharge::getCommentId, c.getId())
                        .eq(CommentCharge::getUserId, userId));
                c.setChargedByMe(charged);
            }
        }
        return list;
    }

    public boolean toggleCharge(String commentId, String userId) {
        CommentCharge exists = commentChargeMapper.selectOne(
            new LambdaQueryWrapper<CommentCharge>().eq(CommentCharge::getCommentId, commentId)
                .eq(CommentCharge::getUserId, userId));
        if (exists != null) {
            commentChargeMapper.delete(
                    new LambdaQueryWrapper<CommentCharge>().eq(CommentCharge::getCommentId, commentId)
                            .eq(CommentCharge::getUserId, userId));
            return false;
        }
        CommentCharge cc = new CommentCharge();
        cc.setCommentId(commentId);
        cc.setUserId(userId);
        cc.setCreatedAt(OffsetDateTime.now());
        commentChargeMapper.insert(cc);
        return true;
    }

    private OutingResponse buildOutingResponse(Outing o) {
        OutingResponse r = new OutingResponse();
        r.setId(o.getId()); r.setCreatorId(o.getCreatorId()); r.setMode(o.getMode());
        r.setStatus(o.getStatus()); r.setTitle(o.getTitle());
        r.setDestinationSpotId(o.getDestinationSpotId());
        r.setStartedAt(o.getStartedAt()); r.setEndedAt(o.getEndedAt());
        return r;
    }

    // ─── Collect & Replay ───

    @Transactional
    public Memory collectOuting(String outingId, String userId) {
        Outing outing = outingMapper.selectById(outingId);
        if (outing == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "出行不存在");

        Memory memory = new Memory();
        memory.setOutingId(outingId);
        memory.setUserId(userId);
        memory.setTitle((outing.getTitle() != null ? outing.getTitle() : "未命名出行") + " · 剧本");
        memory.setStyle("wangjiawei");
        memory.setSummary("收藏的剧本，随时可以重放。");
        memory.setVisibility("private");
        memory.setIsSynthetic(true);
        memory.setGeneratedAt(OffsetDateTime.now());
        memoryMapper.insert(memory);
        return memory;
    }

    public List<Memory> listCollectedScripts(String userId) {
        return memoryMapper.selectList(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getIsSynthetic, true)
                .orderByDesc(Memory::getGeneratedAt));
    }
}
