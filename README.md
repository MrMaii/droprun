<div align="center">
  <img src="assets/brand/mark.png" width="96" alt="DropRun">
  <h1>DropRun</h1>
  <p><strong>Share from your phone. Put local Codex to work.</strong></p>
  <p>Choose an existing project. Add an optional note. Inspect the result.<br>Android 8+ · Windows x64 · signed-in Codex · your Cloudflare</p>
  <p><a href="docs/technical/SELF_HOSTING.md"><strong>Get started →</strong></a></p>
  <p><a href="https://droprun.dengmaizi0802.chatgpt.site">Website</a> · <a href="https://github.com/MrMaii/droprun/releases/tag/v0.5.8-rc.1">Preview downloads</a> · <a href="README.zh-CN.md">简体中文</a></p>
</div>

> **Public preview · 0.5.8-rc.1.** [Validation record and open acceptance gates](docs/releases/0.5.8-rc.1.md).

Codex works on your Windows computer. You deploy the Relay to your own Cloudflare account.
DropRun brings progress, reports and evidence back to your phone.

## Inside the share sheet

Current native development UI · October 6, 2026 · memory-only samples.
The project is preset; the optional note is empty. No task is sent.

<p align="center"><picture><source media="(prefers-reduced-motion: reduce) and (prefers-color-scheme: dark)" srcset="assets/brand/source-share-material-en-dark-20261006.png"><source media="(prefers-reduced-motion: reduce)" srcset="assets/brand/source-share-material-en-light-20261006.png"><img src="assets/brand/native-share-en-20261006.gif" width="320" alt="Native Android share sheet: inspect received sample material, select another sample model, view its default Thorough effort and collapse the options; visible memory-only marker, no task sent"></picture></p>

[8.53-second original recording](landing/media/native-share-en-20261006.mp4) · [Captions](landing/media/native-share-en-20261006.vtt)

[Light still](assets/brand/source-share-material-en-light-20261006.png) · [Dark still](assets/brand/source-share-material-en-dark-20261006.png). Reduced motion shows a static image.
The GIF is a 25 fps sampled display at native 320 × 640; the MP4 keeps the original timeline.

<details>
<summary><strong>Earlier UI tours and longer walkthrough</strong></summary>

The October 5 tours use the 0.5.1 development UI with labelled demonstration data.
They show interface navigation, not a completed source-to-Codex task.

[Earlier 9-second tour](https://droprun.dengmaizi0802.chatgpt.site/media/ui-tour-en.mp4) · [24-second video](https://droprun.dengmaizi0802.chatgpt.site/media/demo.mp4) · [64-second walkthrough](https://droprun.dengmaizi0802.chatgpt.site/media/walkthrough.mp4)

</details>

**[Set up DropRun →](docs/technical/SELF_HOSTING.md)**

## Share. Follow. Inspect.

Current screenshots and the Share recording use memory-only native samples
(October 6, 2026). Earlier tours retain their original source dates. These show the interface only, not the signed download or a
real Codex task. [Capture notes](assets/brand/readme-media.md).

### 01 · Share a reference

After choosing an existing project, add a note when you have a specific change in mind.

<p align="center"><picture><source media="(prefers-color-scheme: dark)" srcset="assets/brand/source-share-material-en-dark-20261006.png"><img src="assets/brand/source-share-material-en-light-20261006.png" width="280" alt="Native Android note step with expandable received material, optional intent and Model & effort controls; memory-only probe marker retained"></picture></p>

### 02 · Follow local Codex

Codex works on your computer, in the chosen project. Follow progress and decisions in its history.

<p align="center"><picture><source media="(prefers-color-scheme: dark)" srcset="assets/brand/source-home-en-dark-20261006.png"><img src="assets/brand/source-home-en-light-20261006.png" width="280" alt="Native Android project home with names, task and dispatch counts aligned at the card edge; memory-only and sample-project markers retained"></picture></p>

### 03 · Inspect the delivery

See the result, required actions, screenshots and files. Open the full report when needed.

<p align="center"><picture><source media="(prefers-color-scheme: dark)" srcset="assets/brand/source-delivery-stable-en-dark-20261006.png"><img src="assets/brand/source-delivery-stable-en-light-20261006.png" width="280" alt="Native handoff page with result, fixed-snapshot explanation and delivery actions; visible memory-only marker"></picture></p>

## Start with your own setup

| Computer | Phone | Relay |
| --- | --- | --- |
| Windows x64, Git, signed-in Codex, Chrome or Edge | Android 8 or newer | Your Cloudflare account with Workers, D1 and R2 |

1. **Install on Windows.** Get the [installer](https://github.com/MrMaii/droprun/releases/download/v0.5.8-rc.1/DropRun-0.5.8-windows-x64-setup.exe) or [portable ZIP](https://github.com/MrMaii/droprun/releases/download/v0.5.8-rc.1/DropRun-0.5.8-windows-x64.zip).
   For the portable edition, extract to a permanent folder and open `DropRun.cmd`.
2. **Set up your Relay.** Open the local browser guide, check prerequisites and
   deploy to your own Cloudflare account.
3. **Pair your phone.** Install the [Android APK](https://github.com/MrMaii/droprun/releases/download/v0.5.8-rc.1/DropRun-0.5.8-android.apk), scan the computer's code, confirm the server and authorize projects.
4. **Make a handoff.** Share a reference, then follow receipt, execution and
   approval through to the report. “Saved” means saved on your phone, not finished.

**[Complete installation, recovery and upgrade guide →](docs/technical/SELF_HOSTING.md)**

DropRun has no official account or subscription. Cloudflare, optional
transcription and Codex usage belong to your accounts and **may incur charges**.
Windows packages are unsigned and may show an unknown-publisher prompt; compare
the release [checksums](https://github.com/MrMaii/droprun/releases/download/v0.5.8-rc.1/SHA256SUMS.txt).
The public Android app installs alongside the historical private/debug app;
different signing identities cannot silently replace each other.

## Made for a small handoff

- **Keep the intent.** Your note leads the task. Each independent share starts a
  new Codex conversation; a follow-up continues the same one.
- **Stay oriented.** Home shows only projects with handoff history or locally saved shares.
  Tasks count independent requests; dispatches count accepted shares and follow-ups.
  Network retries add neither.
- **Decide how work starts.** Choose direct execution or plan review. Project
  permissions and approval for high-risk actions remain separate.
- **Inspect what happened.** Reports distinguish actual source coverage, project
  changes and verification. Delivery can include screenshots, files and supported
  static snapshots; live preview is optional.

## Your phone. Your computer. Your Relay.

<p align="center"><picture><source media="(prefers-color-scheme: dark)" srcset="assets/brand/readme-architecture-dark.svg"><img src="assets/brand/readme-architecture.svg" width="400" alt="Self-hosted architecture: Android connects through your Cloudflare Relay and Windows Connector to local Codex; project code and Codex login stay on your computer."></picture></p>

References and notes: Android → your Relay → Windows Connector → local Codex.
Progress, reports and delivery files: Connector → Relay → Android.

Project code and Codex login stay on your computer. Shared material, reports and
uploaded artifacts pass through your own Cloudflare instance. Each Relay belongs
to one owner and one Connector; multiple phones can pair with it. The public
website provides information and downloads. [Data and privacy →](docs/technical/PRIVACY.md)

## Know the boundaries

- **An online computer does the work.** Accepted handoffs queue while it is away.
- **Source access varies.** A video URL, cover or transcript does not establish
  full video coverage. Read the report's actual extraction scope.
- **Android controls background delivery.** Notifications are not an unlimited
  always-on push guarantee.
- **Preview depends on the project.** Static snapshots need compatible exports.
  Live preview needs your own domain, managed tunnel and online computer.
- **Cancellation is not a rollback.** Cancelling a task or deleting its cloud
  record does not undo changes already made to your project.

This is a release candidate. Physical Android performance, clean Windows
installation and the full real-source handoff workflow still have open acceptance
gates. See the [dated validation record](docs/releases/0.5.8-rc.1.md) before using
it on important projects. Screenshots and a UI tour do not establish those results.

## Build and contribute

Start with Node.js 24 and Git. Android development also needs JDK 17+ and Android
SDK 35; the [development guide](docs/technical/DEVELOPMENT.md) covers builds and tools.

```sh
npm ci
npm test
node scripts/setup.mjs doctor
node scripts/setup.mjs open
```

[Architecture](docs/technical/ARCHITECTURE.md) · [Contracts](docs/technical/CONTRACTS.md)
· [Contributing](CONTRIBUTING.md) · [Security](SECURITY.md)

Source: [Apache-2.0](LICENSE). Bundled and optional dependencies retain their
own [licenses](THIRD_PARTY_NOTICES.md).
