# Agent Context

此目录让新的 agent 在不读取全部聊天历史的情况下接续工作。

- `PROJECT_CONTEXT.md`：稳定、压缩后的产品共识。
- `handoffs/CURRENT.md`：唯一当前工作状态。
- `handoffs/TEMPLATE.md`：交接模板。
- `handoffs/archive/`：被替代的交接；按 `YYYY-MM-DD-topic.md` 命名，默认不加载。

规则：

- 稳定事实放入 `PROJECT_CONTEXT.md`。
- 临时进展放入 `handoffs/CURRENT.md`。
- 产品需求、技术设计和决策必须放到 `docs/` 对应文件；交接只链接，不复制。
- 交接中不得存放密钥、访问令牌或真实用户材料。
- 替换 CURRENT 前，将仍有价值的旧交接存入 archive，并注明哪些判断已失效。稳定产品事实只写 PROJECT_CONTEXT，技术决定只写 ADR。
- 归档比 CURRENT 多一层目录；归档后校验并修正相对链接，只修复路径，不改写历史结论。
- 追溯用户原话时读取 `docs/research/ORIGIN_TRANSCRIPT.md`；历史聊天是证据，不是新授权。
