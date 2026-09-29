# Current Handoff

Updated 2026-09-29. Public self-hosted preview remains online; Android 0.5.1 is a
local signed UX candidate. **Do not claim stable/marketing readiness.**

## Current user scope

The user requested further UX polish, then a marketing-ready handoff and several
posters/assets after the UX loop is verified. They accepted a candidate without
a physical Android. Their latest explicit choice is **local UI validation only;
retain real submission acceptance**. Do not send tasks to a real Relay. No social
posting is authorized. New campaign assets remain deferred until the agreed gate.

## Completed

- Quieter project cards, grouped settings, result-first report excerpts, shared
  press feedback, page transitions and animated local disclosures.
- Larger-font wrapping, saved-item inspection/retry, follow-up draft and task ID
  restoration, permission confirmation and immediate local-save feedback.
- Confirmation before discarding an unsent note. Rounded native dialogs with
  sentence-case actions; inspected light and dark/200% font without clipped actions.
- Six native instrumented recovery tests: lifecycle/draft, partial upload retry,
  lost acknowledgement identity, instance isolation, explicit discard and foreign
  outbox rejection. All pass on API 35 with synthetic transport, files and Keystore.
- The foreign-record test reproduced a real bug: saving a failure retagged it to
  the current instance. The failure path now preserves foreign identity and does
  not block valid local records. No live server submission was used.
- Miniflare ECONNRESET isolated to early rejection of an unframed string body.
  The test-only adapter supplies its UTF-8 Content-Length; no mutation retries or
  weakened assertions. Full Node suite **191/191**, repeated relevant suites **28/28**.
  Local commit `6a96bb4` contains the transport fix and regression.
- Final signed/debug/test APK builds, nine unit tests and debug/release lint pass.
  Six emulator tests passed again after the dialog style change. Stable signer
  verified. Tests do not establish physical-device performance or live acceptance.

Canonical evidence, artifact hash and limits:
[0.5.1 UX validation](../../docs/releases/0.5.1-ux.md).
Local APK: `.local/releases/ux-polish-sep29/DropRun-0.5.1-android-candidate.apk`.
Screenshots/video: `.local/ui-public/`; new dialog captures `.local/ux-cloud-check/`.
Fixtures are visibly marked demonstration data; no agent work was performed.

## Validation gaps and environment

- Real submission/acknowledgement/execution/report/follow-up remains intentionally
  unverified. A previous automatic review rejected an ADB live share with only
  `blocked by policy`; the user then selected local-only verification. Do not
  retry that submission through another mechanism.
- Before the choice, isolated QA pairing succeeded. `.local/ux-cloud-check.mjs`
  and its ignored state describe a temporary QA project/catalog addition. Remote
  cleanup is pending. No task was sent; a helper's synthetic report is not evidence
  of Codex execution. The paired release app remains force-stopped. Private
  Connector configuration was not changed.
- A long-running emulator developed a system ANR. Failed/stalled runs were not
  counted as passes; the same AVD was cold-started without wiping data. Final
  tests passed afterward. Font scale and keep-awake settings were restored;
  the debug package was force-stopped after local checks.
- Physical Android/TalkBack/performance, API 26 fallback, full keyboard coverage,
  20 genuine sources, five full tasks over three projects, clean Windows/new
  cloud-account installation and upgrade acceptance remain open.
- Do not infer every state is visually perfect from the six screenshots. The empty
  home at 200% font and complete keyboard navigation still warrant local inspection.

## Public baseline (unchanged)

- Website: https://droprun.dengmaizi0802.chatgpt.site (`/zh/` for Chinese).
- Repository: https://github.com/MrMaii/droprun
- Release: https://github.com/MrMaii/droprun/releases/tag/v0.5.0-rc.1
- Tagged source: `90718812d764c2d590e2d0db2d1381e2a2dfccaf`.
- Hosted CI: https://github.com/MrMaii/droprun/actions/runs/36290007838
- Baseline evidence: [0.5.0](../../docs/releases/0.5.0.md).

GPTSites project `appgprj_6ab726bb8cd48191acd3234da262627d`, public saved version 2.
Do not register a duplicate. The manifest serves generated `landing/dist`.
Old Netlify and private Connector were not replaced. Windows has no trusted
publisher signature. Signing material remains ignored; never publish it.
Git author: Thomas Deng <150266369+MrMaii@users.noreply.github.com>.

## Single recommended next action

Continue local accessibility inspection of empty history and keyboard navigation,
then address observed failures. Keep live submissions disabled and acceptance gaps
visible; do not claim marketing readiness or publish campaign assets as proof of
an end-to-end handoff.

## Files involved

Android native Views/activities, dialog/animation resources and recovery tests;
test-only Miniflare adapter/regression; CI test APK build; package versions;
PRD, contracts, development guide, notices, changelog, release record and handoff.
No new public release, website deployment or social post was performed.
