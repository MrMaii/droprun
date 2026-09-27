# DropRun 技术栈

状态：第 0 节记录当前代码技术栈；其余分节明确保留历史提议与目标要求。  
更新时间：2026-09-07

## 0. 当前代码采用的技术

本轮静态读取确认：Android 原生 Java（Java 17 语言级别，compile/target SDK 35，min SDK 26）；Connector 为 Node.js ESM JavaScript；Relay 为 Cloudflare Worker JavaScript，绑定 D1、R2、Workers AI；媒体处理调用 yt-dlp、FFmpeg 和 Whisper；测试使用 Node test runner + Miniflare。

这是已有代码的描述，不是本轮新增的架构决定，也不是部署成功证明。源码入口、限制见 [IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md)。下方 Kotlin、TypeScript、Fastify、PostgreSQL 等为早期替代提案，不能要求后续 agent 无依据地重写现有原型。

## 1. 选择原则

- 首版只有一个手机平台、一个内容来源、一个 agent。
- 分享扩展、后台上传、本地进程和安全存储是核心，不以跨平台表面统一为优先。
- 先减少基础设施数量；任务量证明需要后再拆 Redis、微服务或事件总线。
- 协议必须版本化，Connector 与云端可独立升级。

## 2. 早期推荐基线（历史提议，未等同采纳）

| 层 | 推荐 | 原因 | 状态 |
|---|---|---|---|
| 手机端 | 原生客户端 | 系统分享、后台任务和凭证存储是核心能力 | 平台待定 |
| Android 路线 | Kotlin + Jetpack Compose + Share Intent | 可在当前 Windows 环境开发；系统分享支持直接 | 若 Android 首发 |
| iOS 路线 | Swift + SwiftUI + Share Extension | 最少绕过原生分享扩展限制 | 若 iOS 首发；需要 macOS/Xcode |
| Connector | TypeScript + Node.js，打包为后台应用 | 跨 Windows/macOS；适合管理子进程和 JSONL `stdio` | 提议 |
| Codex 集成 | `codex app-server` over `stdio` | 官方默认本地传输；避免暴露网络端口 | 已选架构基线 |
| Relay API | TypeScript + Node.js + Fastify | 与 Connector 共享 schema；小而直接 | 提议 |
| 数据库/队列 | PostgreSQL；先用任务表 + `FOR UPDATE SKIP LOCKED`/租约 | MVP 少一个基础设施；可实现可靠领取 | 提议 |
| 对象存储 | S3 兼容对象存储 + 短时签名 URL | 大文件不穿过 API 进程 | 提议 |
| Schema | JSON Schema + TypeScript 生成类型 | 跨手机、云端、Connector；运行时可校验 | 提议 |
| 通知 | APNs 或 FCM | 由首发平台决定 | 待定 |
| 可观测性 | 结构化事件 + 最小指标；内容默认脱敏 | 定位阶段失败，不记录材料正文或凭证 | 提议 |

## 3. 为什么暂不选 React Native / Flutter

它们不是被永久排除。首版最难的是系统分享扩展、后台上传、原生安全存储和平台真实 payload，不是复用页面。若先上跨平台框架，仍可能需要维护原生扩展与桥接，增加首个技术验证的变量。

平台 spike 完成后，若第二平台已是近期确定目标，再重新评估共享 UI 框架。

## 4. 后端部署原则

以下是早期候选筛选提案，不是当前 Cloudflare 实现的前置条件。长时处理可放本地 Connector；数据库不必限定 PostgreSQL。发布前仍需确认供应商、数据区域、成本和保留策略。原提案包括：

- 长任务与异步 worker，不只支持短请求函数。
- PostgreSQL。
- 大文件直传对象存储。
- APNs/FCM 出站访问。
- 数据区域、删除、备份和成本可说明。

MVP 可用一个 API 服务、一个 worker、一个 PostgreSQL 和一个对象存储。不要预先拆微服务。

## 5. Connector 技术要求

- Windows 优先可运行；若首发 iOS，不代表 Connector 必须先做 macOS。
- 能定位 `codex` 可执行文件并记录版本。
- 启动 `codex app-server`，使用默认 `stdio` JSONL。
- 首次连接时生成/缓存该 Codex 版本的 schema。
- 使用系统凭证库保存设备私钥和令牌。
- 支持开机启动、自动更新、断线重连、任务租约和干净卸载。
- 对任务素材、执行记录与旧隔离副本实施配额和保留策略；新任务按 [ADR 0009](../decisions/0009-original-project-plan-approval.md) 在原项目执行，不创建源码副本。

## 6. 明确不在 MVP 引入

- Kubernetes。
- 多区域主动-主动架构。
- Kafka 或独立事件总线。
- 自建向量数据库或复杂知识图谱。
- 同时接入 Codex 与 Claude。
- 云端托管完整开发环境。
- 全平台通用视频下载器。

## 7. 决策门槛

对已有原型补齐决策记录与验证：

1. 首发手机平台 ADR。
2. 首个内容来源 ADR。
3. App Server 项目映射 spike。

开始封闭测试前必须完成：

1. 云部署与数据区域 ADR。
2. 材料保留/删除政策。
3. 视频获取合法性与供应商条款核查。
4. 费用归属与额度展示方案。
