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

- Fixed the orphan reproduced in `ecf0173`. ShareImport now persists an atomic
  instance-scoped UUID journal before copying, validates source identity and
  completed-byte receipts, retains one operation across recreation, and overwrites
  only its own partial on process restoration. Close cannot delete another share
  or outbox-owned files. First pairing can adopt an unpaired draft; paired instances
  cannot exchange drafts. Contract updated with ownership and transfer rules.
- Outbox uses the receive UUID; restoration detects an already saved task. Both
  restoration and outbox cleanup handle a leftover journal after the save boundary.
  Completion callbacks are consumed once, protecting a restored note from a late
  duplicate callback. Legacy flat attachment paths remain supported.
- Ten native attachment cases now include copy reuse, interruption, recreation,
  journal recovery, source/instance isolation, outbox ownership, first pairing,
  byte modification, mid-copy close and independent drafts.
- Full native suites passed61/61 on both APIs. The final callback guard and direct
  receive-UUID assignment were then verified by ten focused cases on each API,
  with API35 at200%. JVM12, debug lint and signed build passed. Signed local APK
  refreshed; previous candidate preserved. Exact evidence/hash belongs in release record.
- Actual API35 process probe passed: task2766/PID16929 killed with first196608-byte
  copy complete and second4096-byte partial; first source deleted; PID17051 resumed
  the same directory, retained first bytes and finished second262144 bytes. Both
  hashes matched. Normal Close removed the entire owned folder; no manual attachment
  cleanup needed. Unknown older6d908a45-e442-428d-aac7-b9eace8e1705 untouched.
- Synthetic source/hold files removed; preferences restored; both emulators stopped.
  No live submission, remote mutation, public deployment or campaign assets.

Previous code pass `88ce635` fixed touch viewport jumps and added three attachment
cases. Its full native suites passed54/54 on both APIs, two viewport cases at200%
passed on each, and JVM12/debug lint/signed build passed. Initial viewport, fixture
launch and file-preview total-read assertion failures remain in the release record.

Prior audit `2b385ac` maps original requirements to evidence in the release record
and verifies stopped text-editor process recovery. It does not close live gates.

Evidence and current artifact hash: [0.5.1 UX validation](../../docs/releases/0.5.1-ux.md).
Control/flow coverage and missing acceptance: [UI matrix](../../docs/releases/0.5.1-ui-matrix.md).
Do not duplicate detailed historical run results here. The preceding 54-case suites
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
Interrupted-copy failing evidence remains `import-held-{before,restored}.xml` and
`import-interruption-observations.json`. Passing ownership evidence is
`import-owned-{before,restored,closed}.xml`, `import-owned-restored.png`, and
`import-ownership-observations.json` in the same local folder.
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

Complete the inbound receive-failure UX using the new ownership records. The
existing provider-failure path still deletes the batch and shows a fatal-dismiss
dialog; the original requirement asks for retained material plus visible retry
and cancel. Preserve verified complete copies when a later source fails, make the
failure state recoverable without duplicate writes, and show accurate source-loss
wording using private providers only. Examine abandoned journals whose Activity is
never restored before claiming the whole draft lifecycle is closed. Do not sweep
unknown files. The older6d908a45-e442-428d-aac7-b9eace8e1705 remains unowned by this
work. Real provider, remote submission and Codex execution remain out of scope.

## Files involved

ShareImport (new), ShareActivity, Store, ShareAttachmentTest, DemoImportProvider,
DemoShareActivity, CONTRACTS, UI matrix, UX release record and this handoff.
Ignored local probe XML/PNG/observations and refreshed signed candidate APK.
