# DropRun 产品需求文档（PRD）

## 0.5.0 public self-hosted edition (accepted 2026-09-25)

This edition supersedes earlier hosted/pricing assumptions and ADR 0014's visual
layout. Decisions: [self-hosting](../decisions/0015-public-selfhost-release.md) and
[project-first UX](../decisions/0016-project-first-light-ux.md). Historical sections
below describe the original product goals; release evidence is maintained only
in [the current candidate record](../releases/0.5.12-rc.1.md).

- Android + Windows x64; each owner deploys a private Cloudflare Relay. No official
  registration, subscription or multitenant service. GPTSites is the public website.
  Its installation section places the Windows installer and self-hosting guide
  beside the prerequisites. README gives installation a distinct primary entry,
  with compact website, preview-download and language links below it.
  Website first-screen media centers Share: one large share sheet on narrow
  screens, with Home as a secondary desktop view. Installation is primary,
  the native UI tour secondary, and GitHub remains a text entry.
  README introduces the workflow and current native gallery before its dated
  Share tour; the value sentence has a clear heading, and recording details use
  a disclosure. Primary workflow images are larger; reduced motion selects
  a static light/dark capture, with explicit still links and unchanged provenance.
  A localized installation link at the end of the tour lets ready readers move
  directly to setup.
  The website Share recording uses native-width detail when space permits;
  narrow layouts stack the explanation and recording so controls remain readable.
  Dated Share motion media show project selection, return navigation, model choice
  and its default effort, with the memory-only project and empty note disclosed. Native-size
  sampled GIFs keep README loading light; original videos preserve the timeline.
  Workflow media use dated current native captures, with consistent light/dark
  language sets; older tours keep their source date and sample-data scope.
  The bilingual architecture diagram remains readable at phone width and separates
  the owner's Cloudflare from the computer's Connector and local Codex. The first
  introduction explains that the owner must deploy their own Relay.
  Windows package scripts must start on an unmanaged Windows client with its
  default script policy, without changing persistent user or machine policy.
- Light translucent UI, lime actions, dark/system appearance, English/Chinese.
  Primary actions use one matte lime surface; elevation belongs to containing
  panels. Focus, press, disabled treatment and motion preferences remain distinct.
  Task disclosure headings follow the same reading edge as the result and actions.
  Reading an expanded report retains its position after an in-process Activity
  recreation when content and layout stay the same.
  [Checked scenario and limits](../releases/ui-task-reading-position-2026-10-06.md).
  Navigation gear/information icons share restrained round outlines; their 48dp
  targets, theme tint, focus, press feedback and action meaning remain distinct.
  Home contains Recent handoffs and settings, with only used or pending projects.
  Project names, counts and status form recognizable groups using the
  [current surface decision](../decisions/0019-semantic-content-groups.md).
  Recycled Home rows update status text and its tint together. Large text puts
  the date below the status, preserving natural wrapping and the project target;
  normal text retains the compact horizontal footer.
  Older list dates identify the calendar date without requiring mental arithmetic;
  absent dates are explicit. The display policy is owned by CONTRACTS.
  Returning to Home or recreating it after an appearance change preserves the
  visible project and reading offset; routine updates do not reset the list.
  Inserting/removing pending entries preserves the record and offset in both
  touch and keyboard mode, with the reading position captured before data changes.
  Settings recreation preserves scroll position and expanded project access.
  Keyboard users keep focus on the appearance, language, model or effort control
  they just changed, with that control visible. Cancel preserves the selected
  values and reading position; local model/effort choices survive recreation.
  Default model and effort use the same setting-row hierarchy as appearance
  and language. Known effort names are localized; storage keeps the original
  protocol IDs and unknown choices remain readable verbatim.
  Background settings refresh also retains the focused appearance or language
  control. Structural page focus must not tint the entire content surface.
  Settings offers Refresh/Retry beside model availability, reading the existing
  project, execution-setting and retention data once at a time. A pending read or
  mode/project change disables this action; partial failure keeps cached data
  visible and offers Retry. Completion retains the then-current reading offset
  or surviving preference/Refresh keyboard focus, without writing execution mode.
  Manual pairing validation keeps entered values and shows a complete, visible
  form error for the active input method; format checking never sends a pairing
  request. A nonempty link takes priority. Help and errors explain that using
  the retained three values requires clearing the link first.
  Share project and intent steps each have one main question, without a
  duplicate section label. Material, execution context and actions remain clear.
  The selected destination stays prominent above the optional note, wraps its
  complete name and remains individually readable before the Send action.
  In the note step, project identity and received material share one read-only
  confirmation surface. Larger text removes the decorative project initial.
  Selecting an available, authorized project advances directly without moving
  that row or adding a fixed delay; selection alone never sends a handoff.
  Reversing the share scrim continues from its current color, cancels its previous
  animator and clears the held animator on destruction. Reduced motion sets the
  destination color immediately. [Scoped checks](../releases/ui-share-confirmation-2026-10-06.md).
  Project search matches the displayed label or full project ID, ignoring outer
  whitespace and case without changing the query, selection or directory order.
  Both pre-Send steps offer a compact, accessible material disclosure.
  Material and model controls use light text rows with visible trailing arrows;
  the optional note remains the distinct input surface. Their complete click
  targets, press feedback and expanded-state descriptions remain available.
  Expansion shows exact received text/URLs and each complete filename in order,
  without fetching links or opening files. It distinguishes received payload from actual
  reading coverage, omits empty sections and preserves expansion across local
  step changes. Long material remains individually reachable by scrolling.
  The optional model control shows a title, summary and expanded state. Effort
  choices show localized meanings with complete 48dp option rows; protocol IDs
  and unknown values stay intact. Focus, project and note survive local choices.
  Compact share windows retain usable height and readable draft feedback;
  taller windows gain breathing room continuously. Short forms still wrap
  naturally, and large-font content remains scrollable within safe bounds.
  Share-save feedback gives offline or disabled-notification instructions a
  readable window, including when animations are off; Close stays immediate.
  It reflects connection/notification state when the flight finishes. Ordinary
  saves retain prompt return; phone persistence never implies Relay receipt.
  The Codex label remains complete at 200% font. Pending execution-setting
  choices report disabled state and cannot take keyboard focus; they become
  available again when the request settles.
  Execution feedback stays beside those choices: neutral while saving, green
  after confirmation, warning on failure or connection change. Accessible option
  names include the visible title, any explanation and the selected state.
  Delivered screenshot links preserve the image ratio and have a touch target
  at least 48dp tall. Saving a decision disables their navigation and keyboard
  focus along with the delivery buttons; normal access returns when it settles.
  A delivered result uses the page's reading width: a clear result heading,
  readable summary with its original list lines and the existing preview actions
  in their original order. The task title stays subordinate to the result's
  reading area, and ready-preview explanation stays beside its action.
  Full reports and evidence remain available through their disclosures.
  Task-action failures never become success confirmations because error text is
  absent. Missing detail gives a localized next step: check the latest handoff
  state before retrying. The controls become available when the callback settles.
  A task refresh with missing detail keeps the cached result and displays a
  localized notice that another refresh will follow; useful error detail stays intact.
  A static preview explains that it is a fixed snapshot. Its complete version is
  selectable in a separate collapsed section, with expansion state restored
  after page recreation; it does not fill the main result explanation.
  File previews put content before full technical evidence. A compact verified-file
  disclosure retains the exact selectable SHA-256 and original non-execution
  explanation. Its expansion preference survives Activity recreation; file
  download, cache verification, decoding and Save/Back behavior stay unchanged.
  A configuration recreation continues observing the same pending mode change,
  including its eventual success or failure, without starting it again. Already
  displayed results are not announced again after recreation. A settled failure
  receives the same guarded, once-only accessibility announcement as success.
  Pending saves remain silent. Old completions
  cannot replace a newer decision, disconnect feedback or another connection.
  Mode-change progress and results appear beside the affected choices, so viewing
  those choices does not scroll their feedback out of the safe area.
- Project totals count retained independent root tasks and accepted dispatches,
  including follow-ups. Retries never count; local pending shares are separate.
- Project history is paginated. Saved outbox entries become immediately visible,
  survive interruption, and merge with server acknowledgements by UUID.
  An empty history shows loading until a read completes, and only a successful
  zero-record read confirms that no handoffs are present. Failure offers Refresh;
  null or blank error details use a useful localized message. Cached and pending
  rows stay visible, with failure feedback separate from the zero-record surface.
  While a history read is pending, Refresh has a persistent disabled/dimmed state
  and a localized refreshing description. Completion restores its usual state;
  repeated clicks reuse the existing busy guard without extra reads or moving rows.
  Only a notice with actual details offers clicking, focus and press feedback.
  Static errors, removal status and requested-retry feedback remain readable;
  switching to readonly or hidden resets the notice's scale. Retry feedback
  cannot inherit an older details action. [Scoped checks](../releases/ui-history-notice-feedback-2026-10-06.md).
  Project identity has its own bounded heading, separate from Back and Refresh.
  A 48dp information entry exposes the complete name and ID when the heading is
  shortened; same-name and retained-history identity remain explicit. Recycled
  status pills update their background and text from the same actual status.
  Each history row reads title, current state, then complete source/date metadata.
  State keeps its natural pill width within the space reserved before the arrow;
  metadata retains the full reading width. Long text may grow vertically.
  Status/pending updates retain the visible task and offset without replaying
  entry motion. Activity recreation restores the reading position in retained
  older pages for both touch and keyboard browsing.
  Consecutive updates before the next frame must preserve the same visible record.
  This also applies when keyboard focus is on a header action and the list has no
  selected row. Refresh must preserve that focus; a focused task card must still
  open the same task when Enter is pressed after updates.
  Removing a saved copy shows immediate progress, survives rotation as one
  operation, and does not wait for a project-history network read. Failure remains
  visible after polling and rotation; interruption never claims successful removal.
  Confirmed server deletion immediately removes the record from retained history
  and detail caches. Late reads cannot restore it; a failed count refresh does not
  present the deletion as failed. Other history rows and paging remain available.
  If a deletion acknowledgement is lost, keep the cached result for an explicit
  retry, while preventing the same saved UUID from being submitted again.
  An inaccessible server record can offer an explicitly confirmed local-cache
  clearing action. Explain that cloud deletion is unconfirmed and an accessible
  server record may return; cancellation retains the phone's saved report.
- Incoming-file failure keeps verified complete copies and identifies missing
  material. Restoring the failure does not retry a provider automatically. Retry
  copies only missing files; repeat taps cannot start concurrent copies. Incomplete
  shares cannot be handed off. Failure actions remain visible while content scrolls;
  discarding requires confirmation and only removes this share's private copies.
  Short landscape screens retain a readable explanation and reachable actions at
  200% font. Rotation does not restart a failed provider or change share identity.
  An interrupted unsent share must remain discoverable after its original page is
  gone, with its material, note and target choices intact. Recovery is scoped to
  the intended instance and never sends automatically. Home and pre-pairing setup
  expose unfinished shares separately from project history. Saving, saved and failed
  states must be distinguishable; discard requires an explicit choice. A draft saved
  before pairing requires confirmation of the target computer and Relay before use.
- Reports prioritize required action and verifiable deliverables, preserve full
  approval context, and correctly disclose original-directory execution.
  The execution-mode label describes configuration, not a pending decision.
  Missing or unknown modes do not imply approval is required or isolated execution.
- Screenshots/files are baseline delivery; compatible static exports have cloud
  snapshots. Live previews are optional owner-managed infrastructure. Absence of
  preview is explicitly reported, never described as verified preview completion.
- Install, resume deployment, pair, revoke, update and recover are first-class UX.
  The Windows guide distinguishes required computer readiness from optional
  source-extraction tools. Their installation remains visible and can happen later;
  unsupported sources still disclose their actual reading limits.
  Update and uninstall refuse while work is being acquired or executed, and wait
  for the Connector process to exit before replacing files or removing startup.
  Unknown shutdown results retain the current installation for an explicit retry.
- Public candidate may ship with named test gaps. Stable release requires clean
  Windows/cloud setup, physical Android validation, at least 3 correct project
  mappings, 5 continuous full tasks and 20 real-source samples with honest coverage.
- Android distribution names must match the built version, retain existing exports,
  and accompany the signed APK with checksums, source identity and dependency notices.
- Windows distributions must identify their source commit, provide source/checksums,
  inventory every payload file and exclude untracked local project materials.
- Accessibility: 48dp targets, 200% font, TalkBack, motion off, API26 fallback.
  Physical-device performance targets: <5% overdue frames, no >700ms frozen frame,
  p95 press feedback <100ms. These are goals, not measured results.
- UX polish acceptance: project/computer names remain readable at 200% font;
  the optional note keeps its visible field label after typing;
  empty-home guidance and share actions remain reachable by scrolling; project
  and task cards can receive keyboard focus and open with Enter; pending entries expose failure and
  retry; follow-up drafts survive Activity recreation. Presses, navigation and
  disclosure changes have feedback with a motion-off path. Destructive or
  permission-expanding decisions require confirmation; ordinary selections show
  their selected value without an extra confirmation dialog.
  Disclosure chevrons take a short 90-degree turn and can reverse during motion
  without leaving their closed-to-open angle range or changing callbacks.
  Disclosure content also continues from its current opacity when reversed.
  Settings and project history use the common page reading edge; the connected
  computer retains its distinct surface. Model availability, Refresh/Retry,
  model and effort form one matte content group with complete values at 200%
  text. [Grouping decision](../decisions/0019-semantic-content-groups.md).
  At 150% font and above, project access
  actions follow the full project name on a separate line. A page-wide save disables
  and dims permission controls until its actual completion.
  Delivery uses the ready preview as its primary action, an outlined file entry
  and a quiet follow-up button with the same target and interaction states.
  Empty Home explicitly identifies DropRun as the system-share destination.
  Home sync errors use a compact attention notice with the original cause in
  details. Manual status checks show immediate, accessible progress, prevent
  duplicate checks and return to the actual cached/connection state. They do not
  promise that pending material was received or project work completed.
  At 150% text size and above, project titles wrap fully instead of hiding the suffix
  that identifies a project.
  Home projects use independent matte surfaces, decorative initials and separate
  identity/count and state/recency areas. Hide initials at150% text and above;
  preserve stable IDs, complete names, focus, counts and reading position on rebind.
  Shared actions, icons, chips and model/effort options show a distinct keyboard-
  focus outline in both themes, without changing layout or activating the action.
- Scrolling text stays outside system bars. Decision errors remain visible after
  polling; expired cached approvals and previews offer accurate next steps.
  A terminal handoff distinguishes a missing preview from an unavailable or
  expired one. Recovery guidance points to delivery files only when that entry
  exists. The primary action helps inspect this delivery: a ready preview, or
  screenshots/files when no preview is ready. Follow-up remains available as a
  secondary action; emphasis does not change permissions or reopen conditions.
  When a report exists, its summary, existing screenshot, preview state and
  inspection actions share one stable content surface. Follow-up, approvals and
  the full report remain separate. Missing reports keep their actual progress
  state; the layout does not fabricate a result or preview.
  Data settings show the last known policy for the selected Relay, distinguish
  unknown/offline values, and explain local pending copies and server retention.
  Actual retention periods and unknown-policy recovery use primary readable text
  in one content group. Reports/preview/cleanup rules stay clear; cached-policy
  status and deployment context are secondary. Typography does not edit policy.
- Share search and catalog expansion survive recreation. Permission explanations
  and actions remain reachable at 200% font; keyboard focus stays inside the
  open dialog. Failed permission changes remain retryable without advancing.
  Permission-success feedback may be closed without letting its old animation
  dismiss a later dialog or advance a different project. Closing that feedback
  does not revoke a permission already confirmed by the server.
  Selecting a model or effort in the share editor preserves keyboard focus on
  that option and keeps it visible. These choices do not change Settings defaults
  or create a handoff before Send.
  Before Send, the editor shows the saved execution preference and its consequence,
  or explicitly says it is unconfirmed. It explains that the Relay's setting on
  first receipt determines the task's mode; a cached preference is not a promise.
  Confirmed execution settings are readonly context, while unknown settings keep
  a distinct warning surface and the complete next step.
  The notice and Send remain readable by scrolling at 200% font. Model summaries
  use localized effort meanings, retaining unknown values and the exact request.
- Share selection prioritizes recent server and local activity; identical names
  remain visibly distinguishable without merging their IDs, counts or permissions.
  Unavailable projects explain recovery and cannot create a new handoff.
- Windows setup lets users match phone project IDs to local folders in a read-only
  inventory. Empty, unavailable and failed checks have clear recovery; partial
  inventories must not be presented as complete. Paths remain local.
- Setup actions show immediate waiting feedback and prevent conflicting or duplicate
  clicks. Errors remain readable after polling. Unknown status blocks further
  mutations until recovery; an uncertain acknowledgement must not trigger an
  automatic retry. Read-only release checks have their own waiting/retry feedback.
- Delivery-file previews recheck cached size and hash after recreation and before
  export. Changed or expired content must not appear as verified. Loading, empty,
  failed/retry and cancelled-save states preserve an accurate route back to reports.
- File saving shows waiting/results beside the action and prevents duplicate
  requests. Recreation preserves the active save; losing its state explains the
  unknown result without automatically writing again. Failure retains the preview
  for explicit retry. Leaving during a save requires an accurate confirmation.


状态：目标需求，含已采纳的 0.3.0 增量  
更新时间：2026-09-12  
负责人：待定

文档性质：目标需求，不是已实现功能清单。原目录执行与计划审批边界见 [ADR 0009](../decisions/0009-original-project-plan-approval.md)，新配对默认直接执行见 [ADR 0011](../decisions/0011-relay-station-ux.md)。代码覆盖情况单独维护在[实现现状](../technical/IMPLEMENTATION_STATUS.md)，不能用本 PRD 宣称功能已上线。

## 当前增量：反复分享网页效果

已采纳 [ADR 0013](../decisions/0013-repeatable-visual-delivery.md)：用户将 Instagram 网页效果转发到已有网站并注明目标页后，可以离开分享页继续刷视频。视觉任务先自动核对材料和页面，再在既有执行授权内修改；报告解释实际看到的效果、改了哪页及其用途，附可核对截图与版本明确的预览。效果名称不确定时描述行为，不编造术语。

连续使用要求：材料不足不误改、模糊页面不猜改、未验证不报完成；前一报告的静态结果不随下一任务变化；连接或上传短暂失败可恢复；结果通知尊重手机通知开关及系统后台限制。产品验收如下，运行阈值和实现结构见 [CONTRACTS](../technical/CONTRACTS.md#当前增量可重复视觉交付-030)，本轮发布及实测状态见 0.3.0（历史私人发布记录，未随公开版分发）。

| 场景 | 验收要求 |
|---|---|
| 复现指定页面动效 | 记录实际取得的时序画面、目标路由及用途；参考不足或无法唯一定位则明确受阻，执行前不修改。 |
| 真实项目页面定位 | 支持查询参数与 hash 页面；由 agent 查找文件及验收行为，不要求用户提供技术路径。内部定位结果不完整时先只读补正一次，持续失败说明实际原因；不得把合法地址误报成用户未说明页面。 |
| 交付截图与报告 | 实际打开每个目标页，按效果触发动作并检查结果，再对照截图复核；全部必要交付上传成功后才能完成。 |
| 连续分享同一项目 | 每个分享有独立会话和报告；静态预览绑定本次快照版本，live 明示反映当前项目；旧链接失效有真实状态和重开入口。 |
| 离开 DropRun | 分享后启动有可见状态的限时结果接收，完成或需处理时通知；通知未开启、长时离线或系统停止时明确说明如何查看。 |
| 网络中断或 Connector 重启 | 已完成的编码不因待上传截图而重做；核对已有任务、回合及文件哈希，不能确认则受阻，不造完成记录。 |

上述为验收要求，不代表已完成 20 条真机 Instagram 样本或已经测得稳定成功率。

## 1. 产品定义

DropRun 是“给项目 agent 的灵感收件箱”。用户在手机上看到可能对项目有用的视频、图片、网页或文字后，直接分享到 DropRun，选择已经连接的项目并可选地留言。DropRun 获取材料、建立新任务、让该项目的本地 agent 在已授权范围内完成判断与工作，再把真实产物和验证结果回传到手机。

核心承诺：

> 不用先把灵感整理成完整需求；转发给那个项目，它接着做。

## 2. 问题

### 用户现状

用户刷到有用内容后，通常只能收藏，或手动完成以下流程：保存/复制链接、打开电脑、找到正确项目、向 agent 解释材料和项目背景、监督执行、再寻找最终结果。

### 需要解决的工作

当用户看到一个可能有用的参考时，希望低成本交给已经理解项目的 agent：

- 有明确想法时，按留言在项目中落实。
- 没有明确想法时，让 agent 自己判断是否适用以及用在哪里。
- 用户离开分享界面后，仍能看到任务状态和最终交付。

### 不解决的问题

- 不成为通用稍后读、知识库或视频摘要产品。
- 不替代 Codex 等执行 agent。
- 不承诺读取任意平台的任意视频。
- 不承诺首版在用户电脑离线时立即执行。
- 不在 MVP 中同时支持所有手机平台和所有 agent。

## 3. 目标用户

### 首要画像

- 使用 coding agent 做真实项目。
- 经常从 UI、交互、产品流程和实现教程视频获得灵感。
- 能判断一次项目修改是否节省工作。
- 典型群体：独立开发者、产品型开发者、自由职业开发者。

### 暂非首批用户

- 只想收藏或总结知识视频的人。
- 没有可连接项目环境的人。
- 要求电脑关机后仍由云端完整托管代码执行的人。

## 4. 体验原则

1. **分享面板要短。**确认材料、选择项目、可选留言、提交；不等待长时间分析。
2. **材料状态要诚实。**明确区分收到链接、取得网页文本、取得字幕、取得音频、取得画面和完整视频。
3. **不重复解释。**项目和权限在首次连接时配置，日常分享不要求重新描述。
4. **行动优于摘要。**交付必须落到项目判断、实际工作或明确的不适用结论。
5. **先明确再执行。**新配对默认直接执行，视觉任务仍先自动核对材料与页面；用户可选择先看计划，高风险动作逐次确认。
6. **结果可核验。**报告关联文件、diff、命令、测试或预览；模型自述不是完成证据。

## 5. MVP 用户流程

### 5.1 首次连接

1. 用户在电脑安装并启动 DropRun Connector。
2. 手机安装后首屏是连接引导：默认扫描电脑配对页的二维码建立一次性设备关系；扫码多次失败（识别到非 DropRun 二维码、长时间无识别或相机不可用）时，App 主动提示改用配对码连接。
3. Connector 发现本地 Codex 环境与可用项目。
4. 用户确认哪些项目可以从手机交办（首次转发到某项目时，或在设置的"项目授权"里）。
5. 项目授权允许该手机交办；新配对默认直接执行，可在设置里改为先看计划；既有手机设置保留。
6. 项目列表同步到手机。

### 5.2 分享与提交

自 0.4.0（[ADR 0014](../decisions/0014-mobile-ux-v2.md)）起，分享不打开主 App，而是覆盖在来源应用上的三步轻量浮层，顶部小点指示步骤：

1. 用户在来源应用点击系统分享，选择 DropRun；浮层只读展示收到的链接或文件。
2. 第一步选择项目：未授权的项目弹出毛玻璃授权框，绿色按钮授权后播放打勾动画并自动进入下一步。
3. 第二步留言（可留空直接提交）；仪表盘图标展开后可临时切换模型与推理强度，默认值来自设置。
4. 第三步无需操作：本机可靠保存分享内容后播放纸飞机发送动画，显示“已保存，自动发送”；动画不是服务端接收或视频读取完成的证明。按执行模式提示后续（先看计划：读取可获取的材料后给出方案，等待批准；直接执行：读取可获取的材料后处理），随后自动返回原来的应用。通知关闭时提示从 App 或 Codex 查看；该步骤不弹系统权限框。
5. 任务进入首页的紧凑历史列表（每条 1–2 行：模型生成的简短名称与处理时间），点击查看完整反馈报告。

补充验收：分享期间调节模型与强度只影响本次任务，设置页负责默认值；切换配对页面、旋转及页面恢复不能丢弃已保存的附件或重复提交。模型尚未返回名称时可显示留言或来源作为暂名，不能宣称暂名为 GPT 生成。设置目前提供已有电脑配对与项目授权；多账户 OAuth 接入仍需另行定义后端契约。

### 5.3 材料处理

1. 系统判断收到的是文件、文本还是链接。
2. 文件直接校验；链接交给目标平台适配器。
3. 系统生成素材包：原始来源、实际取得的文件、转写、关键帧、摘要和读取范围。
4. 无法取得关键内容时显示受阻，不得伪装为完整理解。

### 5.4 项目执行

1. 本地设备在线后领取任务。
2. Connector 定位项目身份与工作目录。
3. 准备本地素材，在原项目目录创建新的 Codex 会话，不复制源码或创建 worktree。
4. 选择“先看计划”时，只读核对项目与材料，把理解、拟修改范围、步骤和验证安排回传手机，显示“等待确认计划”。此时不能写项目或请求提升权限。
5. 用户批准当前计划版本后，原会话开始执行回合；拒绝或取消则不执行，不把计划显示成完成交付。
6. 新配对手机的“直接执行”全局设置默认开启，已有设置不重置；从先看计划切回直接执行仍需联网确认。设置只影响此手机之后新提交的任务，包括追问，不放行已有待批计划。
7. 已批准或直接执行的任务在原目录修改和验证；保留用户已有改动。需要超范围或高风险授权时单独请求审批。

### 5.5 结果交付

APP 展示三段式报告：

1. **我看到了什么**：材料内容、实际读取范围、缺失或不确定部分。
2. **我理解它应该用在哪里**：用户留言的理解，或无留言时 agent 的自主判断；对应项目位置。
3. **我做了什么**：实际改动、验证、交付入口、未完成事项。没有修改时必须说明原因。

执行报告必须准确说明原项目文件是否已直接修改；不再称作副本交付或等待合并。显式产物继续支持核验后下载，原目录任务不要求自动附带 `changes.patch`。计划审批页与最终报告分开，计划准备好不能触发“任务已完成”。

## 6. 功能需求

### P0：MVP 必须具备

| 编号 | 需求 | 验收标准 |
|---|---|---|
| P0-01 | 系统分享入口 | 从选定手机平台的目标来源分享后，DropRun 能收到并显示真实 payload 类型。 |
| P0-02 | 设备配对 | 手机能与一个 Connector 安全配对、撤销配对，并显示在线/离线。 |
| P0-03 | 项目发现与授权 | 手机项目列表能对应桌面 Codex 项目；用户可选择允许远程交办的项目。 |
| P0-04 | 可选留言 | 留言原文不丢失；没有留言时被明确标记为无留言。 |
| P0-05 | 素材包 | 每个任务记录实际获取到的内容、缺失内容、来源和处理时间。 |
| P0-06 | 离线排队 | 电脑离线时不丢任务；恢复在线后只执行一次。 |
| P0-07 | 新会话投递 | 每个新分享在正确原项目目录建立独立 Codex 会话，不因全项目快照文件数而受阻。 |
| P0-08 | 计划审批与执行边界 | 新配对默认直接执行，已有设置保留；review 准确版本获批后同会话执行，未批准、拒绝、取消、旧版本或撤销不能开始写入。设置仅对新提交生效；高风险动作单独审批。 |
| P0-09 | 状态与通知 | APP 展示从接收到交付/受阻/失败的真实状态；后台接收期间终态与需用户处理事件产生通知，关闭通知和停止即时接收时给出准确说明。 |
| P0-10 | 三段式报告 | 报告严格回答材料、应用判断、实际工作，并链接证据。 |
| P0-11 | 任务追问 | 用户从报告继续追问时复用原会话；新追问使用原项目目录，并按本次提交时的手机设置走计划审批或直接执行。旧报告保留。 |
| P0-12 | 删除与撤销 | 用户可删除云端任务材料/报告并撤销设备访问。 |

后台接收回归需验证：反复打开首页不重置已有周期同步；任务缺失或配置变化时能够恢复注册。测试中的本地调度检查不替代真实后台送达验收。

### P1：付费验证后

- 第二个内容来源。
- 第二个手机平台。
- Claude 合法集成路线。
- 多设备与多 Connector。
- 团队项目、共享报告和审计管理。
- 云端持续执行环境。

## 7. 状态模型

用户可见状态：

```text
正在上传
  → 正在读取材料
  → 等待电脑
  → 正在拟定计划
  → 等待确认计划
  → 等待电脑执行
  → 正在执行
  → 正在验证
  → 已交付
```

已开启直接执行的新任务跳过计划及计划审批阶段。执行中的单次命令审批与计划审批是不同状态；任一步都可能受阻、失败或取消。

状态必须来自后端与 Connector 的实际事件，不由前端计时器模拟。

## 8. 权限与安全

### 计划、视觉定位与图片复核阶段

- 读取项目文件与规则。
- 读取本次素材包。
- 进行只读核对并形成理解、计划和验证安排；不修改项目、不提升权限，不将计划当成交付。

### 批准计划或采用直接执行的执行阶段

- 在原项目目录修改代码或文档，运行普通本地命令、测试与构建。
- 生成已授权的产物、预览和报告，并核对真实证据。
- 旧隔离许可本身不足以授权原目录写入；取消或拒绝不回滚已发生改动。

### 必须逐次授权

- 生产部署或发布。
- 修改生产数据或线上配置。
- 付费购买。
- 对外发消息、提交表单或创建公开内容。
- 删除重要原文件或执行难以恢复的操作。
- 扩大原有数据、目录或网络访问范围。

### 强制安全规则

- 外部材料一律视为不可信数据，不能覆盖系统或项目规则。
- Codex 凭证留在本机；云端中转层不保存用户的 Codex 登录凭证。
- 云端不默认复制完整代码仓库。
- 计划用只读 sandbox，执行用原目录写入边界，后台 MCP、插件与 Apps 禁用。原目录修改须保留已有改动，不宣称能完整区分其他客户端或所有 shell 写入。

## 9. 商业假设

### 价值假设

用户愿意为“少做解释、少切设备、灵感真正变成项目工作”付费，而不是为另一份视频摘要付费。

### 初始价格实验

- 测试价：USD 12/月。
- 示例额度：每月 30 次、每条视频不超过 5 分钟。
- 这是实验起点，不是已验证定价。
- 内容处理费与用户自身 agent 执行额度必须清楚拆分。

### 两周付费验证门槛

招募 10 名具有真实项目的目标用户：

- 至少 5 人第二周主动再次使用。
- 至少 3 人愿意按测试价格付费。
- 同时记录材料读取成功率、从分享至交付的时间、结果采纳率和用户补充解释次数。

这些是内部继续投入门槛，不是行业标准。

## 10. MVP 成功指标

| 指标 | 定义 | 初始目标 |
|---|---|---:|
| 分享接收成功率 | APP 正确收到目标来源 payload / 发起分享次数 | ≥ 95% |
| 素材可用率 | 取得 MVP 承诺内容 / 已接收任务 | 待 Spike A 定基线 |
| 正确投递率 | 在正确项目建立可追踪会话 / 素材就绪任务 | ≥ 95% |
| 重复执行率 | 同一任务被执行超过一次 / 已执行任务 | 0% |
| 报告证据覆盖率 | 含真实产物或无改动依据的终态报告 / 终态任务 | 100% |
| 第二周复用率 | 第二周再次主动分享的测试用户 / 测试用户 | ≥ 50% |
| 付费意愿 | 愿按测试价付费的测试用户 / 测试用户 | ≥ 30% |

## 11. 发布前开放问题

1. 后续实施目标已明确为 Android MVP；商业发行地区、渠道和正式签名仍待确定。这是原对话之后的决定，不倒填为原对话要求。
2. 后续实施目标点名 Instagram、X 等社交平台；首个重点验收来源及支持边界仍需用真实分享确认，不以相册文件测试替代。
3. 默认模式已由 [ADR 0011](../decisions/0011-relay-station-ux.md) 更新：新配对直接执行、已有设置保留，可改为先看计划；ADR 0009 的计划门禁与原目录执行边界继续有效。
4. 视频取得的授权、平台条款与商业使用边界是什么？
5. 材料默认保留多久？用户删除后备份多久内完成清除？
6. 单次命令手机审批已采纳，见 [ADR 0004](../decisions/0004-single-command-approval.md)；其他权限类型的手机审批范围仍待决定，真实验收另记。
