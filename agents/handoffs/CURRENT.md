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

- Windows setup now exposes a read-only local project inventory: full IDs, local
  root paths and availability. This closes the phone's same-name identity lookup.
- Codex diagnostics now traverse all pages, not only the first 100 projects.
  Failed/partial or signed-out checks do not expose an inventory. Account details
  and project instructions stay excluded; CLI/default diagnostics remain path-free.
- The inventory remains behind the existing loopback/Host/Origin/session boundary.
  Relay synchronization remains unchanged; no local root paths are sent to it.
- Bilingual loading, empty, unavailable and error recovery states are implemented.
  Checks clear stale rows; diagnostic errors persist across ordinary status polls.
- 13 setup contract tests pass. Full Node regression: 194/194, no skips. The real
  browser fixture covers 101 rows, long paths, safe text rendering, keyboard
  disclosure/focus, narrow layout, 200% zoom, dark/reduced motion and retry states.
- Windows candidate installer and portable ZIP built successfully. Package hashes
  and verification are recorded in the release record. Public release and website
  remain unchanged.

The preceding Android pass (`ac5d284`) fixed recent ordering, same-name identity
labels and unavailable-target guards. Its 24 native cases and 11 JVM cases pass
with exact runtime/font evidence in the release record. The signed APK is unchanged
in this Windows-only pass.

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
Windows installer/ZIP: `.local/releases/ux-polish-sep29-windows/`.
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

Verify Windows setup action recovery locally: media-tool/update controls currently
lack consistent pending state, and ordinary status polling can erase action errors.
Reproduce with synthetic local responses, then fix feedback without starting real
deployment, installation, Connector execution or task submission.

## Files involved

scripts/setup.mjs, installer/setup.html/setup-ui.js/setup.css, setup contract and
browser tests; PRD, contracts, bilingual self-hosting guides, changelog, release
record and this handoff. Android source/artifact unchanged. No public deployment
or social posting in this pass.
