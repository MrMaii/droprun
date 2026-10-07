<div align="center">
  <img src="assets/brand/mark.png" width="96" alt="DropRun">
  <p><strong>DropRun</strong> · Android + Windows · Self-hosted</p>
  <h1>Share from your phone.<br>Put local Codex to work.</h1>
  <p>Share a link, text or file to an existing project. Add an optional note; inspect the result and evidence on your phone.</p>
  <p><a href="docs/technical/SELF_HOSTING.md"><strong>Start installing →</strong></a></p>
  <p>Android 8+ · Windows x64 · signed-in Codex · your Cloudflare</p>
  <p><a href="https://droprun.dengmaizi0802.chatgpt.site">Website</a> · <a href="https://github.com/MrMaii/droprun/releases/tag/v0.5.16-rc.1">Preview downloads</a> · <a href="README.zh-CN.md">简体中文</a></p>
</div>

> **Public preview · 0.5.16-rc.1.** [Validation record and open acceptance gates](docs/releases/0.5.16-rc.1.md).

Codex works on your Windows computer. You deploy the Relay to your own Cloudflare account.

## Share. Follow. Inspect.

1. **Share → DropRun.** In another Android app, choose an existing project and add an optional note.
2. **Follow local Codex.** Work happens on your computer, in that project. Follow progress and decisions in its history.
3. **Inspect the delivery.** Check the result, required actions, screenshots and files; open the full report when needed.

Screenshots and recording show separately captured, labelled memory-only development samples, not a genuine task or the signed download. [Media sources and recording scope](assets/brand/readme-media.md).

<p align="center">
  <picture><source media="(prefers-color-scheme: dark)" srcset="assets/brand/source-share-count-en-dark-20261006.png"><img src="assets/brand/source-share-count-en-light-20261006.png" width="260" alt="01 · Share: project and received material together, a persistent optional-note label, received file counts and model controls; native memory-only probe marker retained"></picture>
  <picture><source media="(prefers-color-scheme: dark)" srcset="assets/brand/source-home-typography-en-dark-20261007.png"><img src="assets/brand/source-home-typography-en-light-20261007.png" width="260" alt="02 · Follow: native Android project cards with larger complete names, task and dispatch counts, status and recency; memory-only and sample-project markers retained"></picture>
  <picture><source media="(prefers-color-scheme: dark)" srcset="assets/brand/source-delivery-followup-en-dark-20261007.png"><img src="assets/brand/source-delivery-followup-en-light-20261007.png" width="260" alt="03 · Inspect: native handoff page with a prominent result, grouped preview and files, and an outlined follow-up button; visible memory-only marker"></picture>
</p>

<p align="center"><strong>01 · Share a reference</strong> &nbsp; · &nbsp; <strong>02 · Follow the project</strong> &nbsp; · &nbsp; <strong>03 · Inspect the result</strong></p>

Tasks count independent requests; dispatches count accepted shares and follow-ups.
Network retries add neither.

For example, share a design reference to an existing app project with this note:

> Adapt this navigation idea to our settings screen. Keep our existing visual style.

<details>
<summary><strong>Watch the sharing interaction</strong></summary>

Choose a different project, then switch from Thorough to Balanced before sending.

<p align="center"><picture><source media="(prefers-reduced-motion: reduce) and (prefers-color-scheme: dark)" srcset="assets/brand/source-share-count-en-dark-20261006.png"><source media="(prefers-reduced-motion: reduce)" srcset="assets/brand/source-share-count-en-light-20261006.png"><img src="assets/brand/native-share-touch-en-20261007.gif" width="320" alt="Native Android memory-only full-timeline tour: choose a project, return and switch to a different project, change Thorough to Balanced and collapse; no task sent"></picture></p>

[Original recording](landing/media/native-share-touch-en-20261007.mp4) · [Captions](landing/media/native-share-touch-en-20261007.vtt)

</details>

**[Set up DropRun →](docs/technical/SELF_HOSTING.md)**

## Start with your own setup

| Computer | Phone | Relay |
| --- | --- | --- |
| Windows x64, Git, signed-in Codex, Chrome or Edge | Android 8 or newer | Your Cloudflare account with Workers, D1 and R2 |

1. **Install on Windows.** Get the [installer](https://github.com/MrMaii/droprun/releases/download/v0.5.16-rc.1/DropRun-0.5.16-windows-x64-setup.exe) or [portable ZIP](https://github.com/MrMaii/droprun/releases/download/v0.5.16-rc.1/DropRun-0.5.16-windows-x64.zip).
   For the portable edition, extract to a permanent folder and open `DropRun.cmd`.
2. **Set up your Relay.** Open the local browser guide, check prerequisites and
   deploy to your own Cloudflare account.
3. **Pair your phone.** Install the [Android APK](https://github.com/MrMaii/droprun/releases/download/v0.5.16-rc.1/DropRun-0.5.16-android.apk), scan the computer's code, confirm the server and authorize projects.
4. **Make a handoff.** In another Android app, tap **Share → DropRun** and choose
   a project. Follow receipt, execution and
   approval through to the report. “Saved” means saved on your phone, not finished.

**[Complete installation, recovery and upgrade guide →](docs/technical/SELF_HOSTING.md)**

DropRun has no official account or subscription. Cloudflare, optional
transcription and Codex usage belong to your accounts and **may incur charges**.
Windows packages are unsigned and may show an unknown-publisher prompt; compare
the release [checksums](https://github.com/MrMaii/droprun/releases/download/v0.5.16-rc.1/SHA256SUMS.txt).
The public Android app installs alongside the historical private/debug app;
different signing identities cannot silently replace each other.

## Made for a small handoff

- **Keep the intent.** Your note leads the task. Each independent share starts a
  new Codex conversation; a follow-up continues the same one.
- **Stay oriented.** Home shows only projects with handoff history or locally saved shares.
- **Decide how work starts.** Choose direct execution or plan review. Project
  permissions and approval for high-risk actions remain separate.
- **Inspect what happened.** Reports distinguish actual source coverage, project
  changes and verification. Delivery can include screenshots, files and supported
  static snapshots; live preview is optional.

## Your phone. Your computer. Your Relay.

<p align="center"><picture><source media="(prefers-color-scheme: dark)" srcset="assets/brand/readme-architecture-dark.svg"><img src="assets/brand/readme-architecture.svg" width="400" alt="Self-hosted architecture: Android connects through your Cloudflare Relay and Windows Connector to local Codex; Codex works in your local project and its login stays on your computer. Submitted material, reports and uploaded artifacts pass through your own Cloudflare; reports or artifacts may include code."></picture></p>

References and notes: Android → your Relay → Windows Connector → local Codex.
Progress, reports and delivery files: Connector → Relay → Android.

Codex works in the project on your computer, and its login stays there. Submitted
material, reports and uploaded artifacts pass through your own Cloudflare instance;
reports or artifacts may include code. Each Relay belongs
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
gates. See the [dated validation record](docs/releases/0.5.16-rc.1.md) before using
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
