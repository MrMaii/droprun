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

- Investigated the prior API 35 ANR. Logs show cancelled-job binding timeouts;
  captured app main thread was idle and instrumentation was entering an Activity.
  This suggests test/job lifecycle interference but does not establish root cause.
- Fixed a proven scheduling problem: reopening home repeatedly registered the
  same periodic job. Identical JobInfo now preserves the existing job; missing or
  changed schedules are registered. Explicit immediate retries remain separate.
- Two new native contract cases reproduce the old duplicate registrations and
  pass after the fix. They use Android JobInfo with a recording scheduler, not
  actual background-delivery execution.
- Local UI setup seeds data without opening home and cancelling a newly scheduled
  job before each test. The decision-error fixture now supplies a local initial
  refresh too; a DNS-dependent test timeout was reproduced and removed.
- Final API 26 and API 35 runs each pass 30/30 at 100% font. Twelve JVM tests,
  debug/test/release builds and both lint variants pass. No new DropBox ANR in
  either final run; the old incident is retained and not claimed fixed.
- Signed candidate refreshed. Exact hash/logs are in the release record; no push,
  public deployment, real submission or existing Connector change.

The preceding Android pass (`f8c370d`) verifies restored/exported cache bytes,
shortens file errors and fixes focus scrolling within system-bar padding. Its
28 native cases passed on both runtimes at 100%/200% font; 12 JVM cases passed.
Final tests above include those 28 cases. Original ANR and emulator system-server
crash evidence remain preserved in the release record.
The preceding Windows pass (`3f02f56`) added immediate action locks and feedback,
status reconciliation after lost acknowledgement, persistent inline errors and
localized release-check states. Node suite passed 198/198, no skips; all five
setup browser cases passed the final inline adjustment. Installer/ZIP built and
2,019 manifest files plus checksums verified. No Windows/Node source changed here.

The preceding Windows pass (`61c4870`) added a protected read-only inventory of
full project IDs, local root paths and availability, including all project pages.
Account details/project instructions are excluded; CLI diagnostics stay path-free.
Its Node suite passed 194/194. Public website/release remain unchanged.

The preceding Android pass (`ac5d284`) fixed recent ordering, same-name identity
labels and unavailable-target guards. Its 24 native cases and 11 JVM cases pass
with exact runtime/font evidence in the release record. Current Android evidence
is recorded above.

The previous pass (`f486599`) preserved share search/catalog expansion across
recreation, made permission surfaces scroll, contained keyboard focus, and exposed
synthetic grant errors without advancing. Its 21 native tests passed on both
runtimes at both font scales. No real grant or task was sent.

Previous local pass (`a97658c`) added the control/state matrix, fixed cached
approval/preview expiry and persistent decision errors, added instance-scoped
retention policy display, clarified Chinese reading status, and prevented
scrolling content from overlapping system bars. Its 17 tests passed on both
runtimes at both font scales.

Earlier local work remains: quieter project cards, grouped settings, result-first
reports, shared press/navigation/disclosure motion, draft restoration, explicit
note discard, 200% empty-home guidance, keyboard card activation, and share focus.
Foreign outbox records are rejected without retagging. The test-only Miniflare
framing fix passed 191/191 Node tests and a repeated 28/28 relevant subset; current
Node validation is recorded above. Earlier local commits: `6a96bb4`,
`767651a`, `5abb672`.

Canonical evidence and current artifact hash:
[0.5.1 UX validation](../../docs/releases/0.5.1-ux.md).
APK: `.local/releases/ux-polish-sep29/DropRun-0.5.1-android-candidate.apk`.
Windows installer/ZIP: `.local/releases/ux-actions-sep29-windows-final/`.
UI captures: `.local/ui-public/` and `.local/ux-cloud-check/`.
Fixtures are demonstration data, never evidence of actual agent execution.

## Remaining gates and environment

- Real submission/acknowledgement/execution/report/follow-up remains intentionally
  unverified. Earlier automatic review rejected an ADB live share with only
  `blocked by policy`; the user then selected local-only verification. Do not
  retry that submission through another mechanism.
- Before that choice, isolated QA pairing succeeded. `.local/ux-cloud-check.mjs`
  and ignored state describe a temporary QA project/catalog addition. Remote
  cleanup is pending. No task was sent; a synthetic report is not Codex execution.
  The paired release app remains force-stopped. Private Connector config unchanged.
- Dedicated emulators: API 35 `DropRunTest` port 5556 and API 26 `DropRunApi26`
  port 5558. Do not run another UI automation client during instrumentation.
  Test fixtures refuse to overwrite an existing developer Relay connection.
  Both dedicated emulators are stopped; font/keep-awake preferences restored.
- Accessibility may briefly have no active window during dialog replacement.
  The test helper waits for the exact visible enabled action with a bounded
  deadline. Window-fade capture waits affect tests only. Failed runs are retained,
  not counted as passes; see the release record.
- Physical Android/TalkBack/performance, remaining keyboard paths, 20 genuine
  sources, five complete tasks over three projects, clean Windows/new Cloudflare
  installation, upgrade and uninstall acceptance remain open.

## Public baseline (unchanged)

- Website: https://droprun.dengmaizi0802.chatgpt.site (`/zh/` for Chinese).
- Repository: https://github.com/MrMaii/droprun
- Release: https://github.com/MrMaii/droprun/releases/tag/v0.5.0-rc.1
- Tagged source: `90718812d764c2d590e2d0db2d1381e2a2dfccaf`.
- Hosted CI: https://github.com/MrMaii/droprun/actions/runs/36290007838
- Baseline evidence: [0.5.0](../../docs/releases/0.5.0.md).

GPTSites project `appgprj_6ab726bb8cd48191acd3234da262627d`, public saved version 2.
Do not register a duplicate. Hosting uses generated `landing/dist`. Old Netlify
and private Connector were not replaced. Windows has no trusted publisher signature.
Signing material remains ignored. Git author: Thomas Deng
<150266369+MrMaii@users.noreply.github.com>.

## Single recommended next action

Verify Android export progress/recovery with a synthetic document provider:
DeliverablesActivity returns from the picker and performs background authorization
and copy work without explicit pending feedback. Reproduce duplicate Save,
provider failure and lifecycle changes locally before modifying behavior.
Keep the prior ANR as an unresolved real-background acceptance item; current local
regressions do not reproduce it. Do not send real tasks or call fixtures delivery.

## Files involved

SyncJob, TaskActivity; debug fixture seeding/decision activity/manifest; native
tests; PRD, contracts, changelog, release record and this handoff.
No Windows runtime change, public deployment or social posting in this pass.
