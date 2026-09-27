# DropRun 技术架构

## Public self-hosted profile (0.5.0)

Each owner runs one Windows Connector and one Cloudflare Relay (Workers, D1, R2).
The phone is bound to a verified HTTPS origin and instance ID. Codex credentials
and project code stay local; Cloudflare administration belongs only to setup.
GPTSites hosts static marketing and installation information, not task APIs.
There is no official user/account or multitenant service.

The instance's preview Worker uses the same private storage but a separate origin.
Versioned snapshots use capability access, sandbox CSP and expiry/revocation;
raw uploaded source does not become public by being stored. Cloud snapshot links
can reopen without the computer. Optional live preview uses the owner's managed
tunnel and remains tied to the current local project.

Windows packages include Node and a browser setup interface, with DPAPI-protected
runtime credentials. Android public distribution is separate from old debug
installs and uses Keystore-protected tokens. Protocol facts live in
[CONTRACTS](CONTRACTS.md); current verification is in [the release record](../releases/0.5.0.md).


状态：目标架构，含已采纳的 0.3.0 增量  
更新时间：2026-09-12

本文描述目标架构；原目录执行和计划审批门禁已由 [ADR 0009](../decisions/0009-original-project-plan-approval.md) 采纳，新配对默认直接执行按 [ADR 0011](../decisions/0011-relay-station-ux.md) 更新，既有手机设置保留。实施和发布证据统一见 [IMPLEMENTATION_STATUS.md](./IMPLEMENTATION_STATUS.md)。目标中的系统凭证库、签名上传等不能据此视为已实现。

## 当前增量：反复交办视觉任务

[ADR 0013](../decisions/0013-repeatable-visual-delivery.md) 保留组件分工，为“分享网页效果后继续刷视频”增加执行前、交付前和交付后的核对。精确状态、阈值和 API 只在 [CONTRACTS](./CONTRACTS.md#当前增量可重复视觉交付-030) 维护；本版本发布与集成证据见 0.3.0（历史私人发布记录，未随公开版分发）。

| 层 | 本轮责任 |
|---|---|
| Android | 分享保存后从可见操作启动限时结果接收；增量缓存保留旧报告，通知完成/需处理；显示预览类型、版本、有效性与重开入口。手机系统停止接收不等于电脑任务取消。 |
| Relay | 固定任务执行模式和授权代次；原子领取仍串行；保存终态、截图元数据、独立预览状态及重开请求；返回增量条目与完整有序 ID。 |
| Connector 材料处理 | 下载/解码、带时间及哈希的总览与密集变化片段；核对实际附给模型的视觉输入，不把封面或字幕当动态画面。 |
| Codex 原会话 | 先只读定位页面和验收目标，再在项目授权内执行；交付前另开只读图片复核，可针对核对差异补修一次。 |
| Connector 浏览器与交付 | 只在当前获准执行阶段访问本机预览、执行受限真实动作与断言；持久截图、检查、哈希和上传进度；程序核对证据后上传，成功才结束任务。 |
| Connector 预览 | 静态构建保存任务版本快照，live 明示当前项目；管理隧道、进程、有效期、容量和快照保留，恢复已有描述时不启动编码回合。 |

执行骨架为：材料处理、只读定位、材料充分性与目标文件漂移检查、执行、实际浏览器检查、只读图片复核、持久交付。`review` 仍先等待用户批准计划；`direct` 跳过手机计划审批，但不跳过视觉定位、交付核对或高风险授权。视觉阶段保存在本机任务状态，不能仅用 Relay 的 running 展示推断已走完全部步骤。

截图和静态结果快照分别保存；截图属于任务交付证据，快照属于限期预览恢复资源。新任务不复制原项目用于编码，生成静态预览快照也不改变原目录执行模式。只核对已定位文件开始执行前的漂移，不宣称全面锁住桌面或可靠归属其他客户端的全部改动。

Android 使用自身前台服务拉取而非无限运行的 Job 或已接入的 FCM；后台启动/超时与通知权限边界见契约和 [Android 官方规则](https://developer.android.com/develop/background-work/services/fgs/timeout)（核查 2026-09-12）。预览沿用 Quick Tunnels 的私人临时链接，不升级为有 SLA 的生产托管，官方依据见 ADR 0013。

## 1. 架构目标

在不把用户完整代码仓库或 Codex 凭证上传到 DropRun 云端的前提下，将手机分享的外部材料可靠投递到正确的本地项目，让 Codex 在明确权限内工作，并把有证据的结果回传。

## 2. 系统边界

```text
来源 APP
   │ 系统分享
   ▼
DropRun 手机 APP / Share Extension
   │ HTTPS：任务、素材、留言、项目选择
   ▼
DropRun Relay API
   ├── Task Store（任务、状态、审批、报告）
   ├── Object Store（受限时效的素材与产物）
   └── Worker（认证、中转与转写调用）
   ▲
   │ 出站长轮询或安全流连接；不开放用户电脑入站端口
   ▼
DropRun Desktop Connector
   ├── 设备身份与项目映射
   ├── 素材获取、抽帧、转写协调与本地暂存
   ├── 原项目目录与计划/执行权限门禁
   └── Codex App Server（stdio）
          └── 目标项目文件、AGENTS.md、工具与测试
```

## 3. 组件职责

### 手机 APP 与分享扩展

- 接收系统分享 payload，不推测来源应用没有提供的内容。
- 显示材料类型、选择项目、采集可选留言。
- 创建任务后立即返回，不在扩展中等待视频处理。
- 展示设备在线状态、任务状态、审批和最终报告。
- 新配对默认直接执行；可选先看计划并批准具体版本，已有手机设置保留。从先看计划切回直接执行需联网确认。

### Relay API

- 认证用户和设备。
- 签发短时上传/下载凭证。
- 保存任务状态、幂等键、项目的云端别名和报告。
- 将任务派发给已配对 Connector。
- 保存审批与结果，供手机接收并产生通知；当前未接入服务端移动推送。
- 固定新任务的执行模式，保存计划版本及决定；模式由服务端读取设备设置，不接受客户端伪造或追溯改变。
- 不保存 Codex 登录凭证；不默认接收整个代码仓库。

### 内容处理（Connector，按需调用 Relay 转写）

- 判断实际收到的材料类型。
- 针对首个支持来源执行合法的内容获取。
- 校验媒体可读性、格式、时长、音轨和画面。
- 生成素材包：来源、原始材料、转写、关键帧、摘要、覆盖范围与错误。
- 不决定项目应该改什么；项目判断保留给目标项目中的 Codex。

### Desktop Connector

- 主动连接 Relay，避免向公网开放本机端口。
- 保存设备密钥和 Codex 凭证于本机安全存储。
- 发现/维护 Codex 项目与工作目录映射。
- 下载素材包到任务专用目录。
- 在原项目创建独立会话；计划阶段只读，批准准确计划或 `direct` 才允许原目录写入，不先复制完整项目。
- 启动并管理 Codex App Server，提交任务并转发事件。
- 核对显式产物哈希、真实命令/文件事件和最终报告；不对新任务全目录快照或全量变更扫描，不自动把用户既有修改打包为 `changes.patch`。

### Codex App Server

依据 2026-09-07 核查的官方 OpenAI 文档：

- App Server 是用于将 Codex 深度集成到产品中的公开接口，涵盖认证、对话历史、审批与流式 agent 事件。
- 默认传输是 `stdio` 上的 JSONL；WebSocket 仍被标为实验性且不支持生产工作负载。
- 客户端先 `initialize` / `initialized`，再用 `thread/start` 创建会话、`turn/start` 提交工作并监听事件。
- CLI 可生成与本机 Codex 版本匹配的 TypeScript 或 JSON Schema。

官方来源：<https://developers.openai.com/codex/app-server/>（核查日期：2026-09-07）。

早期核查未在该公开页面确认 `project/list`；这不等于本机协议没有此接口。已有 `connector/codex.mjs` 调用了它。应以安装版本的 schema 与真实桌面对照验证兼容性；仅有调用代码不证明映射验收通过。不得把 OpenAI Platform 的 organization project API 与 Codex 桌面项目混为一谈。

## 4. 核心数据流

### 4.1 配对与项目同步

1. 手机创建一次性配对请求。
2. Connector 使用用户可核验的短码完成绑定。
3. Connector 读取本地可用项目及工作目录。
4. 用户在电脑或手机确认允许远程交办的项目。
5. Relay 仅保存项目别名、稳定映射 ID、设备 ID 和最小展示信息。

项目映射至少包含：

- `device_id`
- `local_project_id`（若本机接口提供）
- `display_name`
- `working_directory`（优先只留本机；云端保存不透明映射 ID）
- `permission_profile`
- `last_verified_at`

### 4.2 分享与素材处理

1. 手机生成 `task_id` 和 `idempotency_key`。
2. APP 上传收到的原始 payload 或来源 URL。
3. Connector 检查并生成素材包，按需通过 Relay 调用转写。
4. 素材包记录每种模态的覆盖状态。
5. 关键模态缺失时进入 `blocked` 或在明确降级条件下继续。

### 4.3 执行

1. 在线 Connector 原子领取任务。
2. 验证任务未被执行且项目映射仍有效。
3. 解析原项目目录并准备素材路径；新任务和追问不创建源码副本。
4. 启动 App Server 并完成初始化握手。
5. `review` 使用原 cwd 的只读 sandbox 创建/恢复会话并开始计划回合，不允许提升审批；后台 MCP、插件、Apps 禁用。
6. 计划回传后释放执行器，等待手机批准具体版本。批准后或 `direct` 任务按当前视觉门槛先只读定位，再在同 thread 的执行回合采用原 cwd `workspaceWrite`。
7. 转发进度、工具调用和单次命令审批事件；计划批准不能覆盖超范围及外部高风险操作。
8. 终态前核对 v3 显式文件与真实命令证据，视觉任务还须浏览器验证、图片复核及交付上传，准确说明原目录已发生的改动。旧 `legacy-isolated` 运行恢复继续旧副本语义。

### 4.4 回传

1. Connector 上传报告和允许公开的产物元数据。
2. Connector 核对报告及交付证据，Relay 保存报告并保护终态不被旧事件回退。
3. APP 接收结果、产生通知并展示说明、截图和预览入口；前台服务或系统周期同步均受各自边界限制。
4. 后续追问使用已保存的 `thread_id` 继续同一会话。

## 5. 信任边界

| 边界 | 允许通过 | 禁止默认通过 |
|---|---|---|
| 来源 APP → DropRun | 用户主动分享的 payload | 来源 APP 登录态、未分享内容 |
| 云端 → Connector | 已签名任务、素材、用户留言 | 新权限、系统指令、任意命令 |
| 外部材料 → Codex | 引用、字幕、画面、用户说明 | 覆盖项目规则、索取密钥、扩大权限 |
| Connector → 云端 | 状态、报告、授权产物 | Codex 凭证、完整仓库、无关文件 |
| Codex → 项目 | 计划只读；批准后或 direct 在原目录修改和测试 | 未批准的原目录写入、超范围命令、生产或外部高风险副作用 |

## 6. 可靠性设计

- **幂等创建**：相同分享重试返回同一 `task_id`。
- **原子领取**：一次只有一个 Connector 执行任务。
- **恢复**：当前按任务保存的准确 thread/turn、交付状态和文件证据恢复；不能确认时受阻。租约续期及任意步骤安全重派仍属目标设计。
- **状态单调**：状态只能按允许的转换前进；终态不可被旧事件覆盖。
- **证据终态**：当前状态名为 `completed`，需要报告 v3 及真实验证证据；视觉任务执行上方完整交付门槛，不以模型自述替代。
- **素材校验**：哈希、大小、MIME、媒体解码与病毒/恶意内容检查。
- **共享工作树**：执行前核对相关文件并保留用户已有改动；不宣称全面锁定项目，或完整区分其他客户端改动和全部 shell 写入。
- **计划幂等**：计划决定绑定当前版本及会话，重复有效批准不创建第二执行回合；设置开关不放行旧待批任务。

## 7. 本地目录建议

不要把临时视频和转写散落进用户仓库。Connector 使用操作系统应用数据目录：

```text
droprun-connector/
  config/            非敏感配置
  tasks/<task-id>/   临时素材、事件与报告
  legacy-workspaces/  旧隔离任务的恢复数据；新任务不创建副本
  schemas/           当前 Codex 版本生成的协议 schema
```

密钥进入系统凭证库，不进入上述普通文件或日志。任务目录按保留策略自动清理，清理行为必须可配置且可恢复边界明确。

## 8. 架构验收

- 错误项目执行率为零。
- 同一任务重复执行率为零。
- Connector 离线、重启和网络中断后不丢任务。
- 云端数据库与对象存储中不存在 Codex 登录凭证和完整仓库副本。
- 外部材料中的提示注入无法扩大权限。
- 报告中的每个“已完成”均可追溯到产物或验证事件。
