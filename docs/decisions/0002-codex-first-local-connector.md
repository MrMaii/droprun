# ADR 0002：首版采用 Codex + 本地 Connector

状态：Accepted  
日期：2026-09-07

## 背景

产品需要访问用户真实项目、项目规则、本地文件、工具和测试环境。单独获得模型 API key 并不等于获得这些上下文。用户希望参考 Codex 官方远程能力，但没有证据表明第三方可以稳定复用官方 Remote 中继。

## 决定

- MVP 只接 Codex，不同时实现 Claude。
- 使用用户电脑上的 DropRun Connector 连接 Relay 与本地 Codex。
- Connector 主动连接云端；不把本地 agent 端口暴露到公网。
- Connector 优先使用 Codex App Server 默认的 `stdio` 传输。
- 一次分享使用 `thread/start` 新建会话，再用 `turn/start` 执行。
- 设备离线时排队；MVP 不承诺关机后云端继续完整执行。

## 理由

- 保留已有项目文件、规则、凭证和工具环境。
- 降低把完整仓库上传云端的隐私与成本。
- 官方 App Server 已提供线程、回合、审批和事件接口。
- `stdio` 是本地默认传输；官方文档将 WebSocket 标为实验性且不支持生产工作负载。

## 后果

- 必须安装和维护 Connector。
- 需要可靠的设备配对、断线重连、任务租约与本地自动更新。
- 项目发现与桌面项目映射必须实测。
- 用户电脑离线会增加交付等待时间。

## 验证

- 三个真实项目都能正确列出并映射。
- 新任务出现在正确项目和工作目录。
- 断线重连不丢任务、不重复执行。
- 云端没有 Codex 凭证或完整仓库。

## 官方依据

OpenAI Codex App Server：<https://developers.openai.com/codex/app-server/>（核查日期：2026-09-07）。

