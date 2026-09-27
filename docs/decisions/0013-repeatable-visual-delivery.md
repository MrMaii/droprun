# ADR 0013：可重复交办的视觉任务交付

日期：2026-09-12  
状态：已采纳；0.3.0 已实施。发布状态、实测范围与产物见 0.3.0 发布记录（历史私人发布记录，未随公开版分发）。

## 背景

用户要求把 Instagram 网页效果分享到已有个人网站，注明目标页后继续刷视频，由 Codex 自动修改，并在手机收到能核验的说明、截图和预览。此前的单次配色示例不能证明反复改页、动态效果一致性、旧预览稳定性和后台交付。用户本轮授权自主完成支持该场景的版本；这不意味着今后经 DropRun 交办的每个项目都获得生产发布或对外操作授权。

保留 Android、Relay、Connector 和原项目 Codex 的分工。新配对默认直接执行沿用 [ADR 0011](./0011-relay-station-ux.md)，已有手机设置不重置；可选计划审批和单次高风险授权仍有效。本决定更新 [ADR 0012](./0012-visible-results.md) 的取样、交付门槛和预览生命周期；不重写其历史发布事实。

## 决定

1. **改页前先只读定位。** 根据留言识别视觉任务，在原会话的只读回合记录效果行为、目标路由、页面用途、相关文件和验收要求。动效需要时序材料；封面、简介和字幕不足以通过动态参考检查。模糊页码或缺少必要材料时受阻，不先猜着修改。效果名称不确定时描述行为。
2. **分开机械证据与视觉判断。** 执行后由 Connector 的浏览器实际访问目标路由、触发交互、检查文本或样式并截图。程序核对路由、HTTP、动作、断言、截图哈希、版本及修改时序；另开只读图片复核回合，对照参考和交付截图检查用户目标。可做一次针对性补修，仍不满足时受阻。静态截图不证明全部缓动和连续时间细节。
3. **完成必须等交付成功。** 采用报告 v3：给人的 Markdown 与机器核对的 `droprun` 证据块分开。截图、浏览器检查、预览描述和上传进度由执行器持久保存；传输失败先恢复交付，不把再次修改项目当成上传重试。缺少准确回合或证据时不补造完成状态。
4. **旧报告有可识别的结果版本。** 静态页面/构建目录生成受限快照；同项目下一任务保留前一任务的静态预览。动态 `live` 预览在同项目下一任务修改前关闭，重开显示当前项目，不能冒充历史快照。链接、服务进程、快照保留期各自独立；过期或提前停止要反映真实状态。重开是恢复已有描述的预览请求，不创建新的编码任务。
5. **用户离开分享页后继续接收。** 分享时启动有可见通知的限时 `dataSync` 前台服务，增量拉取任务结果；完成或需要处理时通知，工作结束后停止。周期 Job 保留为离线、系统限制及服务停止后的补偿。这是本版本可运行的接收方式，不宣称已接入 FCM，也不保证锁屏、杀进程或离线时仍秒达。
6. **先约束资源和恢复，再扩容。** 保留私人 Connector 串行执行、精确任务/项目授权检查和终态不回退。相关文件开始修改前检查漂移；不承诺全面锁住桌面编辑。预览容量、快照清理和任务列表有明确上限，不宣称无限队列或多租户规模能力。

具体结构、阈值、状态机、恢复条件和 API 只在 [CONTRACTS 当前增量](../technical/CONTRACTS.md#当前增量可重复视觉交付-030) 维护；产品验收在 [PRD](../product/PRD.md)，组件所有权在 [ARCHITECTURE](../technical/ARCHITECTURE.md)。

## 外部运行边界

- Android 14 起前台服务需要声明类型和对应权限；Android 15 对后台 `dataSync` 设累计时限，并要求及时处理 `onTimeout`，从可见用户操作启动。Android 16 中 JobScheduler 仍受执行配额影响，不能把常驻 Job 当无限接收通道。依据：[服务类型](https://developer.android.com/develop/background-work/services/fgs/service-types#data-sync)、[后台启动限制](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)、[超时](https://developer.android.com/develop/background-work/services/fgs/timeout)、[Android 16 配额](https://developer.android.com/about/versions/16/behavior-changes-all#job-scheduler)，核查日期均为 2026-09-12。
- 预览沿用 Cloudflare Quick Tunnels；官方将其定位为测试开发工具，不保证 SLA。本决定没有把临时链接升级成永久生产托管。依据：[Quick Tunnels](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/do-more-with-tunnels/trycloudflare/)，核查日期 2026-09-12。

## 验证与未承诺项

实现代码和自动化测试覆盖材料充分性、目标文件漂移、真实浏览器动作、视觉交付门槛、上传恢复、预览版本与权限、增量列表及 Android 接收策略。各次命令结果和真实集成记录归发布记录，不在此维护另一份测试总数。

尚不能由代码或模拟测试推出“20 条真实 Instagram 分享已通过”或确定的通知时效、动效复现成功率。真机连续分享、锁屏、断网、电脑重启及桌面并行编辑仍按场景验收逐项记录；是否完成以实际证据为准。
