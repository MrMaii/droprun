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

- Added inbound receive-failure recovery: retain verified copies, remove partial
  or corrupt copies, persist a bounded failure kind/index, and restore without
  automatically retrying providers. Explicit retry copies only missing material
  and rejects repeat taps. Missing receipt fields cannot claim a copy is ready.
- Failure overlay shows cause before the file list, kept count and file states.
  Retry/discard stay below scrolling content; retry disables with immediate status.
  Discard asks for confirmation; Keep retains materials. Incomplete batches cannot
  submit. PRD and CONTRACTS updated. No abandoned-draft recovery screen yet.
- Fourteen native attachment cases pass on both APIs at200%. The subsequent
  reason-first layout change passes its focused visibility/confirmation case on
  both. Visibility waits for actual sheet alpha1/translation0 and allows outward
  integer mapping; earlier assertion failures remain recorded, not erased.
- Actual API35 failure process probe passed: task2777/PID17907 killed after first
  196608-byte copy and second-source failure. First original deleted; second source
  restored before PID18058 resumed. Failure persisted without auto-retry; explicit
  Retry completed the second262144 bytes, retained the first and matched both hashes.
  Normal Close cleaned own folder; outbox stayed empty and unknown older file stayed.
- A broader run found API35 history shift(item170 to169) and pending removal still
  present after3s. Isolated history passed, but pending removal failed again. These
  remain UNFIXED. Initial API26 pre-footer suite passed64/64; later full runs are
  not green. Do not represent the attachment subset as full acceptance.
- JVM12, lint, final signed build passed. New APK is a separate review build;
  named candidate remains `ffdfe7e`'s artifact until regression investigation.
  System preferences restored, synthetic sources/markers removed, both emulators
  stopped. No live submission, remote mutation, public deployment or campaign assets.

Evidence and current artifact hash: [0.5.1 UX validation](../../docs/releases/0.5.1-ux.md).
Control/flow coverage and missing acceptance: [UI matrix](../../docs/releases/0.5.1-ui-matrix.md).
The record retains preceding viewport, model/focus, file-export and ownership
results, including failed attempts. Local assertions do not prove real delivery
or physical performance; earlier green runs do not erase the current regression.

APK: `.local/releases/ux-polish-sep29/DropRun-0.5.1-android-candidate.apk`.
New review only: `.local/releases/ux-polish-sep29/DropRun-0.5.1-import-retry-review.apk`.
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
Latest failure probe: `import-retry-{before,restored,actions,success,closed}.xml`,
`import-retry-observations.json`. UI: `import-retry-footer.png` (EN200%) and
`import-retry-final-zh.png` (Chinese dark, cause-first final layout).
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

Fix the repeated local pending-removal failure before promoting the review APK.
ProjectHistoryActivity.load and removeSaved share a single executor; a blocked
history request may queue local removal. Reproduce deterministically with local
fixtures, inspect actual thread/queue state, then make removal responsive with
honest progress. Do not just increase the test's3s timeout. Also retain the one-row
history shift from the full run: a second render before layout may read an anchor
from already-replaced rows, but this is only a hypothesis; its isolated case passed.
Evidence is in `import-retry-api35-final.log` and `import-retry-history-isolated.log`.
After these regressions, examine never-restored draft ownership and short landscape.
Do not sweep unknown6d908a45-e442-428d-aac7-b9eace8e1705. No real provider, Relay task
or Codex execution; current authorized validation remains local-only.

## Files involved

ShareImport, ShareActivity, ShareAttachmentTest, DemoImportProvider,
DemoShareActivity, PRD, CONTRACTS, UI matrix, UX release record and this handoff.
Ignored local probe XML/PNG/observations and a separate signed review APK.
