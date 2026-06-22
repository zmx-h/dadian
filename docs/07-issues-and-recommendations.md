# 搭电 (Dā Diàn) — 综合问题报告与深度建议

> 版本: v1.6 | 日期: 2026-06-21 | 已对齐全栈+合规+成本+离线+注销清理 | 已吸收 Code Review 反馈（第 6 轮）

---

## 一、原 Product 文件夹问题诊断

### 1.1 架构层面

| 问题 | 严重程度 | 说明 |
|------|---------|------|
| **没有后端设计** | 致命 | 全部内容是前端 spec+types+data，没有任何后端架构、API 设计、数据库设计 |
| **没有社区板块** | 严重 | Roadmap 提到「社区 — 同频客厅」，但文件夹中完全没有对应的 spec/types/data |
| **移动端思维贯穿** | 严重 | Shell spec 的「下滑隐藏/上滑呼出」「长按」「陀螺仪」「touch 事件」都是移动独有 |
| **POI 数据直接依赖第三方** | 严重 | 数据模型直接引用大众点评/高德/小红书字段，没有自建中间层，系统耦合度高 |
| **外部 API 无备选方案** | 高 | 高德地图没有替代方案；VLM 只提到概念但没有技术实现方案 |
| **安全设计缺失** | 高 | 完全没有认证、授权、数据加密、内容审核相关设计 |
| **离线/弱网无设计** | 中 | Web 端虽然通常在线，但地图离线缓存在地下/信号差场景会出问题 |
| **AI 成本模型缺失** | 严重 | VLM 评价、回忆录生成的 token 消耗和成本没有任何设计 |

### 1.2 数据模型层面

| 问题 | 严重程度 | 说明 |
|------|---------|------|
| **实体关系不完整** | 高 | `data-shape.md` 只有概念关系描述，没有真实 schema |
| **缺少 participant_missions** | 高 | Mission 面向特定 Participant，但没有关联表，多人出行时任务无法分配 |
| **Footprint 缺少评论字段** | 中 | Spec 提到「留下评价以毛玻璃便利贴形态」，但数据模型中的 Footprint 没有 comment |  
| **DiceRecord 缺少 outing_id** | 中 | 无法关联到具体出行，只能关联用户 |
| **成就系统缺少条件字段** | 低 | 成就的解锁条件没有字段存储，只有描述文字 |
| **Spot 的 daily_checkin_count 缺少重置机制** | 中 | 需要用定时任务每日重置 |
| **Follow 是单向但私信要互关** | 低 | Follow 表设计合理，但 Messages 的解锁逻辑需要查询 Follow |
| **没有用户隐私设置字段** | 高 | GDPR 合规要求，需要位置保留期、成就可见性等设置 |

### 1.3 Spec 设计层面

| 问题 | 严重程度 | 说明 |
|------|---------|------|
| **出发页 spec 的 Callback hell** | 高 | `ChuFaProps` 有 12 个独立 callback，状态管理散乱。建议合并为事件总线或 reducer |
| **探索页 spec 功能过多** | 高 | 一个页面承载 10+ 独立交互流程，对 MVP 来说过重 |
| **回忆页的 3D Tilt 依赖 DeviceOrientation** | 中 | Web 端没有陀螺仪，需要降级方案（已设计为鼠标 Tilt） |
| **Shell 的「步行 5s 收缩为圆点」** | 中 | 桌面端无步行概念，此行为需要重新设计 |
| **骰子软锁的挽留文案使用 hardcoded** | 低 | 应该支持产品端配置 |
| **足迹共振的实时人数** | 中 | 需要 Redis 实时计数 + 定时同步，spec 中没有提 |

### 1.4 产品逻辑层面

| 问题 | 严重程度 | 说明 |
|------|---------|------|
| **命运骰子可能被视为赌博机制** | 严重 | 「盲盒」「骰子」「命运」等词汇在中国语境下容易触发政策风险 |
| **社交电量系统的复杂度** | 高 | 一个从未见过的概念需要极低的学习成本；Onboarding 三问是否足够建立理解？ |
| **角色系统过于游戏化** | 中 | 「特工」「美食家」「NPC」三个角色差异化是否足够？用户为什么选 NPC？ |
| **远程联机是 P2 功能** | 高 | 产品概述将其作为核心卖点，但技术复杂度远超同城出行，建议移出 MVP |
| **成就系统的正向激励不足** | 低 | 「逃跑大师」等反向荣誉是否真的产生分享/炫耀行为？ |

---

## 二、深度建议

### 2.1 品牌与合规

**命运骰子的重命名建议：**
- 「命运骰子」→ 「灵感骰子」或「惊喜罗盘」
- 「盲盒」→ 「惊喜事件」或「彩蛋」
- 「逃离命运」→ 「这次我听自己的」或「改主意了」
- 降低赌博暗示，强化「随机惊喜」的正面联想

**内容审核前置：**
- MVP 阶段就接入内容安全 API（图片 + 文字）
- UGC 内容先发后审（回忆录、留言）——即时展示给用户，异步审核兜底，不阻塞体验
- 建立举报→临时隐藏→人工审核闭环

### 2.2 MVP 范围裁剪建议

**P0（必须）：**
1. 单人出行闭环：出发→探索→回忆
2. 用户注册 + Onboarding
3. 社交电量 + 职业选择
4. 锚定任务（基于 POI 数据）
5. 基础 AI 回忆总结（单一风格：王家卫）
6. 基础 POI 发现流
7. Web 端 Standard 模式导航

**P1（迭代）：**
1. 多人组队（同城）
2. 命运骰子
3. 彩蛋任务
4. VLM 视觉打卡
5. 一键转职
6. 基础社区 Feed
7. AIGC 回忆录多风格

**P2（远期）：**
1. 远程联机
2. 足迹共振
3. 盲盒突发事件
4. AI 人格成长引擎
5. 城市玩家年鉴
6. 私信功能
7. 社区完整功能

### 2.3 技术决策建议

**MVP 技术栈已确认：**
- Vue 3 + Vite + Vue Router + Pinia（前端 SPA，部署 Vercel）
- Tailwind CSS + Radix Vue（UI 层）
- @vueuse/motion（动效，替代 Framer Motion）
- 高德 JS API 2.0（地图）
- Spring Boot 3.x 单体（后端，部署阿里云 ECS 常驻实例）
- MyBatis-Plus + PostgreSQL 16 + PostGIS（数据层）
- Redis 7（阿里云 Redis）
- 阿里云 OpenSearch（搜索）
- 阿里云 OSS + CDN（文件存储）
- 阿里云内容安全 API（图片/文字审核）
- DeepSeek API（AI，通过自建 AI Gateway 管理）
- 不做 i18n，仅保留 i18n 路由占位
- 完全免费 + 打赏按钮
- MVP 用 H5 + 图片分享替代小程序

### 2.4 工程组织建议

**仓库结构：**
```
dadian/
  └── docker-compose.yml          # 本地开发基础设施（PostgreSQL + Redis）
├── dadian-web/              # Vue 3 前端
│   ├── src/
│   │   ├── views/           # 页面组件
│   │   ├── components/      # 通用 + 业务组件
│   │   ├── composables/     # Vue Composables
│   │   ├── stores/          # Pinia Stores
│   │   ├── api/             # API 客户端
│   │   ├── i18n/            # i18n 占位
│   │   └── styles/          # Tailwind + CSS 变量
│   ├── vite.config.ts
│   └── vercel.json
├── dadian-server/           # Spring Boot 单体
│   ├── src/main/java/com/dadian/
│   │   ├── common/          # 通用基础设施
│   │   ├── module/          # 业务模块（user/outing/poi/dice/memory/social/notification）
│   │   └── infrastructure/  # AI/高德/OSS/内容安全/搜索
│   └── pom.xml
├── docs/                    # 设计文档（本文档）
└── product/                 # 原始产品素材（已冻结）
```

**前端目录结构已在 `02-technical-architecture.md` 中定义。**

**MVP 开发阶段建议：**

| 阶段 | 周期 | 内容 | 验收节点 |
|------|------|------|---------|
| Phase 0：骨架 | 1 周 | 项目初始化、CI/CD、Shell 导航框架、设计系统 tokens、docker-compose 本地开发环境 | 设计师像素级验收 Shell 导航 + 色板 |
| Phase 1：出发 | 2 周 | 驾驶舱、电量滑块、职业选择、发现流、Onboarding | 设计师验收驾驶舱 UI 还原度 + 交互 |
| Phase 2：探索 | 3 周 | 地图集成、路线规划、锚定任务、基础打卡 | 设计师验收地图 UI + 任务卡片 |
| Phase 3：回忆 | 2 周 | 迷雾墙、影卷详情、AI 文本生成、分享图 | 设计师验收回忆全流程 + 可用性测试 |
| Phase 4：运营后台 | 1 周 | 极简内容审核后台 + 基础数据看板（见下文） | 运营验收审核流程 |
| Phase 5：联调 | 1 周 | E2E 测试、性能优化、错误处理、灰度上线 | 全员回归测试 |

**极简运营后台（Phase 4 必须）：**
- 内容审核列表：待审/已过/已拒的回忆录 + 留言，支持一键通过/拒绝
- 骰子文案配置：软锁挽留文案、彩蛋文案可配置化（否则每次改文案要发版）
- 基础数据看板：DAU、出行创建数、回忆录生成数、AI Token 消耗

### 2.5 本地开发体验

**docker-compose.yml（根级）：**
```yaml
services:
  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: dadian
      POSTGRES_USER: dadian
      POSTGRES_PASSWORD: dadian_dev
    ports: ["5432:5432"]
    volumes: ["./dev-data/pg:/var/lib/postgresql/data"]
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
```

**根级启动脚本（package.json scripts）：**
```json
{
  "scripts": {
    "dev": "concurrently \"cd dadian-web && pnpm dev\" \"cd dadian-server && ./mvnw spring-boot:run\"",
    "db:up": "docker compose up -d postgres redis",
    "db:down": "docker compose down"
  }
}
```

新成员：`git clone` → `docker compose up -d` → `pnpm dev`，三个命令即可开始开发。

### 2.6 UI 设计的额外建议

**桌面端专属交互：**
- 键盘快捷键：`Space` 暂停/恢复出行，`← →` 翻回忆页，`Esc` 关闭遮罩
- 右键菜单：地图上右键 → [此处有什么？] [标记为目的地]
- 拖拽：出发页驾驶舱底部可拖拽调节发现流的高度

**易用性改进：**
- 驾驶舱 Social Energy 滑块增加数字输入（桌面端方便精确调节）
- 影卷照片增加全屏模式（点击照片放大到全屏）
- 地图支持滚轮缩放 + 框选 POI 区域

### 2.7 指标与成功标准

| 指标 | MVP 目标 | 测量方式 |
|------|---------|---------|
| 新用户首日出行完成率 | > 40% | 完成一次完整出行（出发→探索→回忆） |
| 首次出行时间 | < 5 分钟 | 从打开 App 到按下出发 |
| 回忆录生成满意率 | > 60% | 生成后用户是否选择保存/分享 |
| 7 日留存 | > 25% | 第 7 天仍有活跃行为 |
| AI 单次出行成本 | < ¥0.5 | 总 AI 调用成本 / 出行次数 |

---

## 三、已确认的技术决策

| 决策项 | 结论 |
|--------|------|
| 地图服务商 | 高德 JS API 2.0 |
| AI 模型 | DeepSeek API（Chat + VL）|
| 内容审核 | 阿里云内容安全 API |
| 部署 | 前端 Vercel + 后端阿里云 ECS (2c4g 常驻) |
| 用户规模 | 按 50-500 DAU 设计，架构先容 1000 |
| i18n | 不做，仅保留路由占位 |
| 支付 | 完全免费 + 打赏按钮 |
| 小程序 | MVP 用 H5 + 图片分享替代，未来计划 uni-app |
