# 搭电 (Dā Diàn) — 数据与存储设计

> 版本: v1.6 | 日期: 2026-06-21 | 数据库: PostgreSQL 16 + PostGIS + Redis Streams + 阿里云 OpenSearch

---

## 1. PostgreSQL Schema

### 1.1 核心表

```sql
-- ═══════════════════════════════════════════
-- 用户与档案
-- ═══════════════════════════════════════════

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone         VARCHAR(20) UNIQUE NOT NULL,   -- 用于登录时匹配：收到验证码的手机号 → 定位用户
    phone_hash    VARCHAR(64) UNIQUE,            -- SHA-256(phone)，用于唯一性检查，替代明文遍历
    phone_encrypted BYTEA,                        -- AES-256-GCM 加密存储（密钥托管阿里云 KMS），用于合规数据导出
    display_name  VARCHAR(40) NOT NULL,
    avatar_url    TEXT,
    bio           VARCHAR(120),
    -- Onboarding 三问结果
    social_trait      SMALLINT CHECK (social_trait BETWEEN 1 AND 3),   -- 1:独处 2:三两好友 3:人越多越好
    weekend_style     SMALLINT CHECK (weekend_style BETWEEN 1 AND 3),  -- 1:咖啡书店 2:街头漫游 3:商圈夜市
    crowd_feeling     SMALLINT CHECK (crowd_feeling BETWEEN 1 AND 3),  -- 1:想躲开 2:看心情 3:很自在
    -- 搭电人格（运行时逐渐调校）
    companion_tone       VARCHAR(12) DEFAULT 'gentle',  -- gentle/playful/dry/humorous
    companion_intensity  SMALLINT DEFAULT 50,            -- 0(安静陪伴) ~ 100(高频推送)
    humor_level          SMALLINT DEFAULT 30,
    -- 隐私
    achievement_visibility VARCHAR(10) DEFAULT 'private', -- private/friends_only/public
    location_retention     INTERVAL DEFAULT '7 days',     -- 位置保留时长
    created_at    TIMESTAMPTZ DEFAULT now(),
    updated_at    TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_users_phone ON users(phone);
CREATE UNIQUE INDEX idx_users_phone_hash ON users(phone_hash);
-- phone_hash 用于唯一性检查，替代明文 phone 遍历；phone 明文仅在登录验证码匹配时使用

-- ═══════════════════════════════════════════
-- 出行
-- ═══════════════════════════════════════════

CREATE TYPE outing_mode AS ENUM ('solo', 'team_local', 'team_remote');
CREATE TYPE outing_status AS ENUM ('draft', 'active', 'paused', 'completed', 'cancelled');

CREATE TABLE outings (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id    UUID NOT NULL REFERENCES users(id),
    mode          outing_mode NOT NULL DEFAULT 'solo',
    status        outing_status NOT NULL DEFAULT 'draft',
    title         VARCHAR(100),
    dice_result_id UUID,          -- → dice_records.id，可为 NULL（不摇骰子直接出发）；不加 REFERENCES 因为骰子结果可能被用户拒绝后删除记录，但出行仍保留
    destination_spot_id UUID REFERENCES spots(id),  -- 最终目的地（骰子结果或用户自选）
    started_at    TIMESTAMPTZ,
    ended_at      TIMESTAMPTZ,
    created_at    TIMESTAMPTZ DEFAULT now(),
    updated_at    TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_outings_creator ON outings(creator_id);
CREATE INDEX idx_outings_status ON outings(status);

-- ═══════════════════════════════════════════
-- 参与者（用户在某次出行中的角色）
-- ═══════════════════════════════════════════

CREATE TYPE outing_role AS ENUM ('agent', 'foodie', 'npc');

CREATE TABLE participants (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outing_id      UUID NOT NULL REFERENCES outings(id) ON DELETE CASCADE,
    user_id        UUID NOT NULL REFERENCES users(id),
    role           outing_role NOT NULL,
    social_energy  SMALLINT NOT NULL CHECK (social_energy BETWEEN 10 AND 100),
    role_score     INT DEFAULT 0,           -- 当前角色积分（转职后重置）
    is_completed   BOOLEAN DEFAULT false,   -- 是否走完全程
    created_at     TIMESTAMPTZ DEFAULT now(),
    UNIQUE (outing_id, user_id)
);
CREATE INDEX idx_participants_outing ON participants(outing_id);
CREATE INDEX idx_participants_user ON participants(user_id);

-- ═══════════════════════════════════════════
-- 转职记录
-- ═══════════════════════════════════════════

CREATE TYPE switch_trigger AS ENUM ('manual', 'auto_suggest');

CREATE TABLE role_switches (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    participant_id UUID NOT NULL REFERENCES participants(id) ON DELETE CASCADE,
    from_role      outing_role NOT NULL,
    to_role        outing_role NOT NULL,
    trigger        switch_trigger NOT NULL DEFAULT 'manual',
    narrative      TEXT,                 -- AI 生成的转职文案
    switched_at    TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_role_switches_participant ON role_switches(participant_id);

-- ═══════════════════════════════════════════
-- POI 与路线
-- ═══════════════════════════════════════════

CREATE TYPE spot_category AS ENUM ('cafe', 'restaurant', 'bar', 'bookstore', 'attraction', 'teahouse', 'other');
CREATE TYPE crowd_density AS ENUM ('low', 'medium', 'high');

CREATE TABLE spots (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    category    spot_category NOT NULL DEFAULT 'other',
    lat         DOUBLE PRECISION NOT NULL,
    lng         DOUBLE PRECISION NOT NULL,
    geom        GEOGRAPHY(POINT, 4326),       -- PostGIS 地理类型
    address     TEXT,
    city        VARCHAR(40),
    crowd_level crowd_density,
    rating      DECIMAL(2,1) CHECK (rating BETWEEN 0 AND 5),
    tags        TEXT[],
    highlight   TEXT,                          -- 亮点描述
    image_url   TEXT,
    -- 高德数据回填（主数据源）
    source_amap      JSONB,                    -- {poi_id, navi_lat, navi_lng, biz_hours}
    -- AI 生成的搭电专属描述（DeepSeek 离线批量生成）
    dadian_tags      TEXT[],                   -- 如「独立小店」「手冲咖啡」
    dadian_highlight TEXT,                     -- 搭电话术亮点
    daily_checkin_count INT DEFAULT 0,         -- 当日打卡人次（Redis 同步重置）
    last_synced_at TIMESTAMPTZ,
    created_at     TIMESTAMPTZ DEFAULT now(),
    updated_at     TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_spots_geom ON spots USING GIST(geom);
CREATE INDEX idx_spots_category ON spots(category);
CREATE INDEX idx_spots_city ON spots(city);
CREATE INDEX idx_spots_name_search ON spots USING gin(to_tsvector('simple', name));

-- ═══════════════════════════════════════════
-- 路线与途经点
-- ═══════════════════════════════════════════

CREATE TABLE routes (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outing_id     UUID NOT NULL UNIQUE REFERENCES outings(id) ON DELETE CASCADE,
    neon_color    VARCHAR(10) DEFAULT 'amber',      -- violet/amber/orange
    total_distance_m INT,                            -- 米
    polyline      TEXT,                              -- 编码的路线 polyline
    created_at    TIMESTAMPTZ DEFAULT now()
);

CREATE TYPE waypoint_type AS ENUM ('start', 'anchor', 'easter_egg', 'branch', 'destination');

CREATE TABLE waypoints (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id    UUID NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    spot_id     UUID REFERENCES spots(id),
    type        waypoint_type NOT NULL,
    seq         SMALLINT NOT NULL,                 -- 顺序
    lat         DOUBLE PRECISION NOT NULL,
    lng         DOUBLE PRECISION NOT NULL,
    UNIQUE (route_id, seq)
);
CREATE INDEX idx_waypoints_route ON waypoints(route_id);

-- ═══════════════════════════════════════════
-- 任务
-- ═══════════════════════════════════════════

CREATE TYPE mission_type AS ENUM ('anchor', 'easter_egg', 'footprint');

CREATE TABLE missions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outing_id     UUID NOT NULL REFERENCES outings(id) ON DELETE CASCADE,
    waypoint_id   UUID REFERENCES waypoints(id),
    type          mission_type NOT NULL,
    title         VARCHAR(200) NOT NULL,
    description   TEXT,
    reward        VARCHAR(100),
    assigned_role outing_role,                    -- 指定给哪个角色
    trigger_radius_m INT DEFAULT 50,              -- 触发半径（米）
    required_photo BOOLEAN DEFAULT false,         -- 是否需要拍照
    status        VARCHAR(20) DEFAULT 'available',
    completed_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_missions_outing ON missions(outing_id);
CREATE INDEX idx_missions_waypoint ON missions(waypoint_id);

CREATE TABLE participant_missions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    participant_id UUID NOT NULL REFERENCES participants(id) ON DELETE CASCADE,
    mission_id     UUID NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    status         VARCHAR(20) DEFAULT 'available',  -- available/active/completed/skipped
    completed_at   TIMESTAMPTZ,
    proof_photo_url TEXT,                            -- 完成打卡的证明照片
    UNIQUE (participant_id, mission_id)
);
CREATE INDEX idx_pm_participant ON participant_missions(participant_id);

-- ═══════════════════════════════════════════
-- 打卡与足迹
-- ═══════════════════════════════════════════

CREATE TABLE footprints (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id),
    outing_id     UUID REFERENCES outings(id),
    spot_id       UUID NOT NULL REFERENCES spots(id),
    photo_url     TEXT,
    comment       VARCHAR(200),                  -- 留给后人的便利贴
    daily_seq      INT,                           -- 当日该地点第几个打卡
    created_at    TIMESTAMPTZ DEFAULT now(),
    UNIQUE (user_id, outing_id, spot_id)          -- 同一出行同一地点只打卡一次
);
CREATE INDEX idx_footprints_spot ON footprints(spot_id);
CREATE INDEX idx_footprints_user ON footprints(user_id);
CREATE INDEX idx_footprints_spot_date ON footprints(spot_id, created_at DESC);

-- ═══════════════════════════════════════════
-- 命运骰子
-- ═══════════════════════════════════════════

CREATE TYPE dice_scene AS ENUM ('departure', 'on_the_way', 'easter_egg');
CREATE TYPE dice_choice AS ENUM ('accepted', 'escaped');
-- 注：产品层术语已从"逃亡"改为"改主意了"/"这次我听自己的"，
-- 但枚举值 escaped 保持向后兼容。业务层在生成文案时做映射：escaped → "改主意了"

CREATE TABLE dice_records (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    outing_id   UUID REFERENCES outings(id),
    scene       dice_scene NOT NULL,
    choice      dice_choice NOT NULL,
    result_spot_id UUID REFERENCES spots(id),
    rolled_at   TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_dice_user ON dice_records(user_id);
CREATE INDEX idx_dice_outing ON dice_records(outing_id);

-- ═══════════════════════════════════════════
-- 回忆录
-- ═══════════════════════════════════════════

CREATE TYPE memory_style AS ENUM ('wangjiawei', 'cyberpunk', 'vinyl');
CREATE TYPE memory_visibility AS ENUM ('private', 'friends_only', 'public');

CREATE TABLE memories (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outing_id     UUID NOT NULL REFERENCES outings(id),
    user_id       UUID REFERENCES users(id),        -- NULL for 合成 Memory
    title         VARCHAR(200),
    style         memory_style NOT NULL DEFAULT 'wangjiawei',
    cover_url     TEXT,
    summary       TEXT,
    visibility    memory_visibility DEFAULT 'private',
    is_synthetic  BOOLEAN DEFAULT false,            -- 异地同框合成 Memory
    stats         JSONB,                             -- {distance, photoCount, missionCount, ...}
    generated_at  TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_memories_user ON memories(user_id);
CREATE INDEX idx_memories_outing ON memories(outing_id);
CREATE INDEX idx_memories_visibility ON memories(visibility);

CREATE TABLE memory_photos (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    memory_id   UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    url         TEXT NOT NULL,
    caption     TEXT,                                -- AI 生成的叙事文案
    style       memory_style,
    vlm_comment TEXT,                                -- VLM 打卡时的即时评价
    spot_name   VARCHAR(100),
    taken_at    TIMESTAMPTZ,
    seq         SMALLINT NOT NULL,
    UNIQUE (memory_id, seq)
);

CREATE TABLE memory_dual_comparisons (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    memory_id         UUID NOT NULL REFERENCES memories(id) ON DELETE CASCADE,
    teammate_name     VARCHAR(40),
    spot_name         VARCHAR(100),
    my_photo_url      TEXT,
    teammate_photo_url TEXT,
    ai_comment        TEXT
);

-- ═══════════════════════════════════════════
-- 成就
-- ═══════════════════════════════════════════

CREATE TABLE achievements (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key           VARCHAR(40) UNIQUE NOT NULL,       -- 程序化 key: escape_master, fulltime_agent
    name          VARCHAR(40) NOT NULL,
    description   VARCHAR(200),
    icon          VARCHAR(10),
    condition_desc TEXT
);

CREATE TABLE user_achievements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    achievement_id  UUID NOT NULL REFERENCES achievements(id),
    unlocked_at     TIMESTAMPTZ DEFAULT now(),
    visibility_override VARCHAR(10),                 -- 单个成就覆盖全局可见性
    UNIQUE (user_id, achievement_id)
);

-- ═══════════════════════════════════════════
-- 社交
-- ═══════════════════════════════════════════

CREATE TABLE follows (
    follower_id  UUID NOT NULL REFERENCES users(id),
    followed_id  UUID NOT NULL REFERENCES users(id),
    created_at   TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (follower_id, followed_id),
    CHECK (follower_id != followed_id)
);

CREATE TABLE messages (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id    UUID NOT NULL REFERENCES users(id),
    receiver_id  UUID NOT NULL REFERENCES users(id),
    content      TEXT NOT NULL,
    read_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_messages_conversation ON messages(
    LEAST(sender_id, receiver_id),
    GREATEST(sender_id, receiver_id),
    created_at DESC
);

CREATE TABLE comments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    memory_id   UUID NOT NULL REFERENCES memories(id),
    content     VARCHAR(100) NOT NULL,              -- 限制 100 字
    created_at  TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_comments_memory ON comments(memory_id);

CREATE TABLE comment_charges (                       -- 留言「充电」（点赞）
    comment_id  UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (comment_id, user_id)
);
```

---

## 2. Redis 缓存设计

### 2.1 缓存键规范

```
┌──────────────────────────────────────────────────────────┐
│ Key Pattern                          │ TTL   │ 说明     │
├──────────────────────────────────────┼───────┼─────────┤
│ session:{token}                      │ 15m   │ JWT 会话 │
│ user:{uid}:profile                   │ 1h    │ 用户档案 │
│ poi:hot:{city}                       │ 1h    │ 城市热门POI │
│ poi:{poi_id}:detail                  │ 24h   │ POI详情  │
│ outing:{oid}:state                   │ 出行期间│ 出行实时状态 │
│ outing:{oid}:teammate_locations      │ 30s   │ 队友位置集合 │
│ dice:lock:{uid}                      │ 5m    │ 骰子软锁 │
│ spot:{sid}:daily_count               │ 1d    │ 当日打卡计数 │
│ rate:ai:{uid}                        │ 1d    │ AI调用限额 │
│ rate:api:{uid}                       │ 1m    │ API限流  │
└──────────────────────────────────────────────────────────┘
```

### 2.2 实时位置共享（Redis Pub/Sub）

位置共享是一种「当前值」场景——旧坐标立刻被新坐标取代，丢失一条历史位置不影响系统，消费端断连后也不需要回溯过去的位置。因此**位置共享保留 Pub/Sub**，不用 Streams 是为避免冗余持久化和消费组开销。

```
Channel: outing:{oid}:locations
Message: {userId, lat, lng, timestamp, accuracy}
```

### 2.3 骰子软锁（Redis）

```redis
SET dice:lock:{uid} {result_spot_id} EX 300 NX
-- 返回 nil 表示已有锁，返回 OK 表示加锁成功
-- TTL 即倒计时秒数
-- 客户端轮询 TTL: GET dice:lock:{uid} + TTL dice:lock:{uid}
-- 或通过 WebSocket 推送倒计时事件
```

---

## 3. 阿里云 OpenSearch 索引设计

用于 POI 搜索和回忆录全文搜索（免运维，中文分词原生支持）：

```json
// spots 索引
{
  "table": "spots",
  "primary_key": "id",
  "filter_columns": ["category", "city", "crowd_level", "tags"],
  "sort_columns": ["rating", "daily_checkin_count"],
  "search_columns": ["name", "address", "tags", "dadian_highlight"],
  "syncing": "DTS 实时同步 PostgreSQL → OpenSearch"
}

// memories 索引
{
  "table": "memories",
  "primary_key": "id",
  "filter_columns": ["user_id", "style", "visibility"],
  "sort_columns": ["generated_at"],
  "search_columns": ["title", "summary"],
  "syncing": "DTS 实时同步"
}
```

---

## 4. 文件存储设计

### 4.1 OSS 目录结构

```
dadian/
├── avatars/{user_id}/{uuid}.webp          # 头像（300x300 webp）
├── photos/{user_id}/{date}/{uuid}.webp    # VLM 打卡原图
├── memories/{memory_id}/
│   ├── cover.webp                          # 电影海报封面
│   ├── photos/{seq}.webp                   # 回忆录中的照片（处理后）
│   └── share/{uuid}.webp                   # 一键分享图
└── synthetic/{outing_id}/                  # 异地同框合成图
    └── combined.webp
```

### 4.2 图片处理流程（使用 OSS 原生 Image Process）

不在应用层做图片转码。OSS 自带图片处理服务，通过 URL 参数即可完成：

```
用户上传 → 格式校验(服务端, JPEG/PNG/HEIC) → 病毒扫描(阿里云内容安全)
  → 上传 OSS source bucket
  → Bucket Event Notification 触发
  → OSS Image Process 自动处理:
      ?x-oss-process=image/resize,w_200    → 缩略图
      ?x-oss-process=image/resize,w_1920   → 全尺寸
      ?x-oss-process=image/format,webp     → WebP 转换
  → 落盘到 processed bucket → CDN 分发
```

应用层只做格式校验 + 病毒扫描。OSS Image Process 无 CPU/内存限制，比自己写全套转码链路稳定得多。

---

## 5. 数据迁移与生命周期

### 5.1 位置数据降级策略

```
Day 0-7:   精确 GPS（PostGIS GEOGRAPHY）
Day 8-14:  0.001° 精度 (~100m)    足够分析用户常去哪家咖啡馆
Day 15-30: 0.01° 精度 (~1km)      保留街区级信息
Day 31+:   仅保留城市级             彻底匿名化
```

通过定时任务 `location_degradation_job` 每日执行。

### 5.2 冷数据归档

出行结束后 90 天，出行详细数据（waypoints、missions、打卡记录）迁移到归档表或冷存储。回忆录不受影响，属于热数据。

---

## 6. 原设计问题修正

| 原设计 | 问题 | 修正 |
|--------|------|------|
| Spot 直接引用外部平台 | 耦合、不可靠 | 自建 spots 表 + 源数据 JSONB 字段 |
| Footprint 无评论字段 | 与 spec 的「便利贴」功能不符 | 增加 comment 字段 |
| 缺少 participant_missions | 无法处理「同一任务不同参与者」的情况 | 新建关联表 |
| DiceRecord 无 outing_id | 无法关联到具体出行 | 增加 outing_id |
| 无用户隐私设置字段 | GDPR/个保法不合规 | 增加 location_retention, achievement_visibility |
| 无 role_switches 的 trigger 字段 | 无法区分主动/系统建议转职 | 增加 switch_trigger 枚举 |
