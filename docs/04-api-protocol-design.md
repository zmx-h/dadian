# 搭电 (Dā Diàn) — 接口与协议设计

> 版本: v1.6 | 日期: 2026-06-21 | 风格: RESTful + WebSocket + SSE | 后端: Spring Boot 单体

---

## 1. API 设计规范

### 1.1 通用约定

| 项目 | 规范 |
|------|------|
| Base URL | `/api/v1` |
| 认证 | `Authorization: Bearer <JWT>` |
| 内容类型 | `application/json` (请求/响应) |
| 日期时间 | ISO 8601 UTC (`2026-06-21T14:30:00Z`) |
| 分页 | Cursor-based: `?cursor=<id>&limit=20`，响应含 `next_cursor` |
| 错误响应 | `{ "error": { "code": "OUTING_NOT_FOUND", "message": "出行记录不存在" } }` |
| 请求追踪 | `X-Request-ID` header，服务端透传 |

### 1.2 HTTP 状态码使用

| 状态码 | 语义 | 示例 |
|--------|------|------|
| 200 | 成功 | GET 查询 |
| 201 | 创建成功 | POST 创建出行 |
| 204 | 成功但无响应体 | DELETE |
| 400 | 请求参数错误 | 电量超出 10-100 |
| 401 | 未认证 | Token 过期 |
| 403 | 无权限 | 查看他人私密回忆 |
| 404 | 资源不存在 | Outing ID 无效 |
| 409 | 冲突 | 同一出行重复打卡 |
| 422 | 业务规则拒绝 | 电量差过大无法组队 |
| 429 | 限流 | 免费用户超出 AI 限额 |

---

## 2. REST API 端点

### 2.1 认证与用户

```
POST   /api/v1/auth/login              # 手机号登录（发验证码）
POST   /api/v1/auth/verify             # 验证码校验 → JWT
POST   /api/v1/auth/refresh            # 刷新 Token
DELETE /api/v1/auth/logout             # 登出（Token 加入黑名单）

GET    /api/v1/users/me                # 获取当前用户档案
PATCH  /api/v1/users/me                # 更新档案（昵称/头像/bio）
GET    /api/v1/users/:uid              # 查看他人公开档案
GET    /api/v1/users/:uid/stats        # 查看他人出行统计

PUT    /api/v1/users/me/onboarding     # 更新 Onboarding 三问结果
PUT    /api/v1/users/me/privacy        # 更新隐私设置
GET    /api/v1/users/me/achievements   # 我的成就列表
PATCH  /api/v1/users/me/achievements/:id/visibility  # 单个成就可见性

# 合规（《个人信息保护法》第 47 条）
POST   /api/v1/users/me/export         # 导出个人数据（异步打包 → OSS 下载链接）
DELETE /api/v1/users/me                # 注销账号（软删除，7 天冷静期内可恢复登录撤销）
  Query: ?confirm=true                 # 需前端二次确认
  # 注销后数据清理策略见下文 2.1.1
POST   /api/v1/users/me/deactivate     # 冻结账号（临时停用，数据保留）
```

### 2.1.1 账号注销数据清理策略

《个人信息保护法》要求"删除"意味着不可恢复，但 UGC 产品的特殊性在于：一条回忆录/留言不仅仅属于注销者，也属于阅读过它的人。

| 数据 | 7 天冷静期内 | 7 天后处理 |
|------|------------|-----------|
| 用户身份（手机号/昵称/头像） | 标记 `deleted_at`，登录即恢复 | **硬删除**。手机号释放可重新注册 |
| 用户发布的回忆录 | 仍对其他用户可见 | **匿名化**：作者显示为"已注销用户"，内容保留 |
| 用户对他人的留言 | 仍可见 | **硬删除**。他人的「充电」记录保留但关联到匿名占位 |
| 用户上传的照片（OSS） | 保留 | 回忆录关联的照片保留（内容保留），其余个人照片 30 天后批量清理 |
| 出行/足迹/打卡 | 对组队队友仍可见 | 关联到匿名占位（"已注销用户"），避免队友数据出现断链 |
| 关注/粉丝关系 | 保留 | **硬删除**。粉丝数自动减去 |
| 成就/骰子记录 | 保留 | **硬删除** |
| 私信 | 保留 | 接收方仍可查看，发送方显示为"已注销用户" |

### 2.2 出行

```
# P0 MVP 必做
POST   /api/v1/outings                 # 创建出行（草稿）
  Body: { mode, energy, role, dice_result_spot_id? }  
  # dice_result_spot_id: P1 保留字段，MVP 阶段传 null/忽略
  Response: 201 { outing }

GET    /api/v1/outings                 # 我的出行列表（分页）
  Query: ?status=active,completed&cursor=&limit=20
GET    /api/v1/outings/:id             # 出行详情（含路线、任务、参与者）
PATCH  /api/v1/outings/:id             # 更新出行状态/标题
DELETE /api/v1/outings/:id             # 取消出行（仅 draft 状态）

POST   /api/v1/outings/:id/start       # 正式开始出行（draft → active）
POST   /api/v1/outings/:id/pause       # 暂停出行
POST   /api/v1/outings/:id/resume      # 恢复出行
POST   /api/v1/outings/:id/complete    # 完成出行（触发回忆录生成）

# P1 组队迭代
POST   /api/v1/outings/:id/invite      # 邀请搭子
  Body: { user_id }
POST   /api/v1/outings/:id/join        # 搭子接受邀请
POST   /api/v1/outings/:id/leave       # 搭子退出
```

### 2.3 路线与任务

```
# P0
GET    /api/v1/outings/:id/route       # 获取出行路线（含 waypoints）
GET    /api/v1/outings/:id/missions    # 获取当前任务列表
  Query: ?status=available,active
POST   /api/v1/outings/:id/missions/:mid/accept   # 接受彩蛋任务
POST   /api/v1/outings/:id/missions/:mid/skip     # 跳过彩蛋任务
POST   /api/v1/outings/:id/missions/:mid/complete # 完成任务（含打卡照片）
```

### 2.4 打卡与足迹

```
# P0
POST   /api/v1/footprints              # 打卡
  Body: { outing_id, spot_id, lat, lng, photo_url?, comment? }
  Response: 201 { footprint, daily_count, daily_seq, friends_here }
  # friends_here 需跨 footprint+follow+WS 实时查询。MVP 阶段返回空数组 []

GET    /api/v1/spots/:id/footprints    # 查看某地足迹（便利贴）
  Query: ?limit=3
POST   /api/v1/footprints/:id/charge   # 给足迹留言「充电」/取消充电
```

### 2.5 命运骰子

```
# P1 迭代
POST   /api/v1/dice/roll               # 掷骰子
  Body: { scene: "departure", outing_id?, lat?, lng? }
  Response: 201 { spot, lock_seconds: 300 }

POST   /api/v1/dice/:id/accept         # 接受骰子结果
POST   /api/v1/dice/:id/escape         # 逃离骰子结果
  Response: { persuasion_text, escape_count, achievement_progress }

GET    /api/v1/users/me/dice-history   # 骰子历史
```

### 2.6 转职

```
# P1 迭代
POST   /api/v1/outings/:id/role-switch # 主动转职
  Body: { to_role: "npc" }
  Response: 201 { narrative, new_color, new_missions }

POST   /api/v1/outings/:id/role-switch/suggest # 系统建议转职（检测到长期无操作）
  Response: { suggestion: "要转成 NPC 休息一下吗？您的任务已转移给队友" }
```

### 2.7 VLM 视觉打卡

```
# P1 迭代
POST   /api/v1/vlm/analyze             # 上传照片获取 AI 评价
  Body: multipart/form-data { photo, spot_id, outing_id, lat, lng }
  Response: 201 { photo_id, ai_comment, style, spot_name }
  --- 实际实现 ---
  SSE: POST /api/v1/vlm/analyze-stream # 流式返回 AI 评价
  Body: 同上
  Event stream:
    event: start     data: { photo_id }
    event: token     data: { text: "这面墙在" }
    event: token     data: { text: "下午两点三十五分" }
    event: complete  data: { photo_id, ai_comment, style }
```

### 2.8 回忆录

```
# P0 核心
GET    /api/v1/memories                # 我的回忆录列表
  Query: ?filter=solo|team|agent|foodie|npc&cursor=&limit=12
GET    /api/v1/memories/:id            # 回忆录详情（含照片列表）
PATCH  /api/v1/memories/:id            # 编辑回忆录（标题/可见性/封面重选）
DELETE /api/v1/memories/:id            # 删除回忆录

POST   /api/v1/memories/:id/generate   # (重新)生成 AI 回忆录
  Response: 202 { job_id }             # 异步任务，轮询或 WebSocket 通知

POST   /api/v1/memories/:id/share-image  # 生成分享图
  Response: 202 { job_id }

GET    /api/v1/memories/:id/dual       # 获取双人视角对比数据（组队出行）
```

### 2.9 社区与社交

```
# P1 社区基础 Feed
GET    /api/v1/community/feed          # 社区 Feed（公开回忆录流）
  Query: ?style_match=true&cursor=&limit=20
GET    /api/v1/community/resonance     # 同频人推荐

# P1 留言（社区互动基础）
POST   /api/v1/comments               # 发表留言（≤100字）
  Body: { memory_id, content }
DELETE /api/v1/comments/:id           # 删除自己的留言

# P2 关注与私信
POST   /api/v1/follows/:uid           # 关注用户
DELETE /api/v1/follows/:uid           # 取消关注
GET    /api/v1/follows/followers      # 我的粉丝
GET    /api/v1/follows/following      # 我的关注

POST   /api/v1/messages               # 发送私信（需互关+相似出行）
  Body: { receiver_id, content }
GET    /api/v1/messages               # 私信列表（会话聚合）
GET    /api/v1/messages/:uid          # 与某用户的对话历史
```

### 2.10 POI 与发现

```
# P0 核心
GET    /api/v1/spots/discover         # 今日新发现
  Query: ?lat=&lng=&energy=&role=&radius=5000&limit=20
  Response: { spots: Spot[], generated_at }

GET    /api/v1/spots/:id              # POI 详情
GET    /api/v1/spots/search           # 搜索 POI
  Query: ?q=咖啡馆&city=上海&category=cafe&cursor=&limit=20

GET    /api/v1/spots/:id/energy-match # 该地点匹配各电量的推荐度
```

### 2.11 通知

```
# P0
GET    /api/v1/notifications          # 通知列表
  Query: ?cursor=&limit=20
POST   /api/v1/notifications/:id/read  # 标记已读
POST   /api/v1/notifications/read-all  # 全部已读
```

---

## 3. WebSocket 协议

### 3.1 连接

```
连接地址: wss://{host}/ws?token={jwt}
协议子协议: dadian-v1
心跳: 客户端每 30s 发送 ping，服务端回复 pong，60s 无心跳断开
重连: 指数退避，初始 1s，最大 30s
```

### 3.2 消息格式

```typescript
// 上行（客户端 → 服务端）
{
  "t": "event_type",       // 消息类型
  "d": { ... },            // 数据载荷
  "id": "req-001"          // 可选，用于请求-响应配对
}

// 下行（服务端 → 客户端）
{
  "t": "event_type",
  "d": { ... },
  "ts": "2026-06-21T14:30:00Z"
}
```

### 3.3 消息类型

```
客户端 → 服务端:
─────────────────
location:update     { lat, lng, accuracy, bearing }         # 位置上报（出行中每 5s）
mission:progress    { mission_id, distance_remaining }      # 任务接近进度
dice:watch          { dice_id }                             # 订阅骰子软锁状态
presence:update     { status: "active"|"idle"|"offline" }   # 状态更新

服务端 → 客户端:
─────────────────
outing:teammate_location  { user_id, name, lat, lng, updated_at }   # 队友位置
outing:mission_trigger    { mission_id, type, title, distance }     # 任务触发提醒
outing:blindbox_spawn     { event_id, marker_type, lat, lng }       # 盲盒出现
outing:footprint_pop      { spot_id, name, comment_count }          # 足迹共振浮现
dice:countdown            { seconds_left }                          # 骰子倒计时
dice:result               { spot, lock_seconds }                    # 骰子结果通知
presence:teammate         { user_id, status }                       # 搭子在线状态
memory:generated          { memory_id, cover_url }                  # 回忆录生成完成
notification:new          { type, summary }                         # 新通知
system:error              { code, message }                         # 服务端错误
```

### 3.4 位置共享优化

Web 端不依赖 GPS 持续追踪。桌面用户位置通过以下方式：
- 浏览器 `navigator.geolocation`（需用户授权，每 10s 更新）
- 或者用户手动在地图上标记位置
- 如果浏览器不支持定位，降级为手动模式

---

## 4. SSE（Server-Sent Events）

用于不需要双向通信的场景（比 WebSocket 轻量）：

```
GET /api/v1/sse/outing/:id/events      # 出行事件流（任务触发、盲盒）
GET /api/v1/sse/notifications          # 通知推送

事件格式:
event: mission_trigger
data: {"mission_id":"...","title":"...","type":"easter_egg"}

event: blindbox_spawn
data: {"event_id":"...","lat":31.2335,"lng":121.4765}
```

### 4.1 SSE 跨域处理

前端在 Vercel，后端在 ECS，不同域名，浏览器 `EventSource` API 会因跨域被阻断。

**Spring Boot 配置：**
```java
@Configuration
public class SseConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/v1/sse/**")
                    .allowedOrigins("https://dadian.app", "https://*.vercel.app")
                    .allowedMethods("GET")          // SSE 仅 GET
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
}
```

**VLM 流式评价 SSE（兼容跨域）：**
```
POST /api/v1/vlm/analyze-stream          # 非标准 SSE：使用 POST 发送图片
  Body: multipart/form-data { photo, spot_id, outing_id, lat, lng }
  --- 实现方案 ---
  Spring Boot 使用 SseEmitter，而非标准 EventSource。
  前端用 fetch() 而非 EventSource API：
    const response = await fetch('/api/v1/vlm/analyze-stream', {
      method: 'POST',
      body: formData,
    });
    const reader = response.body.getReader();
    // 逐行读取 SSE 事件流
  --- Event stream ---
  event: start     data: { photo_id }
  event: token     data: { text: "这面墙在" }
  event: token     data: { text: "下午两点三十五分" }
  event: complete  data: { photo_id, ai_comment, style }
```
注意：POST + SSE 是非标准用法，不能用浏览器原生 `EventSource`（仅支持 GET）。
前端使用 `fetch()` + `ReadableStream` 手动解析 SSE 事件流。

---

## 5. 第三方 API 集成

### 5.1 高德地图

```
# 路线规划
GET https://restapi.amap.com/v4/direction/walking?origin={lng},{lat}&destination={lng},{lat}&key={ak}

# POI 搜索
GET https://restapi.amap.com/v5/place/text?keywords={q}&region={city}&key={ak}

# 逆地理编码
GET https://restapi.amap.com/v3/geocode/regeo?location={lng},{lat}&key={ak}
```

### 5.2 内容安全（图片审核）

```
POST /api/v1/internal/content-check    # 内部封装
  → 阿里云/腾讯云内容安全 API
  Response: { safe: true/false, labels: [] }
```

---

## 6. API 版本化策略

- URL 路径版本：`/api/v1/` → `/api/v2/`
- 保持 2 个大版本共存
- 废弃字段用 `@deprecated` 标记，至少保留一个版本周期
- 破坏性变更只在新大版本引入

---

## 7. 错误码体系

所有 API 使用统一错误码。前端可基于 `code` 做统一 toast 处理：

```
通用错误码：
────────────────────────
RESOURCE_NOT_FOUND       404    资源不存在（outing / spot / memory / user 等）
VALIDATION_FAILED        400    请求参数校验失败
AUTH_EXPIRED             401    Token 过期，需刷新
AUTH_INVALID             401    Token 无效或已吊销
FORBIDDEN                403    无权限操作（访问他人私密回忆、私信未解锁用户）

业务错误码：
────────────────────────
BUSINESS_RULE_VIOLATION  422    业务规则拒绝（电量差过大无法组队、重复打卡、私信未解锁等）
BIO_LIMIT_EXCEEDED       422    留言超过 100 字限制
AI_RATE_LIMITED          429    AI 调用达到当日上限
SMS_RATE_LIMITED         429    短信发送过于频繁（60s 内重复请求）
ACCOUNT_DEACTIVATED      403    账号已注销/冻结
PHONE_ALREADY_REGISTERED 409    手机号已注册
UNLOCK_REQUIRED          403    需要先完成某个条件（如互关+相似出行才解锁私信）
```

**响应格式：**
```json
{
  "error": {
    "code": "BUSINESS_RULE_VIOLATION",
    "message": "他们的电量差过大，可能不适合同一物理空间",
    "detail": "lowest_energy: 20, highest_energy: 95"
  }
}
```

---

## 8. 原设计问题修正

| 原设计 | 问题 | 修正 |
|--------|------|------|
| 无 API 设计 | 只有前端 types/data | 完整 REST + WS + SSE 协议 |
| types.ts 中 callback 是前端思维 | 不适合 API 设计 | 改为标准 HTTP 请求/响应 |
| 无认证/鉴权 | 缺少安全考虑 | 增加 JWT + RBAC |
| 无流式 AI 接口 | VLM「1 秒内评价」需求无法满足 | SSE 流式返回 |
| 无分页规范 | 社区/POI 列表必崩 | Cursor-based 分页 |
| 无内容审核 | 合规风险 | 增加 content-check 接入 |
