# Current Handoff

Updated 2026-09-29. Public self-hosted preview remains online; Android 0.5.1 is a
local signed UX candidate. **Do not claim stable/marketing readiness.**

## Current user scope

The user requested further UX polish, then a marketing-ready handoff and several
posters/assets after the UX loop is verified. They accepted a candidate without
a physical Android. Their latest explicit choice is **local UI validation only;
retain real submission acceptance**. Do not send tasks to a real Relay. No social
posting is authorized. New campaign assets remain deferred until the agreed gate.

## Completed this pass

- Added the [control/state matrix](../../docs/releases/0.5.1-ui-matrix.md), separating
  source inspection, native local tests and real acceptance gaps.
- Reproduced and fixed cached command/preview actions remaining live after expiry.
  Failed task decisions now use a persistent error dialog, surviving background
  refresh. An exception does not claim that the server never received the action.
- Verified pending-copy inspection and Keep/remove confirmation using local files.
- Added read-only retention settings: actual last-synced policy, instance-scoped
  cache, explicit unknown/offline state, and local-copy/preview/report explanations.
- Corrected Chinese reading status: attempted extraction is not complete coverage.
- Found scrolled settings text overlapping the clock. The shared ScrollView now
  clips to system-bar/keyboard insets. Its visible-bounds regression failed before
  the fix and passes afterward.
- The native suite now has 17 cases. Final runtime/font results, red evidence,
  screenshots and logs are in the release record below. No live task was sent.
- Debug/release/test APK builds, nine unit tests, debug/release lint and stable
  signer verification pass. Refreshed local signed 0.5.1 candidate; no publication.
- All 17 native tests pass on API 26 and API 35, at 100% and 200% system font.
  Settled safe-area and large-font confirmation captures were inspected.

Earlier local work remains: quieter project cards, grouped settings, result-first
reports, shared press/navigation/disclosure motion, draft restoration, explicit
note discard, 200% empty-home guidance, keyboard card activation, and share focus.
Foreign outbox records are rejected without retagging. The test-only Miniflare
framing fix passed 191/191 Node tests and a repeated 28/28 relevant subset; Android
changes in this pass do not change Node code. Earlier local commits: `6a96bb4`,
`767651a`, `5abb672`.

Canonical evidence and current artifact hash:
[0.5.1 UX validation](../../docs/releases/0.5.1-ux.md).
APK: `.local/releases/ux-polish-sep29/DropRun-0.5.1-android-candidate.apk`.
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

Use the matrix to complete remaining local keyboard and share catalog/search/no-match
coverage with labelled fixtures. Keep real submission disabled. Then report the
remaining externally dependent acceptance gates without claiming marketing readiness.

## Files involved

TaskActivity, TaskPresentation, SettingsActivity, shared Ui and LocalRecoveryTest;
PRD, contracts, changelog, UI matrix, release record and this handoff. No Node
runtime change, public release, website deployment or social post in this pass.
