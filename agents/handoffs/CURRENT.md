# Current Handoff

Updated 2026-09-26. Task: deliver the public self-hosted DropRun candidate.
The user confirmed no physical Android is available; finish the candidate and
preserve real-device acceptance gaps. The approved full plan remains active.

## Implemented

- Android project home/history, consolidated settings, light/dark, EN/ZH,
  instance-scoped Keystore pairing. Native Java Views retained.
- Relay root-task statistics, pagination, fresh database baseline, separate
  preview capabilities and retention.
- Windows self-host setup, DPAPI, explicit Cloudflare resources, Codex
  account/project diagnostics, portable and per-user installer packaging.
- Bilingual landing source, README, Apache license, contribution/security docs,
  CI, self-hosting guides and launch copy. No social posts sent.

## Evidence

[0.5.0 release evidence](../../docs/releases/0.5.0.md) is the canonical record.
Latest Node suite: **188/188 passed**. npm audit: **0 vulnerabilities**.
Signed Android APK built and signature verified. Local artifacts:

- `.local/releases/DropRun-0.5.0-android.apk`
- `.local/releases/windows-rc1/DropRun-0.5.0-windows-x64.zip`
- `.local/releases/windows-rc1/DropRun-0.5.0-windows-x64-setup.exe`

Cloud synthetic checks passed, fixtures cleaned up; no actual Codex run claimed.
Android unit/build/lint checks passed before final signed build; lint warnings
remain. Signing material is local only and must never be published.

## Incomplete / blockers

- Source still untracked; no first Git commit. Curate staging, scan and manually
  review privacy before publication. Approved author: Thomas Deng,
  150266369+MrMaii@users.noreply.github.com.
- Public source/release and GPTSites deployment incomplete. Existing private
  service and old Netlify site untouched.
- Registered Site: `appgprj_6ab726bb8cd48191acd3234da262627d`.
  Reuse `landing/.openai/hosting.json`; do not register a duplicate. Obtain a fresh
  write credential in session memory for publishing.
- App screenshots/GIF/short and long video pending. Debug-only demo activities
  are labelled synthetic data, never evidence of genuine Codex execution.
- Dedicated emulator `DropRunTest`, port 5556, exited during resumed capture.
- No physical Android, clean Windows install, 20 real-source samples or five
  genuine complete tasks across three projects verified.
- Parallel agents hit account usage limits. Their files remain; inspect last
  polish changes rather than assuming all review feedback was resolved.
- ROADMAP and old document references still need public-release reconciliation.

## Single recommended next action

Complete candidate integration: verify packaged Windows installation and signed
Android runtime, capture labelled app media, then curate/publish source and
prerelease assets and deploy the registered Site. Do not claim stable or online
until the corresponding checks and publishing steps pass.

## Files involved

`android/`, `connector/`, `relay/`, `installer/`, `scripts/`, `tests/`, `landing/`,
`.github/`, package manifests, root open-source files, product/technical/decision/
launch/release documentation. Latest status update modified this handoff and
0.5.0 release evidence; test/build outputs remain in ignored local folders.
