# 搭电 (Dā Diàn) — 设计文档索引

> 版本: v1.6 | 日期: 2026-06-21

---

## 文档列表

| 序号 | 文档 | 内容 | 读者 |
|------|------|------|------|
| 1 | [01-business-product-analysis.md](./01-business-product-analysis.md) | 产品定位、价值分析、商业模式、功能优先级、风险与建议 | 所有人（第一份读） |
| 2 | [02-technical-architecture.md](./02-technical-architecture.md) | 全栈技术选型、前端目录结构、Shell 状态机、后端分层、AI 架构、安全、运维 | 后端/前端/DevOps |
| 3 | [03-data-storage-design.md](./03-data-storage-design.md) | PostgreSQL Schema（15 张表）、Redis 设计、OpenSearch 索引、文件存储、数据生命周期 | 后端/DBA |
| 4 | [04-api-protocol-design.md](./04-api-protocol-design.md) | REST API（60+ 端点）、WebSocket 协议、SSE 事件流、高德 API 集成 | 前端/后端 |
| 5 | [05-ui-ux-design.md](./05-ui-ux-design.md) | 设计哲学、颜色/字体/间距/毛玻璃系统、组件设计、动效规范、响应式布局、字体自托管方案 | 前端/设计师 |
| 6 | [06-community-spec.md](./06-community-spec.md) | 社区板块产品设计（原 product 缺失） | 前端/产品 |
| 7 | [07-issues-and-recommendations.md](./07-issues-and-recommendations.md) | 原 product 问题诊断、Code Review 反馈吸收、MVP 裁剪建议、工程组织、指标 | 技术负责人/PM |

## 推荐阅读顺序

```
产品经理/设计师:  01 → 05 → 06
后端工程师:       01 → 02 → 03 → 04
前端工程师:       01 → 02 → 04 → 05 → 06
技术负责人:       01 → 02 → 03 → 04 → 07
新成员:           01 → 02 → 05（三天内读完）
```

## 技术栈速览

```
前端:  Vue 3 + Vite + Pinia + Vue Query + Radix Vue + Tailwind CSS + @vueuse/motion
地图:  高德 JS API 2.0
后端:  Spring Boot 3.x 单体（部署阿里云 ECS）
ORM:   MyBatis-Plus + PostGIS JDBC
数据库: PostgreSQL 16 + PostGIS 3.x + pgRouting
缓存:  Redis 7 (Streams)
搜索:  阿里云 OpenSearch
存储:  阿里云 OSS + CDN
消息:  Redis Streams
AI:    DeepSeek API（Chat + VL）
部署:  前端 Vercel / 后端 ECS 常驻实例
```

## 关键决策记录

| 决策 | 结论 | 原因 |
|------|------|------|
| Vue 而非 React | Vue 3 | uni-app 预留小程序通道，国内招人易 |
| Spring Boot 而非 Go | Spring Boot 3.x 单体 | 国内开发者供给足，500 DAU 单体够用 |
| PostgreSQL 而非 MySQL | PostgreSQL + PostGIS | 地理空间查询是核心，MySQL 做不了 |
| ECS 而非 FC | 阿里云 ECS 常驻 | FC 冷启动 10-20s+，WebSocket 不兼容 |
| Redis Streams 而非 Pub/Sub | Streams + Consumer Group | 消息持久化，不能丢 |
| 自托管字体而非 Google Fonts | OSS + CDN woff2 | Google Fonts 国内 500ms-3s 延迟 |
| 先发后审而非先审后发 | 先发后审 | 不阻塞用户即时满足，异步审核兜底 |
| OSS Image Process 而非自己转码 | OSS 原生 | 无 CPU/内存限制，比自己写稳 |
| 免费 + 打赏而非订阅 | 完全免费 | MVP 验证产品价值，不设付费墙 |
| KMS 托管 Secrets 而非代码仓库 | 阿里云 KMS | 敏感信息不落盘，密钥自动轮换 |
| Noto Sans SC 中文回退 | Space Grotesk → Noto Sans SC | 中西文 x-height 匹配，视觉一致性 |
| 渐进式承诺替代纯随机骰子 | 区域 → 地点 → 路线 | 拆解决策心理负担，骰子仅做兜底加速器 |
