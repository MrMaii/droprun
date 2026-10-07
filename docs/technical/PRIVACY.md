# Data and privacy / 数据与隐私

DropRun is self-hosted. The project maintainers do not operate an official Relay,
receive your shared materials, or hold your Codex credentials. The public website
provides information and release links; it does not receive task submissions.
Its hosting provider may process ordinary HTTP request logs.

## Where data goes

| Data | Location |
| --- | --- |
| Local Codex work and login | Your Windows computer |
| Pending shares and phone token | Your Android app's private storage |
| Submitted material, task metadata and reports | Your Cloudflare instance |
| Screenshots, files and supported static snapshots | Your private R2 storage |
| Optional transcription input | The provider configured by you |
| Optional live preview | Your computer through your configured tunnel |

Codex works in your local project. Submitted material, reports and uploaded
artifacts pass through your own Cloudflare; reports or artifacts may include code.

Shared URLs may be fetched from their source providers. Those providers' access
conditions still apply. Material content is untrusted input to the coding agent.

## Retention and deletion

The public profile defaults to deleting original uploads seven days after a task
ends and screenshots/artifacts after thirty days. Reports remain until deletion.
Check the instance's actual configuration before relying on those defaults.
Expired objects may remain until the next cleanup run. Deleting a cloud task is
not deletion of its original project files, local Codex conversation, source
provider's copy or backups you keep. Revocation blocks future device access;
cancelling a running task does not undo changes already made.

Preview links are access credentials. Do not post them publicly unless you intend
to share their contents. Revoke links and remove source material before sharing
diagnostics. Public issues must not contain pairing codes, tokens or private paths.

## 中文说明

DropRun 由你自行部署。维护者不运营官方中转、不接收分享材料，也不保存 Codex
登录凭证。官网只提供说明与下载链接；托管平台可能处理普通访问日志。

Codex 在电脑上的项目中工作，登录信息保留本机；手机本地保存待发送内容。
提交材料、报告与上传产物经过你自己的 Cloudflare，报告或产物可能包含代码。
可选转写和实时预览使用你配置的服务。

默认原始上传在任务终止七天后清理，截图和产物三十天后清理，报告保留至删除。
实际配置与清理执行时间以你的实例为准。云端删除不会删除原项目、Codex 会话、
来源网站副本或你自己的备份。预览链接应视为访问凭证，诊断与公开反馈务必脱敏。
