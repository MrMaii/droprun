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

- Added a debug-only `/held-input` source. It writes4096 bytes, then waits while
  cache/synthetic-import.hold exists. This gate survives the receiving process,
  allowing the host to kill it and release the next process without fake lifecycle.
- Reproduced a real orphan: PID15049 was killed while stopped mid-copy; original
  task2642 resumed in PID15246 and completed a new262144-byte file. The old4096-byte
  copy persisted. Normal Close removed only the full copy. This remains UNFIXED.
- The receive path assigns attachment metadata only after all copying finishes;
  saved Activity state omits attachments while receiving. The partial file has no
  restored cleanup owner. Do not call this a passing interruption case.
- Recorded XML and structured observations. Removed only this probe's known
  partial64d71ad9-4c1a-4bb0-b6ae-016ba3fafd1e and synthetic source after recording.
  The older unknown file remained untouched. Manual cleanup is not a product fix.
- Debug/instrumentation build passed; three existing attachment cases pass5.974s.
  No production source or signed APK changed. No full54-case rerun for this small
  fixture extension; previous full results belong to the preceding code pass.
- No preference changes, real submission, public deployment or campaign assets.
  Debug app stopped, emulator shut down; no running workers/markers remain.

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
Interrupted-copy evidence: `import-held-{before,restored}.xml` and
`import-interruption-observations.json`. The latter explicitly records failure.
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

Fix the confirmed interrupted-copy orphan with explicit per-share file ownership
that survives process recovery. Preserve completed copies, validate instance/source
identity, distinguish partial from completed bytes, and prevent a closing old
Activity from deleting another Activity's adopted copy or an outbox-owned file.
Inspect receiver lifecycle before choosing the smallest implementation; blindly
sweeping attachments or merely moving the orphan into cache is not the fix.
Re-run the held-source process probe and the three existing attachment cases.
Older unknown6d908a45-e442-428d-aac7-b9eace8e1705 remains; do not delete it without
ownership evidence. No real provider, remote submission or Codex execution.

## Files involved

DemoImportProvider, UI matrix, UX release record and this handoff. Ignored local
process-probe XML/observations. Production code and signed APK remain unchanged.
