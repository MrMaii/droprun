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

- Replaced barely visible light action focus borders with 2dp accent outlines;
  primary lime actions use a dark inner border. Icons now have a distinct focus
  outline, with disabled state taking precedence. No layout/activation changes.
- Inspected actual focused Home/Settings screenshots in light and dark themes.
  Palette contrast of light secondary focus against white rises from 1.34:1 to
  7.94:1; this is not a full rendered accessibility certification.
- Four keyboard/navigation cases pass on API 26/35 at 100%; model/effort also
  passes at 200% on both. Twelve JVM cases, debug lint and builds pass. No new
  instrumentation case or full-suite claim for this drawable-only change.
- Refreshed signed APK with unchanged certificate. Final DropBox snapshots show
  no new ANR. Emulator preferences restored and devices stopped. No real mutation,
  public deployment or campaign assets. Evidence/hash live in the release record.

The preceding pass (`b297d30`) restored model/effort keyboard focus and full control
visibility across Settings rebuild/recreation. Five affected cases passed on API
26/35 at 100%/200%. It added the 50th available native case, but did not rerun the
whole suite; the prior 49-case full results below belong to the prior source.

The preceding pass (`d5365e2`) preserved Settings scroll/expanded access and
appearance/language focus, including revealing a clipped Language row after a
landscape locale change. Final native suites passed 49/49 on both APIs, with
three Settings cases at 200%. Its initial emulator, test timing and stale-test
installation failures remain in the release record.
The preceding pass (`b7a1a09`) preserved Home project/offset across recreation
and appearance changes, with 45 full cases plus a separate actual Settings
round-trip case. Those cases are included in the final 49-test runs above.
The preceding pass (`6024cbb`) fixed negative stable task IDs causing history
recreation to jump to the top. Its 240-row tests preserve task/offset and cached
pages through status updates, pending changes and touch/keyboard recreation.
Full native suites passed 42/42 on both APIs; three history cases passed at 200%.
The preceding pass (`a30ef4b`) validated the actual system document picker and
partial-write recovery. Both emulator pickers reset edited names on rotation;
this limitation is documented, not counted as passed. Full native suites passed
39/39 on both APIs; details remain in the release record.
The preceding pass (`5d7ab01`) added retained export progress, duplicate-request
locks, inline success/failure/retry and leave confirmation with deferred source
cleanup. Its 36 cases passed on both runtimes at both font scales. The current
release APK still contains that production implementation.
The preceding pass (`604d556`) preserves unchanged periodic JobInfo, registers
missing/changed schedules, and removes DNS/home-launch dependencies from local
fixtures. Its final API 26/35 runs each passed 30 cases. The original API 35
SyncJob service-timeout ANR remains unproven: logs suggest lifecycle interference,
not a confirmed root cause or an exhaustive background-delivery fix.
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

Audit the remaining shared control states locally, starting with the light-theme
destructive button's pressed text/fill contrast in Ui.styleAction. Verify actual
rendered states without activating delete/disconnect/approval actions. The current
pass validated action/icon focus, not every pressed/disabled/chip/option state.
Keep remote mutations disabled; this is not real-delivery or marketing acceptance.

## Files involved

Ui.java, PRD, CONTRACTS, UI matrix, UX release record and this handoff. Refreshed
ignored signed APK and local screenshots. Public deployments, Windows packages
and private Connector unchanged.
