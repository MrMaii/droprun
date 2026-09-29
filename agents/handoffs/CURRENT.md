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
- Native instrumented recovery tests: lifecycle/draft, partial upload retry,
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
  Ten emulator tests pass at both 100% and 200% font on API 26 and API 35. Stable signer
  verified. Tests do not establish physical-device performance or live acceptance.
- Empty-home guidance previously disappeared at 200% font; it now scrolls without
  truncation. Keyboard focus previously stopped at ListView; both lists now let
  cards receive focus and Enter. Actual keyboard project/history/report/follow-up
  navigation was checked, including cancelling and reopening an unsent draft.
- Minimum runtime API 26 now has a dedicated local AVD (`DropRunApi26`, port 5558).
  Ten tests pass at both 100% and 200% font; opaque dark sharing surfaces inspected.
  The signed candidate installed and opened pairing without connecting. Both
  packages were stopped afterward; the AVD was stopped to free resources.
- Share keyboard navigation now skips the inactive sheet and duplicate model icon;
  the tenth regression checks note/model/send focus without sending. Settings
  Appearance was opened/cancelled with keys; final signed EN/ZH pairing UI checked
  on API 26 without pairing. Both dedicated emulators are stopped.

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
- Physical Android/TalkBack/performance, full keyboard coverage,
  20 genuine sources, five full tasks over three projects, clean Windows/new
  cloud-account installation and upgrade acceptance remain open.
- Empty-home, keyboard project/history/follow-up and API 26 fallback evidence is
  in the release record. Other screens and complete TalkBack still need coverage.

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

Build a local UI control/state acceptance matrix and cover remaining approval,
offline and pending screens with labelled synthetic fixtures. Keep live submissions disabled and acceptance gaps
visible; do not claim marketing readiness or publish campaign assets as proof of
an end-to-end handoff.

## Files involved

Android native Views/activities, dialog/animation resources and recovery tests;
test-only Miniflare adapter/regression; CI test APK build; package versions;
PRD, contracts, development guide, notices, changelog, release record and handoff.
No new public release, website deployment or social post was performed.
