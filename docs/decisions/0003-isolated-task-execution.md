# ADR 0003：任务快照与独立执行进程

状态：Superseded（新任务及新追问的快照与副本 cwd）；日期：2026-09-07。

新执行模式见 [ADR 0009](./0009-original-project-plan-approval.md)：新任务使用原目录，`legacy-isolated` 仅兼容旧运行恢复。独立 App Server、后台 MCP/插件/Apps 限制继续保留；下文快照上限与历史验证仅适用于旧路径，不授权删除已有副本或修改历史报告。

## 背景

桌面项目可能有未提交、已暂存或未跟踪文件，也可能尚无 Git 提交或根本不是 Git 仓库。只从 HEAD 建 worktree 会丢失用户当前上下文；直接修改原目录会覆盖并行工作。

## 决定

- 新任务复制当前可见源码到仓库外的任务目录，建立私人 Git baseline，不操作原仓库 index、提交或远端。
- 已隔离任务的追问沿用原副本。历史非隔离会话在下一次追问创建副本并以新 cwd 恢复原 thread，不另建替代会话；历史绝对路径仅作只读参考。
- 每任务启动独立 App Server，进程 cwd、thread cwd、runtimeWorkspaceRoots 与 turn writableRoots 均指向副本。全局 App Server 仅用于项目发现和恢复读取。
- 在执行进程中禁用配置中的 MCP、插件与 Apps；核对生效配置，再检查该 thread 的 MCP 工具清单。覆盖仅存在于子进程命令行，不改用户全局配置。
- 报告第三段附副本路径、Git 变更文件与基线原文件漂移；不自动合并回原项目。

## 理由与替代方案

快照覆盖有提交、未提交和非 Git 项目，不要求用户先提交。代价是没有原 Git 历史、远端和忽略依赖；不是完整环境克隆。未来可在明确需要时增加原生 worktree 路径，但不能默默用 HEAD 替代当前文件。

本机 Windows 探测表明，仅设置命令 cwd/writableRoots 不足以证明原目录被保护；启动进程也必须位于副本。集成使用这一实测要求，不把单次探测推广为所有系统的安全保证。

## 限制

- 当前最多 20000 文件、总计 1 GiB、单文件 100 MiB；链接、子模块和特殊文件拒绝自动复制。
- Git 项目排除忽略文件；非 Git 项目排除常见生成目录。常见凭证文件名被排除，但这不是全面秘密扫描。
- 当前沙箱限制写入，未实现项目专属读取边界或网络目的地白名单。网络允许，用于必要依赖与检查；不得宣称已完成全部权限或外部副作用防护。
- 被忽略产物不一定出现在 Git 文件列表。原目录漂移检测仅覆盖复制基线中的文件，不是完整冲突合并。
- 细粒度项目权限、完整手机审批、合并/应用操作与材料保留清理尚未完成。单次命令审批见 [ADR 0004](./0004-single-command-approval.md)，固定项目授权 UI 见 [ADR 0005](./0005-project-authorization.md)；工具限制缺少必要能力时应明确受阻，不绕过。
- 本机 CLI 对覆盖键按点分段；包含不支持字符的工具配置名称将使任务受阻，不冒险写错覆盖项。

## 验证

- `tests/workspace.test.mjs`：脏文件与暂存状态保留、未跟踪源码、非 Git/无提交、原目录未覆盖、漂移与路径拒绝。
- `tests/execution.test.mjs`：配置不变、无凭证复制、MCP 清单门禁。
- `.local/sandbox-isolation-audit.json`：2026-09-07 17:13:27 UTC，真实 App Server 副本可写、原目录合成 canary 被拒绝，工具门禁通过。
- `.local/isolation-e2e.json`：真实 Relay/Connector/Codex 新任务与追问，同 project/thread/cwd，回归测试由两项增至三项并通过，原目录和旧报告未变。
- `.local/isolation-screen-audit.json` 与截图：Android 缓存及渲染了三段报告和隔离交付说明。
- `.local/legacy-isolation-e2e.json`：17:25:29 UTC，真实旧会话追问迁移 cwd，同 thread、原文件及旧报告未变。

协议与配置依据：[App Server](https://learn.chatgpt.com/docs/app-server)、[Configuration Reference](https://learn.chatgpt.com/docs/config-file/config-reference)，2026-09-07 核查；实际字段以本机 0.153.4 schema 为准。
