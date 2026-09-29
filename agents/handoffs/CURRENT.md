# Current Handoff

Updated 2026-09-29. Public self-hosted preview remains online; Android and Windows
0.5.1 are local UX candidates. **Do not claim stable/marketing readiness.**

## Current user scope

The user requested further UX polish, then a marketing-ready handoff and several
posters/assets after the UX loop is verified. They accepted a candidate without
a physical Android. Their latest explicit choice is **local UI validation only;
retain real submission acceptance**. Do not send tasks to a real Relay. No social
posting is authorized. New campaign assets remain deferred until the agreed gate.

## Completed this pass

- Added private DemoImportProvider and three ShareAttachmentTest cases covering
  exact bytes, original-source disappearance after a complete copy, second-file
  partial failure cleanup, recreation while copying and cleanup on normal Close.
  DemoShare now preserves incoming MIME and multiple-share action. Never submits.
- Actual API 35 post-copy process probe removed the provider source, killed the
  stopped app, restored task2326 with PID11494 changing to11581, and verified the
  copied file's SHA-256 unchanged. Normal Close removed this copy; no outbox write.
- Full regression exposed touch-mode viewport jumps in Home/history. Explicit
  touch/keyboard test variants reproduced both. Touch updates now capture record
  ID and padding-relative offset before replacing rows, then restore that record.
  Keyboard native stable-ID handling and Activity restoration retain precedence.
- Final current-source native suites: API35 and API26 each54/54. Two viewport
  cases at200% pass on both. Twelve JVM cases, debug lint and signed build pass.
  Refreshed signed candidate; same certificate. Hash/evidence in release record.
- Preserved initial failures: fixture MIME launch mismatch (explicitly stopped),
  touch jumps and insufficient native-only saved-state attempt; one total-read
  assertion (1 versus2) in file-preview restoration. That test now requires zero
  added reads across recreation, retaining path/Save assertions. Extra-read cause
  is not established; isolated and final full runs pass.
- API26 snapshot resumed with ADB offline; reconnect failed, orderly stop/cold
  boot retained data and restored connectivity. No new app ANR in final records.
  Preferences restored and devices stopped. No public/remote/Windows/campaign change.

Prior audit `2b385ac` maps original requirements to evidence in the release record
and verifies stopped text-editor process recovery. It does not close live gates.

Evidence and current artifact hash: [0.5.1 UX validation](../../docs/releases/0.5.1-ux.md).
Control/flow coverage and missing acceptance: [UI matrix](../../docs/releases/0.5.1-ui-matrix.md).
Do not duplicate detailed historical run results here. The current 54-case suites
include prior model/default focus, Home/history restoration, file-export recovery,
permission, retention, instance-isolation and scheduling cases. Those tests prove
only their local assertions, not real delivery or physical performance.

APK: `.local/releases/ux-polish-sep29/DropRun-0.5.1-android-candidate.apk`.
Windows installer/ZIP: `.local/releases/ux-actions-sep29-windows-final/`.
Current screenshots: `.local/ux-cloud-check/share-choice-api{26,35}-{light,dark}.png`
and `share-choice-api{26,35}-large-{light,dark}.png`. Text process probe uses
`process-share-{before,after,material}.xml` and `process-share-after.png`.
File process probe uses `attachment-process-{before,after}.xml` and
`attachment-process-after.png` in the same folder.
Synthetic fixtures are not
actual agent work. Node's preceding 198-case pass and Windows manifest evidence
remain in the release record; Node/Windows did not change this pass.

## Remaining gates and environment

- Real submission/acknowledgement/execution/report/follow-up remains intentionally
  unverified. Earlier automatic review rejected an ADB live share with only
  `blocked by policy`; the user then selected local-only verification. Do not
  retry that submission through another mechanism.
- Earlier isolated QA pairing succeeded. `.local/ux-cloud-check.mjs` and ignored
  state describe a temporary QA project/catalog addition. Remote cleanup remains
  pending. No task was sent. API 35 release package `app.droprun.mobile` remains
  paired and force-stopped; use `.debug` fixtures only. Private Connector unchanged.
- Physical Android/TalkBack/performance, 20 genuine source samples, five complete
  tasks over three projects, clean Windows/fresh Cloudflare installation, upgrade
  and uninstall acceptance remain open. See matrix for local coverage limitations.
- Original API 35 SyncJob ANR at 2026-09-29 18:10:25 remains unproven. Later clean
  runs do not establish its root cause. Earlier API 26 emulator crash/kernel panic,
  no-focused-window failures and stale-test installation are recorded, not erased.
- Dedicated emulators are stopped: API 35 DropRunTest/5556 restored font2.0,
  keep-awake3, auto-rotation1/user-rotation0; API 26 DropRunApi26/5558 restored font1.0,
  keep-awake0, auto-rotation1/user-rotation absent. Verify settings before next run.
  Never run another UI automation client during instrumentation. Wait for terminal
  build success before installing APKs, and require `OK (N tests)`, not exit0 alone.

## Public baseline (unchanged; evidence in 0.5.0 release record)

- Website: https://droprun.dengmaizi0802.chatgpt.site (`/zh/` for Chinese).
- Repository: https://github.com/MrMaii/droprun
- Prerelease: https://github.com/MrMaii/droprun/releases/tag/v0.5.0-rc.1
- Tagged source: `90718812d764c2d590e2d0db2d1381e2a2dfccaf`.
- Hosted CI: https://github.com/MrMaii/droprun/actions/runs/36290007838

GPTSites project `appgprj_6ab726bb8cd48191acd3234da262627d`, saved version2.
Do not duplicate it. Hosting uses generated landing/dist. Old Netlify untouched.
Windows has no trusted publisher signature. Signing material remains ignored.
Git author: Thomas Deng <150266369+MrMaii@users.noreply.github.com>.

## Single recommended next action

Validate local attachment integrity and recovery using synthetic files/providers,
specifically abrupt process termination during copying. Post-copy process recovery
and same-process Activity recreation are now covered, not that missing path.
One preexisting fixture-scope attachment remained before/after the manual probe
(`6d908a45-e442-428d-aac7-b9eace8e1705`); ownership is not established and it was
not deleted. Inspect receive/onSaveInstanceState and file ownership, then use a
controlled slow local source to reproduce partial-file cleanup/recovery. Do not
blindly sweep attachment directories. No real provider, submission or Codex run.

## Files involved

MainActivity, ProjectHistoryActivity, LocalRecoveryTest, ShareAttachmentTest,
DemoImportProvider, DemoShareActivity, debug manifest, PRD/CONTRACTS, UI matrix,
UX release record and this handoff. Refreshed ignored signed APK and local evidence.
