# 搭电 (Dā Diàn) — 技术架构设计

> 版本: v1.6 | 日期: 2026-06-21 | 目标: Web App (Desktop + Tablet)

---

## 1. 架构总览

### 1.1 技术选型

| 层次 | 技术 | 选型理由 |
|------|------|---------|
| **前端框架** | Vue 3 + Vite + Vue Router | SPA 场景够用，国内生态成熟，uni-app 预留小程序通道 |
| **UI 库** | Tailwind CSS + Radix Vue | 毛玻璃/暗色主题友好、无头组件可定制 |
| **状态管理** | Pinia + TanStack Query (Vue Query) | Pinia 官方推荐替代 Vuex，Vue Query 管理服务端缓存 |
| **动效** | @vueuse/motion + CSS animations | VueUse Motion 是 Framer Motion 的 Vue 移植，API 一致 |
| **地图** | 高德 JS API 2.0 | 国内地图首选，无需翻墙，POI 数据与高德同源 |
| **后端框架** | Spring Boot 3.x 单体 | 国内开发者供给充足，500 DAU 单体绑线程池绰绰有余 |
| **ORM** | MyBatis-Plus + PostGIS JDBC | MyBatis-Plus 开发效率高，手写 SQL 处理复杂地理查询 |
| **API 文档** | SpringDoc (OpenAPI 3) | 从 Java 注解自动生成，前端直接导入生成 TS 类型 |
| **数据库** | PostgreSQL 16 + PostGIS 3.x + pgRouting | 地理空间查询是搭电核心能力，MySQL 替代不了 |
| **缓存** | Redis 7 (阿里云 Redis) | 会话、热度数据、骰子软锁、位置共享 |
| **搜索** | 阿里云 OpenSearch | POI 搜索、回忆录全文搜索；免运维，中文分词好 |
| **文件存储** | 阿里云 OSS + CDN | 用户照片、回忆录分享图、头像 |
| **内容安全** | 阿里云内容安全 API | 图片审核 + 文字敏感词过滤 |
| **消息** | Redis Streams + Consumer Group | 消息持久化 + 消费组 + ACK 确认；骰子锁/任务触发/回忆录通知不能丢 |
| **AI** | DeepSeek API | 国产模型，成本低，中文能力强；统一通过 AI Gateway 管理 |
| **Secrets 管理** | 阿里云 KMS (参数存储) + 环境变量 | 所有敏感信息（DB密码、API Key、AES密钥、JWT签名密钥）托管 KMS，不落盘代码仓库 |
| **部署** | 前端: Vercel / 后端: 阿里云 ECS (2c4g 常驻) | 前端纯静态 SPA；后端 Spring Boot 需要常驻进程支撑 WebSocket 和低延迟 API |
| **监控** | 阿里云 ARMS + SLS 日志 | 全托管，自动采集 JVM/API/DB 指标 |

### 1.2 部署架构

```
┌────────────────────────────────────────────────┐
│              CDN (阿里云 OSS + CDN)              │
│          静态资源 + 用户图片 + 分享图              │
│          字体文件 (自托管 woff2)                  │
└────────────────────┬───────────────────────────┘
                     │
┌────────────────────▼───────────────────────────┐
│                  Vercel                         │
│           Vue 3 SPA (纯静态托管)                 │
└────────────────────┬───────────────────────────┘
                     │ API 请求 + WebSocket
┌────────────────────▼───────────────────────────┐
│          阿里云 ECS (2c4g 常驻)                  │
│    ┌─────────────────────────────────┐         │
│    │   Spring Boot 3.x 单体应用       │         │
│    │   /api/v1/*                     │         │
│    │   WebSocket /ws                 │         │
│    │   Nginx 反向代理 + SSL 终止      │         │
│    └─────────────────────────────────┘         │
└──┬──────────────┬──────────────┬──────────────┘
   │              │              │
┌──▼──┐   ┌──────▼──────┐  ┌───▼────────┐
│ PG  │   │  Redis 7    │  │ 阿里云      │
│RDS  │   │  (阿里云)    │  │ OpenSearch │
└─────┘   └─────────────┘  └────────────┘
```

**为什么不用 FC（函数计算）部署 Spring Boot：**

| 问题 | 影响 |
|------|------|
| **冷启动 10-20s+** | JVM 初始化 + Spring 上下文加载 + MyBatis 映射。文档要求的 VLM/AI 延迟 <1s、路线规划秒级响应——冷启动直接让目标变成笑话 |
| **WebSocket 长连接不兼容** | FC 实例会被回收，连接断掉就再也连不回来；空闲连接也在计费，不经济 |
| **大图处理 CPU 密集** | FC 的 CPU 和内存上限不够做图片批量解码→压缩→编码 |

**正确的部署选型：ECS 2c4g 常驻实例（或 ACK 容器服务），Nginx 前置。** 500 DAU 下 ECS 成本远低于 FC 的「冷启动 + 长连接」隐性成本。

### 1.3 Nginx 配置（含 WebSocket 代理）

```nginx
server {
    listen 443 ssl http2;
    server_name api.dadian.app;

    ssl_certificate     /etc/ssl/dadian.pem;
    ssl_certificate_key /etc/ssl/dadian.key;

    # 通用 API 代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket 协议升级 —— 这 3 行缺失会导致 WS 握手 101 失败
    location /ws {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 86400s;      # WS 长连接，24h 超时
    }

    # 静态资源直出（如有）
    location /static/ {
        root /opt/dadian;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

### 1.4 为什么不拆微服务

| 维度 | 单体 | 微服务 |
|------|------|--------|
| 500 DAU 的 QPS | < 10，一个线程池搞定 | 服务间 RPC 延迟比业务逻辑还大 |
| 部署复杂度 | 1 个 Fat Jar + 1 个域名 | 9 个服务 × 配置 × CI/CD × 监控 |
| 调试成本 | IDEA 本地一键 debug | 必须上分布式链路追踪 |
| 团队规模 | 1-3 人 | 10+ 人才拆得动 |
| 何时拆 | — | 某模块 QPS 破千，团队 > 10 人 |

**搭电 MVP 期：Spring Boot 单体 + 模块化包结构。** 代码按领域分包，未来拆服务时把包抽出来就是一个独立服务。

### 1.5 包结构（模块化单体）

```
dadian-server/
├── src/main/java/com/dadian/
│   ├── DadianApplication.java          # Spring Boot 入口
│   ├── common/                         # 通用基础设施
│   │   ├── config/                     # Security / Redis / MyBatis / WebSocket 配置
│   │   ├── exception/                  # 全局异常处理
│   │   ├── dto/                        # 通用 DTO（分页、错误响应）
│   │   └── util/                       # JWT、坐标计算、图片处理工具
│   ├── module/
│   │   ├── user/                       # 用户模块
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   └── model/
│   │   ├── outing/                     # 出行模块（含路线、任务、转职）
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   └── model/
│   │   ├── poi/                        # POI 模块（含发现流、足迹）
│   │   │   └── ...
│   │   ├── dice/                       # 灵感骰子模块（产品层已从"命运骰子"改名）
│   │   │   └── ...
│   │   ├── memory/                     # 回忆录模块（含 VLM、分享图）
│   │   │   └── ...
│   │   ├── social/                     # 社交模块（关注、私信、评论）
│   │   │   └── ...
│   │   └── notification/              # 通知模块
│   │       └── ...
│   └── infrastructure/
│       ├── ai/                         # AI Gateway（管理 DeepSeek 调用）
│       ├── amap/                       # 高德 API 封装
│       ├── oss/                        # 阿里云 OSS 封装
│       ├── content_safety/             # 阿里云内容安全封装
│       └── search/                     # OpenSearch 封装
└── src/main/resources/
    ├── application.yml
    ├── application-dev.yml
    └── application-prod.yml
```

---

## 2. 前端架构

### 2.1 目录结构

```
dadian-web/
├── index.html
├── vite.config.ts
├── vercel.json                    # SPA fallback 配置
├── tailwind.config.ts
├── src/
│   ├── main.ts                    # Vue 入口
│   ├── App.vue                    # 根组件（Shell Provider）
│   ├── router/
│   │   └── index.ts               # Vue Router 配置
│   ├── views/                     # 页面级组件（对应路由）
│   │   ├── WelcomeView.vue        # 欢迎流程
│   │   ├── LaunchView.vue         # 出发页
│   │   ├── ExploreView.vue        # 探索页
│   │   ├── MemoriesView.vue       # 回忆页
│   │   └── CommunityView.vue      # 社区页
│   ├── components/
│   │   ├── shell/                 # 外壳组件
│   │   │   ├── AppShell.vue       # 全局外壳（导航状态驱动）
│   │   │   ├── DesktopSidebar.vue # 桌面端侧边导航
│   │   │   ├── TopNavbar.vue      # 标准模式顶部栏
│   │   │   ├── FloatingDock.vue   # 平板悬浮胶囊 Dock
│   │   │   └── GlassOverlay.vue   # 骰子锁定遮罩
│   │   ├── launch/                # 出发页组件
│   │   │   ├── Cockpit.vue         # P0
│   │   │   ├── EnergySlider.vue    # P0
│   │   │   ├── RoleCards.vue       # P0
│   │   │   ├── TeammateSlots.vue   # P1 组队（P1）
│   │   │   ├── DiceButton.vue      # P1 骰子（P1）
│   │   │   ├── DiscoveryStream.vue # P0
│   │   │   └── SideDrawer.vue      # P0
│   │   ├── explore/               # 探索页组件
│   │   │   ├── CyberMap.vue        # P0 赛博地图
│   │   │   ├── RouteBeam.vue       # P0 霓虹路线
│   │   │   ├── MissionMarkers.vue  # P0 任务标记
│   │   │   ├── MissionDrawer.vue   # P0 任务手账
│   │   │   ├── VlmCamera.vue       # P1 VLM 相机
│   │   │   ├── RoleSwitchFab.vue   # P1 转职悬浮球
│   │   │   ├── BlindBox.vue        # P2 盲盒事件
│   │   │   └── FootprintBubbles.vue # P2 足迹气泡
│   │   ├── memories/              # 回忆页组件
│   │   │   ├── FogWall.vue        # P0 时光迷雾墙
│   │   │   ├── MemoryCard.vue     # P0 回忆卡片
│   │   │   ├── FilmScroll.vue     # P0 3D 影卷
│   │   │   ├── PhotoViewer.vue    # P0 3D Tilt 照片
│   │   │   ├── DualPerspective.vue # P1 双人视角
│   │   │   ├── SummaryCard.vue    # P0 总结卡
│   │   │   └── ShareGenerator.vue # P0 分享图生成
│   │   ├── community/             # P1 社区页组件
│   │   │   └── ...
│   │   └── ui/                    # 通用 UI 组件
│   │       ├── GlassCard.vue
│   │       ├── GlassButton.vue
│   │       ├── AmberGlow.vue
│   │       ├── CyberInput.vue
│   │       └── ParticleBg.vue
│   ├── composables/               # Vue Composables（替代 React Hooks）
│   │   ├── useShell.ts            # 导航状态
│   │   ├── useOuting.ts           # 出行状态
│   │   ├── useUser.ts             # 用户档案
│   │   ├── useGeolocation.ts
│   │   ├── useDiceLock.ts
│   │   ├── useWebSocket.ts
│   │   └── useAmap.ts             # 高德地图封装
│   ├── stores/                    # Pinia Stores
│   │   ├── shell.ts
│   │   ├── outing.ts
│   │   └── user.ts
│   ├── api/                       # API 客户端（自动生成 + 手写）
│   │   ├── client.ts              # Axios 实例 + 拦截器
│   │   └── modules/               # 按模块分的 API 函数
│   │       ├── auth.ts
│   │       ├── outings.ts
│   │       ├── missions.ts
│   │       ├── memories.ts
│   │       └── ...
│   ├── i18n/                      # i18n 路由占位（MVP 不做翻译）
│   │   └── index.ts               # 空壳，只 export default locale='zh-CN'
│   └── styles/
│       └── globals.css            # Tailwind + CSS 变量 + 毛玻璃工具类
```

### 2.2 Shell 状态机

```
┌──────────────┐    Welcome     ┌──────────────┐
│  Welcome     │───Complete────→│  Immersive   │
│  (独立页面)   │                │  (/launch)    │
└──────────────┘                └──────┬───────┘
                                       │ 按 [🚀 出发]
                                  ┌────▼───────┐
                                  │  Standard  │
                                  │  (/explore, │
                                  │  /memories, │
                                  │  /community)│
                                  └────┬───────┘
                                       │ 骰子锁定
                                  ┌────▼───────┐
                                  │   Locked   │
                                  │  (遮罩覆盖) │
                                  └────────────┘
```

**Web 端 Standard 模式：**
- Desktop (≥1280px)：左侧 64px 垂直导航条 + 右侧内容区
- Laptop (1024-1279px)：顶部标签行
- Tablet (768-1023px)：底部悬浮胶囊 Dock

### 2.3 状态管理设计

```typescript
// stores/shell.ts (Pinia)
interface ShellState {
  navigationMode: 'immersive' | 'standard' | 'locked'
  desktopSidebarCollapsed: boolean
  diceLockSeconds: number | null
}

// stores/outing.ts (Pinia)
interface OutingState {
  currentOutingId: string | null
  status: 'idle' | 'active' | 'paused'
  route: Route | null
  missions: Mission[]
  participant: Participant
  geolocation: { lat: number; lng: number } | null
  socketConnected: boolean
}

// composables/useWebSocket.ts — 自动管理 WS 生命周期
// 出行激活时连接，出行结束/暂停时断开

// 服务端心跳超时与孤儿清理：
// - 客户端每 30s 发送 ping，服务端回复 pong
// - 服务端内置 120s 无消息超时：超过 120s 未收到任何帧（含 ping），主动关闭 session
// - 客户端断线重连时携带 session_resume_token，服务端复用已有 session 或创建新 session 后关闭旧 session
```

### 2.4 离线策略

Web 端"通常在线"，但地铁通勤、电梯、地下室会高频触发断网。对搭电而言最致命的不是地图不可用——而是断网瞬间「出发」按钮按下去没反应，用户直接流失。

**断网检测与 UI：**
```
navigator.onLine + fetch('/api/v1/health') 双重检测（浏览器 onLine 有假阳性）
→ 断网时：全站顶部滑入琥珀色 36px 提示条"网络已断开 · 操作将在恢复后自动同步"
→ 并不禁用操作按钮，用户仍可点击
→ 在线恢复：提示条变为绿色"网络已恢复 · 正在同步..." 3s 后消失
```

**操作队列（IndexedDB）：**
```
离线期间用户可执行的操作：
  - 按「出发」(创建 outing)
  - 完成打卡 (POST footprints)
  - 切换角色
→ 这些操作存入 IndexedDB 队列，每条带 id + timestamp + endpoint + body
→ navigator.onLine 恢复 + fetch ping 通过后，按序重放队列
→ 重放冲突由服务端用幂等键 (X-Idempotency-Key) 处理
```

**地图离线瓦片：**
```
高德 JS API 2.0 不支持离线地图瓦片缓存。
离线时地图层显示最后缓存的区域（浏览器 HTTP 缓存自然保留）+ 半透明灰色遮罩
+ 中央文字"地图数据不可用 · 路线仍会记录"
GPS 轨迹（如有）继续记录，在线后回填到路线
```

**离线范围（MVP）：**
MVP 阶段不做 Service Worker 级别的全站离线。上述 IndexedDB 队列 + 断网提示条足以覆盖核心场景。

### 2.4 高德地图集成

```typescript
// composables/useAmap.ts
import AMapLoader from '@amap/amap-jsapi-loader'

// 暗色赛博风格地图配置
const CYBER_MAP_STYLE = {
  // 高德自定义地图样式 JSON
  // 暗底 + 霓虹路线 + 琥珀色 POI 标记
}

export function useAmap(containerId: string) {
  const map = shallowRef<AMap.Map | null>(null)

  async function initMap() {
    const AMap = await AMapLoader.load({
      key: import.meta.env.VITE_AMAP_KEY,
      version: '2.0',
      // 安全鉴权：防止 API Key 被从浏览器 DevTools 扒走后盗刷
      // 安全密钥在前端以 securityJsCode 形式配置，配合高德「JSAPI 安全密钥」机制
      // 攻击者仅拿到 Key 没有 securityJsCode 无法调用服务端 API
      securityJsCode: import.meta.env.VITE_AMAP_SECRET,
      plugins: ['AMap.Walking', 'AMap.Geocoder', 'AMap.MarkerClusterer']
    })
    map.value = new AMap.Map(containerId, {
      zoom: 15,
      center: [121.4737, 31.2304],
      mapStyle: 'amap://styles/dark',  // 高德内置暗色 + 自定义覆盖
      // ...
    })
  }
  return { map, initMap }
}
```

**Key 暴露风险**：高德 JS API 2.0 的 Key 在浏览器端可见，任何人打开 DevTools → Network 面板就能从 URL 参数扒走。防护措施：
1. 高德控制台配置**域名白名单**（vercel.app / 自定义域名），限制 Key 仅在此域下可用
2. 启用 **securityJsCode**（安全密钥），配合高德的安全密钥机制，攻击者仅拿到 Key 无法调用服务端 API
3. 高德控制台设置**按 QPS 配额限流**，即使 Key 泄露也有限流兜底
4. 生产环境定期轮换 Key

---

## 3. 后端架构

### 3.1 Spring Boot 单体分层

```
┌─────────────────────────────────────────┐
│          Controller 层                   │
│  @RestController + @Validated           │
│  参数校验、认证注解、响应包装              │
├─────────────────────────────────────────┤
│          Service 层                      │
│  @Service + @Transactional              │
│  核心业务逻辑、跨模块编排                │
├─────────────────────────────────────────┤
│          Repository 层                   │
│  MyBatis-Plus BaseMapper + 自定义 SQL   │
│  PostGIS 地理查询（手写 SQL）            │
├─────────────────────────────────────────┤
│          Infrastructure 层               │
│  Redis / OSS / AMap / DeepSeek / 内容安全 │
└─────────────────────────────────────────┘
```

### 3.2 关键业务流程

**出行创建：**
```
Client            Controller         OutingService     PoiService     Redis
  │                    │                   │               │            │
  │ POST /api/v1/      │                   │               │            │
  │   outings          │                   │               │            │
  ├───────────────────→│                   │               │            │
  │  {energy,role,     │  createOuting()   │               │            │
  │   diceResult}      ├──────────────────→│               │            │
  │                    │                   ├─getRoute()───→│            │
  │                    │                   │←──Route───────┤            │
  │                    │                   ├─buildMissions()             │
  │                    │                   ├─PUBLISH───────┼──outing:new→│
  │                    │←──Outing──────────┤               │            │
  │←──201 {outing}─────│                   │               │            │
```

**队友位置同步（Redis Streams）：**
```
Teammate A      WebSocket Handler    Redis Streams     Teammate B
    │                   │                  │               │
    │ location:{lat,lng}│                  │               │
    ├──────────────────→│                  │               │
    │                   ├──XADD(outing:123│               │
    │                   │   loc:{...})───→│               │
    │                   │                 ├──XREADGROUP─→│
    │                   │←──ACK───────────┤               │
```

Redis Streams 比 Pub/Sub 的关键优势：消息持久化、消费组保证至少一次投递、支持 ACK 确认和死信队列。

---

## 4. AI 架构

### 4.1 AI Gateway 设计

```
Client → AI Gateway → DeepSeek-Chat   (回忆录文案生成)
                       DeepSeek-VL     (VLM 视觉打卡评价)
                       本地规则引擎     (AI 人格推断，运行在 Spring Boot 主线程——轻量加权滑动平均，不依赖 GPU)
```

**Gateway 职责：**
- 统一管理 DeepSeek API Key / 限流 / 重试
- Token 消耗追踪（按用户维度）
- 降级策略（DeepSeek 不可用时返回模板文案）
- 缓存（相同 POI + 天气 + 时段的评价模板复用）
- 审计日志（所有 AI 调用的 prompt 指纹和响应摘要）

### 4.2 VLM 打卡延迟优化

- 拍照后立即显示骨架动画（琥珀光圈呼吸）
- 评价文本通过 SSE 流式返回，首 token 500ms 内到达
- 超时 3s 则降级为本地模板评价

### 4.3 AI 人格成长引擎（MVP：规则引擎）

运行在 Spring Boot 主线程——纯查表+浮点运算，不依赖 GPU，不阻塞 API：

```
出行数据 → 特征提取 → 加权滑动平均更新（O(1) 计算量）
  ├─ 连续接受骰子 3 次 → +playful 0.2
  ├─ 电量 < 30% 出行 5 次 → +gentle 0.3
  └─ 完成 10 个锚定任务 → +active 0.1
```

结果存 `users.companion_tone` 字段，前端据此调整 UI 文案和 AI 回复风格。

### 4.4 AI 成本模型

| 场景 | 模型 | Token 预估/次 | 频率限制 |
|------|------|--------------|---------|
| VLM 即时评价 | DeepSeek-VL | ~500 input + ~100 output | 每次出行最多 10 次 |
| 回忆录文案 | DeepSeek-Chat | ~2000 input + ~800 output | 每次出行 1 次 |
| POI 标签抽取 | DeepSeek-Chat (批量) | 离线 | 天级定时任务 |

---

## 5. 安全架构

### 5.1 图片处理流程（使用 OSS 原生能力）

不在应用层做图片处理——OSS 自带图片处理服务（Image Process），通过 URL 参数即可完成：

```
用户上传 → 格式校验(服务端) → 上传 OSS source bucket
    → Event Notification 触发
    → OSS Image Process 自动生成多尺寸:
        ?x-oss-process=image/resize,w_200   → 缩略图
        ?x-oss-process=image/resize,w_1920  → 全尺寸
        ?x-oss-process=image/format,webp    → WebP 转换
    → 落盘到 processed bucket → CDN 分发
```

应用层只做格式校验 + 病毒扫描。OSS Image Process 无尺寸/CPU 限制，比自己写全套转码链路稳定得多。

### 5.2 内容审核策略（先发后审）

MVP 阶段采用**先发后审**策略，不阻塞用户即时体验：

```
用户发布 → 关键词自动过滤（本地毫秒级） → 即时展示给用户
    → 异步送审阿里云内容安全 API（图片 + 文字）
    → 审核通过：正常展示 + 可在社区被推荐
    → 审核拒绝：自动下架 + 用户收到温和通知（"您的部分内容正在重新整理"）
```

回忆录在审核期间显示「审核中」琥珀色占位视觉——让用户知道内容在路上，而非被吞了。

### 5.3 认证与授权

- **认证**：手机号 + 短信验证码（阿里云短信服务）
- **短信成本与防刷**：
  - 阿里云短信国内 ¥0.045/条。500 DAU 按日均 10% 登录率估算，月成本约 ¥68。极端情况（脚本刷短信轰炸）一个月可烧掉数千元
  - **防刷机制**：同一手机号 60s 内限发 1 条、同一 IP 1h 内限发 5 条、同一设备 24h 内限发 10 条（Redis 计数器）。超限降级为图形验证码（滑动拼图），人工操作无需短信
  - 上线首周加入短信发送量告警：单日发送量 > 200 条即触发排查
- **JWT**：AccessToken 15min + RefreshToken 7d
- **Spring Security**：`SecurityFilterChain` + 自定义 `AuthenticationProvider`
- **授权**：方法级 `@PreAuthorize` + 行级权限（用户只能操作自己的数据）
- **隐私**：位置数据加密存储，出行结束后 7 天降级精度

### 5.4 数据安全

| 数据 | 保护措施 |
|------|---------|
| 手机号 | AES-256 加密存储 + 日志脱敏；密钥托管阿里云 KMS，不与应用代码同仓 |
| 精确位置 | 加密存储，7 天后降级：8-14d→0.001°(~100m), 15-30d→0.01°(~1km), 31d+→城市级 |
| 用户照片 | OSS 私有 Bucket + 签名 URL 访问 |
| API 请求 | HTTPS + CORS 白名单 + CSRF Token |
| AI 对话 | 不存储原始 prompt，只存评价结果 |

### 5.5 PostgreSQL 备份与灾备策略

搭电的用户生成内容（回忆录、照片元数据、足迹）是核心资产，PG 故障可能导致数据永久丢失。

| 策略 | 配置 | 说明 |
|------|------|------|
| **阿里云 RDS 自动备份** | 保留 30 天 | 默认 7 天不够，升级为 30 天 |
| **PITR (Point-in-Time Recovery)** | 开启 | 支持秒级恢复到 7 天内任意时间点 |
| **跨可用区部署** | 主备架构（一主一备） | 主可用区故障自动切换至备可用区，切换时间 < 60s |
| **日志备份频率** | 每 30 分钟一次 | 配合 PITR，RPO < 30 分钟 |
| **手动快照** | 每次发版前手动打快照 | 保留 3 个版本，确保可回滚 |

**RTO/RPO 目标（MVP）：**
- RTO（恢复时间目标）：< 30 分钟（阿里云 RDS 自动切换）
- RPO（恢复点目标）：< 30 分钟（日志备份间隔）
- 每月进行一次恢复演练，确认备份可用

---

### 5.6 高德 API 商业授权

高德 JS API 2.0 对企业开发者有明确的**商业授权要求和用量配额限制**。

| 阶段 | 授权类型 | 预估成本 |
|------|---------|---------|
| MVP 开发/测试 | 个人开发者免费额度 | ¥0 |
| 上线运营（有注册用户） | 企业商业授权 | 数万元/年（需向高德商务确认） |
| 日 PV > 配额 | 按量或更高套餐 | 另行协商 |

**风险缓解**：在 `composables/useAmap.ts` 中将高德 API Key 的环境变量配置化。如果需要切换地图源（成本原因），只需更换 Map 适配层，不侵入业务组件。

### 5.7 Secrets 管理策略

所有敏感信息不落盘代码仓库，统一托管阿里云 KMS（密钥管理服务）或 Parameter Store。

| 敏感项 | 注入方式 | 来源 | 开发环境 |
|--------|---------|------|---------|
| 高德 API Key | 环境变量 `VITE_AMAP_KEY` | KMS → Vercel Env | 开发 Key，域名白名单 `localhost` |
| 高德 Security Code | 环境变量 `VITE_AMAP_SECRET` | KMS → Vercel Env | 开发 Secret |
| DeepSeek API Key | 环境变量 `DEEPSEEK_API_KEY` | KMS → ECS 环境变量 | 开发 Key，限额 ¥50/月 |
| 阿里云短信 AccessKey | 环境变量 `ALI_SMS_AK` / `ALI_SMS_SK` | KMS → ECS 环境变量 | 沙箱模板 |
| Redis 密码 | 环境变量 `REDIS_PASSWORD` | KMS → ECS 环境变量 | `dadian_dev`（本地 docker-compose） |
| PostgreSQL 密码 | 环境变量 `DB_PASSWORD` | KMS → ECS 环境变量 | `dadian_dev`（本地 docker-compose） |
| AES-256 加密密钥 | KMS 密钥 ID（不导出明文） | 阿里云 KMS | 本地随机 256-bit 测试密钥 |
| JWT Signing Key | KMS 密钥 ID（不导出明文） | 阿里云 KMS | 本地随机 256-bit 测试密钥 |

**密钥轮换策略**：
- AES 密钥 & JWT Key：KMS 自动轮换（90 天周期），旧版本保留 30 天用于解密/验证旧数据
- API Key（高德/DeepSeek）：手动轮换，提前 7 天创建新 Key，双 Key 并行期过后下线旧 Key
- 生产环境 Secrets 与应用代码严格分离，开发环境使用独立密钥，不与生产共享

---

## 6. 可观测性

### 6.1 阿里云原生方案

| 支柱 | 工具 | 关键指标 |
|------|------|---------|
| **Logging** | SLS (日志服务) | 结构化 JSON，统一 requestId 串联 |
| **Metrics** | ARMS (应用实时监控) | API p50/p99、JVM GC、DB 连接池 |
| **Tracing** | ARMS 链路追踪 | 出行创建全链路、AI 调用追踪 |

### 6.2 核心告警

- AI Gateway 调用失败率 > 5%
- 高德 API 超时率 > 10%
- API 平均延迟 > 500ms
- 出行创建失败率 > 2%
  - 短信单日发送量 > 200 条
