# ADR 0008：手机私有产物与差异交付

状态：Accepted（私有产物下载保留；副本与自动差异部分 Superseded）；实际验收另记。
日期：2026-09-07

2026-09-07：新任务自动生成 `changes.patch`、差异相对副本及“不自动合并”部分由 [ADR 0009](./0009-original-project-plan-approval.md) 取代。原目录任务准确披露直接修改，保留显式产物的私有下载和核验；旧副本产物与历史记录保留。

## 背景与决定

电脑路径不是手机交付入口。Connector上传已核验的声明产物及changes.patch，复用私有R2。D1保存任务元数据和ready状态，手机认证并核对任务归属后才能下载，不提供公开对象链接。

文件ID由kind和相对路径生成；同任务同ID元数据不可修改，对象键包含taskId/id/SHA-256。上传核对真实字节哈希和长度，发布前复查任务、设备和项目授权版本。

Android原生预览文本/图片，不执行HTML、脚本或patch。文本预览限1MiB；其他文件可另存。下载进入私有缓存并核对SHA-256与大小，保存前重新检查授权，使用系统文件选择器选择content URI。

差异相对隔离快照起点，追问包含累计修改，不自动合并原项目。生成差异不执行Git外部diff、textconv或git add过滤器，不改变原索引。

## 后果与边界

- 单文件50MiB、单任务100MiB，最多63个声明产物及1份diff。超过手机上限的本地有效产物也会受阻。
- 拒绝.env及.env.*、.git和pem/key/jks/keystore路径；不是文件内容秘密扫描。
- 上传失败阻止completed；前几个成功文件可能已可见，不代表整个任务完成。
- 旧终态任务不自动补传或重跑。删除云端任务同时删除其产物，不删除电脑副本、Codex会话或已导出的手机文件。
- 撤销阻止新请求，不能收回已下载或正在传输的字节。未承诺立即抹除全部私有缓存；完整保留清理、备份删除和异常孤立对象回收尚未覆盖。

## 替代方案

公开对象URL不满足任务归属，外部浏览器会扩大凭证和活动内容边界。首版采用认证下载与原生预览，不增加第三方预览服务。

## 验证

tests/deliverables.test.mjs覆盖归属、撤销、哈希、大小、停止上传和删除。tests/publish-deliverables.test.mjs覆盖真实Git差异、产物字节及索引不变。手机实测见 验收账本（历史私人验收记录，未随公开版分发）。

官方参考（2026-09-07核查）：[R2 Workers API](https://developers.cloudflare.com/r2/api/workers/workers-api-reference/)、[Android文档保存](https://developer.android.com/training/data-storage/shared/documents-files)。
