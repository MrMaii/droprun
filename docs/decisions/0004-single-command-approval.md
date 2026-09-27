# ADR 0004：手机仅批准单次命令

状态：Accepted（单次命令审批保留；执行目录部分 Superseded）；日期：2026-09-07。

2026-09-07：执行目录部分由 [ADR 0009](./0009-original-project-plan-approval.md) 取代。新任务计划阶段只读、不允许提升审批；计划批准后的执行或 `direct` 使用原目录，超范围命令继续按本文单次审批。下文副本描述保留旧执行语义，不表示原目录任务仍创建副本。

## 决定

在隔离副本基础上使用 App Server `on-request` / `user` 审批。手机展示完整命令、cwd、原因、额外权限与过期时间。批准前再次确认，只返回该回调的 `accept`，不选择会话授权、execpolicy amendment 或网络策略修改。

审批由任务所属设备决定，固定任务/thread/turn/item/内容，首次注册后 15 分钟过期。同一请求不重复执行；取消、撤销设备、结束、变更回合或 Connector 重启使旧请求失效。网络失败不能视作批准。准确端点见 [CONTRACTS](../technical/CONTRACTS.md)。

本机 0.153.4 的真实命令请求提供 `accept`、策略扩展选项及 `cancel`，不一定提供 `decline`。优先使用可用的 decline，否则使用 cancel；cancel 停止当前回合，以 cancelled 和三段执行器报告回传。不能为了适配差异自动选择长期授权。

## 边界

- 手机暂不支持长期目录授权、网络批量放行、MCP 审批或任意交互输入。未知请求明确受阻。
- 用户批准的命令可能超出副本写入边界；不是精细文件 ACL，也不能自动证明命令没有其他副作用。
- 当前网络与读取限制仍见 [ADR 0003](./0003-isolated-task-execution.md)。审批界面不等于已完成项目级权限配置。
- 审批不使用手机离线 outbox；有效期内可重新提交相同决定，但不能推翻已作出的相反决定。
- D1 当前保存最新审批状态；失效会覆盖决策状态，尚不构成完整历史审计。

## 验证

本地测试覆盖单次响应、重复回调、两种拒绝协议、归属、不可变内容、相反决定竞争、取消、撤销、过期、重启和不同回合。真实模拟器与 Codex 结果统一记录在 MVP_ACCEPTANCE（历史私人验收记录，未随公开版分发），失败记录保留，不能拿单元测试替代。

协议依据：[App Server](https://learn.chatgpt.com/docs/app-server)，2026-09-07 核查；结合本机生成的 CommandExecutionRequestApprovalParams / CommandExecutionApprovalDecision 与真实 callback 验证。
