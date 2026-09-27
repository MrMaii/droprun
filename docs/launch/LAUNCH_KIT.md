# DropRun launch kit

Status: prepared copy for the public preview. Verify the release links and current
evidence before posting. Nothing in this file authorizes posting to social media.

## Message

**English:** Share inspiration from your phone. Give your local Codex a next step.

**中文：** 你转发，它开工。把手机里的灵感，交给已有项目。

Short description: DropRun is an open-source Android and Windows tool that turns
a shared reference into a handoff to an existing local Codex project. Operate your
own Cloudflare Relay, choose project access, and inspect reports and deliverables.

中文简介：DropRun 是开源的 Android 与 Windows 交办工具。把参考分享到已有的
Codex 项目，由本地 agent 判断和工作，手机查看报告与交付。Relay 部署在你自己的
Cloudflare；电脑离线时排队，材料读取范围明确说明。

## Launch article

### English

I kept saving UI references on my phone and never bringing them back into the
projects I was building. Copying a link was easy. Finding the project, explaining
why the reference mattered, and checking the result was the part I put off.

DropRun makes that handoff smaller: share to the Android app, choose an existing
project, leave an optional note, and continue what you were doing. The Windows
Connector works with your local Codex. The phone shows what material was actually
read, what changed and what was verified.

This is an open-source, self-hosted public preview, not a hosted coding service.
Your code and Codex login stay on your computer. Your own Cloudflare instance
handles transport and stored deliverables. Provider usage can still cost money.

The preview focuses on project-grouped history, a short share flow, explicit
approval boundaries and inspectable results. It cannot promise access to every
video platform, and execution waits for your computer to be online. Screenshots,
files and supported static previews are included; live previews need your setup.

Try it on a small project first. The release notes list what we tested and what
still needs real-device and source-access validation. Specific installation and
handoff feedback is more useful than a star alone.

### 中文

我经常在手机上收藏网页和交互参考，却迟迟没有把它用到手头项目里。复制链接很
简单，找到项目、重新解释背景、再追踪结果，才是一直被推迟的那部分。

DropRun 把这次交接缩短：分享到 Android App，选已有项目，可选留一句话，然后
继续刚才的事。Windows Connector 使用本地 Codex，手机呈现实际读到了什么、改了
什么、如何验证。

这是开源、自部署的公开预览版。代码与 Codex 登录留在你的电脑，Cloudflare 中转
和存储由你自己的账号提供，不把开源等同于无限免费的云资源。

这一版重点是按项目归类的交办记录、轻量分享、明确授权和可检查的交付。不是
所有视频都能读取；电脑在线后才能执行。截图、文件和支持的静态预览是基础能力，
实时预览需要额外配置。请先用小项目尝试，安装和真实交办反馈最有价值。

## Channel copy

| Channel | Title / opening | Attachment and next action |
| --- | --- | --- |
| Show HN | Show HN: DropRun – share phone references into local Codex projects | GitHub, 30-second real UI clip; explain self-hosting and preview limits in first comment |
| Product Hunt | Give inspiration a next step | Three actual screens, subtitled walkthrough, self-hosting prerequisites, no unsupported performance claims |
| X | I built a small bridge between the ideas I save on my phone and the projects I build with Codex. DropRun is open source: share, pick a project, inspect what changed. | Short clip; second post explains Android/Windows and own Cloudflare |
| V2EX | 做了一个开源工具：把手机参考交给本地 Codex 项目 | 中文安装步骤、真实截图、读取限制、费用归属和待验证项 |
| GitHub Release | DropRun 0.5.0 public preview | Exact artifact names/hashes, migration notes, tests, remaining gates |

## Video shot lists

**15–30 seconds:** 0–4s premise; 4–10s real project home; 10–18s actual share sheet
and saved state; 18–24s inspect a result; 24–30s self-hosting and repository. If a
screen uses synthetic data, label it. Never splice a fixture into a claimed live
end-to-end task. A visual UI tour is not a successful source extraction demo.

**60–90 seconds:** 0–10s problem; 10–25s requirements and pairing; 25–45s actual
handoff; 45–65s result/evidence; 65–80s settings/privacy; final install link and
preview limitation. Record a genuine complete task before advertising this as
an end-to-end demo. Provide English and Chinese captions, no invented narration.

## Rollout checklist

- Before launch: anonymous download, hashes, independent installation, image and
  clip privacy review, known-limitations list, security-report channel.
- Launch day: GitHub release and website first; verify links; then publish only
  to explicitly authorized accounts/channels.
- Following week: triage reproducible reports, label device/source failures,
  publish fixes with regression evidence and update both languages together.

## Measurement without collecting private material

Use opt-in feedback or locally exported counters, not raw tasks uploaded to the
maintainers. Installation completion = reached paired/ready divided by started
setups with explicit consent to reporting. First-handoff success = first tasks
with a verified outcome divided by submitted first tasks. Record failure stage,
versions and source type, never private source content. Report sample size and
period; no growth/success numbers until observed.

## Delivered media and copy

- [English posts](POSTS.en.md), [中文发布文案](POSTS.zh-CN.md).
- `media/demo-en.mp4`, `media/demo-zh.mp4`: 24-second actual UI tour, burned captions.
- `media/walkthrough-en.mp4`, `media/walkthrough-zh.mp4`: 64-second overview,
  actual UI recording followed by an actual settings still; burned captions.
- `media/social-wide.png` (1200×630), `social-portrait.png` (1080×1350),
  `social-square.png` (1200×1200), with editable SVG sources alongside them.
- `../../landing/media/demo.gif`: README animation. Uncaptioned video and VTT
  tracks live in the same website media folder.

All visible task/project examples are labelled demonstration data. No Codex task
was run for these videos. Their editorial pacing is not an Android performance
measurement. Promote this as a public preview, with the linked validation gaps.
