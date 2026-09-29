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

- Reproduced share-model keyboard selection moving focus to the backdrop labelled
  Cancel. Model/effort panel rebuilds now restore focus by model ID or effort value
  and reveal the replacement control. Touch selection does not force focus.
- Chips, option rows and effort segments now distinguish keyboard focus from
  selected state. Existing selected fills and press ripples remain. Dedicated
  styleDanger has no production caller; its unused palette was not modified.
- Added one native case selecting a synthetic model and effort in both themes.
  It checks replacement focus, full visibility, selected values, unchanged default
  model and empty outbox. No task is submitted.
- Full current-source native suites: API 35 and API 26 each 51/51. Four relevant
  cases pass at 200% on each API. Twelve JVM cases, debug lint, debug/test builds
  and signed release build pass. Refreshed the ignored APK, same certificate.
- Initial class-based share Activity launch timed out before assertions; using
  the existing ACTION_SEND harness then reproduced the actual focus failure.
  Both logs remain preserved. Final DropBox snapshots contain no new ANR.
- No public deployment, remote mutation, Windows change or campaign asset change.
  Emulator preferences restored and both devices stopped.

Evidence and current artifact hash: [0.5.1 UX validation](../../docs/releases/0.5.1-ux.md).
Control/flow coverage and missing acceptance: [UI matrix](../../docs/releases/0.5.1-ui-matrix.md).
Do not duplicate detailed historical run results here. The current 51-case suites
include prior model/default focus, Home/history restoration, file-export recovery,
permission, retention, instance-isolation and scheduling cases. Those tests prove
only their local assertions, not real delivery or physical performance.

APK: `.local/releases/ux-polish-sep29/DropRun-0.5.1-android-candidate.apk`.
Windows installer/ZIP: `.local/releases/ux-actions-sep29-windows-final/`.
Current screenshots: `.local/ux-cloud-check/share-choice-api{26,35}-{light,dark}.png`
and `share-choice-api{26,35}-large-{light,dark}.png`. Synthetic fixtures are not
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

Consolidate the final local acceptance audit against the UI matrix and original
release gates. Identify concrete remaining locally reproducible failures or missing
checks, separating them from user-retained real-submission and unavailable device
acceptance. Do not prolong work with speculative restyling, treat synthetic flows
as real delivery, or start marketing assets before the agreed acceptance gate.

## Files involved

ShareActivity, Ui, LocalRecoveryTest, PRD, CONTRACTS, UI matrix, UX release record
and this handoff. Refreshed ignored signed APK and local screenshots.
