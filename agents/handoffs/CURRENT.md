# Current Handoff

Updated 2026-09-29. Public self-hosted preview remains online. Android/Windows
0.5.1 are local candidates; **stable release and marketing readiness are unproven**.

## Current user scope

Further UX polish, then marketing handoff and posters after the UX loop is verified.
A physical Android is unavailable; the user accepted candidate work. Latest explicit
choice: **local UI validation only; retain real submission acceptance**. No real Relay
tasks, permission grants, Codex execution or social posts. Campaign assets remain
behind the agreed gate. Do not retry the previously blocked live share by another
mechanism. Automatic review gave only `blocked by policy`; the user chose local-only.

## Completed this pass

- Deterministically reproduced history/Home one-row shifts on consecutive pre-layout
  updates and local removal blocked behind a history GET holding the outbox lock.
- History reads now release the outbox lock during network access. Instance-scoped
  request tokens reject older page publication; older task receipts cannot replace
  newer cache entries. Actual upload/removal serialization remains intact.
- Separate local-removal worker shows progress, disables duplicate actions, retains
  one operation across rotation, and preserves errors after polls/recreation.
  Interrupted process restoration with a remaining copy explains the interruption.
- List anchors use the ID bound to the visible View, avoiding replaced-data indices.
  Added seven native tests. Full suites passed72/72 on API35 and API26 at100%.
  Initial focused run had one viewport failure; isolated rerun and both full suites
  passed. Final helpers wait for pre-draw, not queue idle; ten focused cases pass
  at200% on both APIs. Preserve the earlier failure, do not claim its timing proven.
- Screenshot review caught an incorrect retry hint during removal. Final wording
  describes local-copy removal; disabled controls/rotation/progress pass on both
  APIs at200%. Signed build, JVM12 and lint pass. Candidate refreshed, not published.
- Contract, PRD and UI matrix updated. No Node/Windows changes, no remote actions.

Canonical evidence, hashes and exact logs:
[0.5.1 UX record](../../docs/releases/0.5.1-ux.md).
Control coverage and unmet gates: [UI matrix](../../docs/releases/0.5.1-ui-matrix.md).

## Artifacts and environment

- Current signed APK `.local/releases/ux-polish-sep29/DropRun-0.5.1-android-candidate.apk`:
  SHA256 `fefbe41e64341e9f0a3e7ebbbffe9435a0346e6342fb682907d131f4f36188cb`.
  Version0.5.1/code14, package `app.droprun.mobile`, same stable certificate.
  Previous927b2a... candidate saved as `DropRun-0.5.1-before-history-race.apk`.
  The earlier `DropRun-0.5.1-import-retry-review.apk` remains separate.
- Windows artifacts unchanged: `.local/releases/ux-actions-sep29-windows-final/`.
  No trusted publisher signature. Signing material stays ignored.
- New screenshots `.local/ux-cloud-check/history-removing-api{26,35}-large.png`
  show native200% UI with synthetic data. Older actual process-recovery evidence
  and all failed logs remain linked in the UX record.
- Both emulators stopped; API35 DropRunTest/5556 restored font2.0, keep-awake3,
  auto-rotation1/user-rotation0. API26 DropRunApi26/5558 restored font1.0,
  keep-awake0, auto-rotation1/user-rotation absent. Check before changing next time.
  Outbox cleanup passed; older unknown6d908a45-e442-428d-aac7-b9eace8e1705 remains.
- Release package on API35 remains paired and force-stopped. Use `.debug` only.
  Never run two UI automation clients on one emulator. Wait for terminal Gradle
  success before APK install; instrumentation requires `OK (N tests)`, not exit0.

## Remaining gates

- Real submissions/receipt/execution/report/follow-up intentionally unverified.
  Earlier isolated QA pairing/catalog addition is described in ignored
  `.local/ux-cloud-check.mjs`; remote cleanup remains pending. Private Connector
  untouched. No task sent; do not mutate the Relay under current local-only scope.
- Physical Android/TalkBack/performance,20 genuine source samples,five complete
  tasks over three projects,clean Windows/fresh Cloudflare installation,upgrade
  and uninstall remain open. Emulator timing does not establish device performance.
- ShareImport now preserves verified files/failure state and supports explicit
  retry across process restoration. Never-restored incoming journals have no
  recovery surface yet. Real provider URI loss, storage-full/max-size and short
  landscape require further coverage. See prior process evidence in UX record.
- Original API35 SyncJob ANR(2026-09-29 18:10:25) still has no proven root cause;
  earlier API26 emulator crashes and test failures remain recorded.

## Public baseline (unchanged; historical evidence in 0.5.0 record)

Website https://droprun.dengmaizi0802.chatgpt.site (`/zh/`).
Repository https://github.com/MrMaii/droprun .
Prerelease https://github.com/MrMaii/droprun/releases/tag/v0.5.0-rc.1 .
Tagged source `90718812d764c2d590e2d0db2d1381e2a2dfccaf`.
CI https://github.com/MrMaii/droprun/actions/runs/36290007838 .
GPTSites project `appgprj_6ab726bb8cd48191acd3234da262627d`, version2;
do not duplicate. Hosting uses generated landing/dist; old Netlify untouched.
Git author Thomas Deng <150266369+MrMaii@users.noreply.github.com>.

## Single recommended next action

Examine never-restored incoming share ownership/recovery using local fixtures.
A journal survives process loss, but an abandoned task currently has no screen
for the user to discover it. Define a minimal instance-scoped recovery path and
explicit discard, without inventing server receipt or silently deleting materials.
Then cover the incoming-failure controls at short landscape/200% font. Keep unknown
older files untouched. No genuine source, Relay submission or Codex execution.

## Files involved

MainActivity, ProjectHistoryActivity, Store, LocalRecoveryTest, PRD, CONTRACTS,
UI matrix, UX release record and this handoff. Ignored logs/screenshots/signed APK.
