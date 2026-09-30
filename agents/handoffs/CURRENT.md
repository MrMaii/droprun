# Current Handoff

Updated 2026-09-29. Public self-hosted preview remains online. Android/Windows
0.5.1 are local candidates; **stable release and marketing readiness are unproven**.

## Current user scope

Further UX polish, then marketing handoff and posters after the UX loop is verified.
Physical Android unavailable; candidate work accepted. Latest explicit choice:
**local UI validation only; retain real submission acceptance**. No real Relay
submissions, grants, cleanup, Codex execution or social posts. Do not retry the
previously blocked live share by another mechanism. Automatic review previously
reported only `blocked by policy`; the user chose local-only. Campaign assets stay
behind the agreed gate. Continue local work without requesting that permission again.

## Completed this pass

- Implemented discoverable unfinished shares, preserving material, note, project,
  model/effort and navigation state in the atomic import journal. Home/setup entry
  stays separate from history/outbox. Saving/saved/error feedback and retry added.
- Private recovery route, one active editor/copy, explicit discard, first-pairing
  target confirmation, instance isolation and late-write/transfer cleanup covered.
  System page destruction no longer implies the user chose to discard.
- Native200% review: recovery heading wraps, empty status uses no space, Home link
  aligns with cards. Actual API35 background process loss recovers through Home
  after the original synthetic file is removed; note/target/model/effort and exact
  retained67200-byte hash agree. Keep and confirmed Discard tested; no submission.
- JVM12/lint/debug/test/signed release build pass. All11 draft cases pass within both
  final full native runs. API26 complete84/84; API35 complete83/84 with one existing
  history burst/keyboard failure. Earlier83-case complete runs passed per API.
- Initial test harness launch-Intent, accessibility-window and teardown races were
  fixed; failed logs retained. Known test-created leftover files were inspected and
  removed by exact path. Unknown older material remains untouched.
- ADR0017, PRD, contracts, UI matrix and canonical UX record updated. No Node/Windows
  source changes or external mutations. New APK is a separate review artifact.

Canonical logs, timings, hashes and process evidence:
[0.5.1 UX record](../../docs/releases/0.5.1-ux.md).
Control coverage: [UI matrix](../../docs/releases/0.5.1-ui-matrix.md).

## New unresolved local regression

API35 complete run `.local/share-drafts-complete-api35.log` fails
`historyBackToBackUpdatesKeepTheVisibleRecord`, non-touch mode. Two pending inserts
before layout change visible history item170 to168 (IDs a991084d... to4f77a651...).
Full run83/84,137.207s. API26 full84/84,98.231s. All draft cases pass in those runs.
Isolated original history case1/1,5.28s; strengthened four touch/keyboard cycles
1/1,6.459s. Logs `share-drafts-history-isolated-api35.log` and
`share-drafts-history-stress-api35.log`. Those passes do not prove the bug fixed.
The stronger test is retained; no speculative production history change was made.

Likely area to inspect, not a proven diagnosis: non-touch native ListView stable-ID
synchronization around repeated adapter notifications before layout. Touch mode
already records the actual visible View's bound ID; keyboard mode relies on native
sync. Preserve both reading offset and keyboard selection/focus if changing it.
Avoid making the test wait for an expected row or weakening its assertion. Native
pre-draw synchronization already exists in `awaitFrame`; capture actual lifecycle,
window focus, selected ID, bound visible ID and pending layout to distinguish a
product anchor issue from a fixture/window transition. No external service needed.

## Artifacts and environment

- New review APK `.local/releases/ux-polish-sep29/DropRun-0.5.1-share-drafts-review.apk`:
  SHA256 `27b3de21eec09e69687ff47faf57fa271abf0defad4e4c917ca61ffcd2d0044d`.
  Version0.5.1/code14; package `app.droprun.mobile`; stable certificate unchanged.
  Recovery Activities are non-exported; no Demo fixture in release manifest.
- Named candidate remains `.local/releases/ux-polish-sep29/DropRun-0.5.1-android-candidate.apk`:
  SHA256 `d2c8084e7b76dbda8f173f9777498f90023c755f378e68b43e710ca6e65e3b51`.
  Not overwritten because complete API35 regression is unresolved.
- Windows unchanged `.local/releases/ux-actions-sep29-windows-final/`, no trusted
  publisher signature. Signing material and local runtime data remain ignored.
- Native screenshots/XML `.local/ux-cloud-check/draft-{home,list,resumed}-final.*`,
  `draft-confirm-final.xml`; synthetic private-provider data, no genuine delivery.
- Both emulators stopped. Restored API35 DropRunTest/5556 font2.0, keep-awake3,
  auto-rotation1/user0; API26 DropRunApi26/5558 font1.0, keep-awake0,
  auto-rotation1/user absent. Verify before changes. Debug outboxes/journals empty;
  API35 older unknown6d908a45-e442-428d-aac7-b9eace8e1705 remains.
- Public package on API35 stays paired and force-stopped. Use `.debug` only. Wait
  for terminal Gradle build before install. Never automate an emulator concurrently
  with instrumentation. Require `OK (N tests)`, not merely shell exit0.
- No running tests/builds remain. Last build after signed release changes tests
  only; the separate review APK includes all current production source.

## Remaining gates

- Real submission/receipt/execution/report/follow-up intentionally unverified.
  Earlier isolated QA pairing/catalog mutation is described in ignored
  `.local/ux-cloud-check.mjs`; remote cleanup is not authorized now. No task sent;
  private Connector untouched. Do not mutate the Relay under local-only scope.
- Physical Android/TalkBack/performance,20 genuine sources,five full tasks over
  three projects,clean Windows/fresh Cloudflare installation,upgrade/uninstall.
  Emulator test duration does not establish physical-device frame/press performance.
- Real provider URI loss and storage-full/maximum-size still need bounded evidence.
  New draft-write failure is injected, not a claim of actual full-device storage.
- Original API35 SyncJob ANR(2026-09-29 18:10:25) root cause remains unproven;
  earlier emulator crashes/test failures remain in the canonical release record.

## Public baseline (unchanged; historical evidence)

Website https://droprun.dengmaizi0802.chatgpt.site (`/zh/`).
Repository https://github.com/MrMaii/droprun .
Prerelease https://github.com/MrMaii/droprun/releases/tag/v0.5.0-rc.1 .
Tagged source `90718812d764c2d590e2d0db2d1381e2a2dfccaf`.
CI https://github.com/MrMaii/droprun/actions/runs/36290007838 .
GPTSites project `appgprj_6ab726bb8cd48191acd3234da262627d`, version2;
do not duplicate. Hosting uses generated landing/dist; old Netlify untouched.
Git author Thomas Deng <150266369+MrMaii@users.noreply.github.com>.

## Single recommended next action

Diagnose and resolve the intermittent API35 keyboard history jump described above,
using local fixtures. Preserve selection and reading position; rerun the affected
history/Home/keyboard cases on both APIs, then refresh the named candidate only
when relevant verification is clean. User authorization remains local-only.

## Files involved

ShareImport, ShareActivity, ShareDrafts, ShareDraftsActivity, RecoveredShareActivity,
MainActivity, PairActivity, Store, manifest, DemoShareActivity; LocalRecoveryTest,
ShareAttachmentTest, ShareDraftRecoveryTest; ADR0017/index, PRD, CONTRACTS, UI matrix,
UX release record and this handoff. Ignored logs/screenshots/review APK. Previous
5016695 short-landscape and fa1c8bf history fixes remain intact.
