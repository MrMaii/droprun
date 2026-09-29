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

- Reproduced short-landscape failure UI at200%: stacked actions plus86% height cap
  left less than72dp of readable explanation. Initial test inherited the launcher's
  portrait orientation; using the local fullscreen Home as backdrop made it valid.
- Compact screens(available height<=400dp) now use available height. Failure actions
  are parallel in landscape and stacked in portrait. Landscape hides the decorative
  title and shows the failure cause before counts/files; content remains scrollable.
  Rotation preserves one import, kept bytes, no outbox entry and no provider replay.
- Full native suites pass73/73 per API at200%. After the final cause-first adjustment,
  all15 attachment cases pass on each API at200%. JVM12, lint and signed build pass.
  Stable native captures inspected; prior captures caught system rotation frames,
  so final screenshot capture waits for those transforms as well as View animation.
- Candidate refreshed locally. No Node/Windows changes or external mutation.
- Recovery source inspection found the next gap: journals retain material but not
  note/project/model choices; those only have an Activity Bundle. No Home entry
  opens a never-restored journal. onDestroy also discards whenever isFinishing(),
  which does not distinguish explicit discard from other reasons a page ends.
  PRD now names this still-unmet recovery acceptance. No recovery UI implemented.

Canonical evidence, hashes and exact logs:
[0.5.1 UX record](../../docs/releases/0.5.1-ux.md).
Control coverage and unmet gates: [UI matrix](../../docs/releases/0.5.1-ui-matrix.md).

## Artifacts and environment

- Current signed APK `.local/releases/ux-polish-sep29/DropRun-0.5.1-android-candidate.apk`:
  SHA256 `d2c8084e7b76dbda8f173f9777498f90023c755f378e68b43e710ca6e65e3b51`.
  Version0.5.1/code14, package `app.droprun.mobile`, same stable certificate.
  Previousfefbe4... candidate saved as `DropRun-0.5.1-before-short-landscape.apk`.
  The earlier `DropRun-0.5.1-import-retry-review.apk` remains separate.
- Windows artifacts unchanged: `.local/releases/ux-actions-sep29-windows-final/`.
  No trusted publisher signature. Signing material stays ignored.
- New screenshots `.local/ux-cloud-check/receive-failure-{landscape,portrait}-api{26,35}-large.png`
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
  recovery surface yet. Real provider URI loss and storage-full/max-size
  require further coverage. See prior process evidence in UX record.
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

Implement recoverable, instance-scoped unsent shares with local fixtures. Retaining
files alone is insufficient: preserve the note, project and model choices alongside
material; show a recovery entry after the original page is gone, and require an
explicit choice before discarding. Never auto-submit or imply server receipt.

Implementation proposal, not an adopted architecture: keep editor snapshots with
the existing atomic import journal, serialize writes off the UI thread, and guard
late writes after transfer/discard and across recreation. Add a non-exported recovery
route rather than trusting recovery IDs passed to the exported share target. Handle
an active editor/copy without concurrent owners of the same files. Show only the
current instance's drafts; explicitly handle unpaired drafts at first pairing,
never adopt another paired instance's material. Exclude pending/outbox-owned IDs.
Do not treat isFinishing() alone as evidence the user confirmed deletion. Corrupt
or legacy material cannot be silently swept; unknown6d908a45... remains untouched.

Verify real background process loss with private fixtures, restored note/material
identity, explicit discard/Keep, duplicate-open prevention, two-instance isolation
and no submission. Latest authorization remains local-only; no real sources, Relay
mutation or Codex execution. All formal device/provider/install gates remain open.

## Files involved

ShareActivity, ShareAttachmentTest, PRD, CONTRACTS, UI matrix, UX release record and
this handoff. Ignored logs/screenshots/signed APK. Previous history fixes remain
committed in fa1c8bf; their evidence is in the canonical UX record.
