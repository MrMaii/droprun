# Snapshot details on demand — October 6, 2026

The result card now explains that a static preview is a fixed snapshot from this
handoff. The complete version remains in a selectable, initially collapsed
**Snapshot version / 快照版本** section, after Follow up and before Full report.
This source refinement does not replace the published 0.5.4 candidate packages.

## Observed problem and bounded change

The Connector's static revision is a 64-character hexadecimal content digest.
The former result explanation appended that entire value to its main sentence.
On the actual 320dp-wide native result card, this produced three lines of
technical metadata where the user needs to understand what the preview shows.
The earlier `sample-version` fixture did not represent its real length.

Two production lines change in `TaskActivity`: the fixed-snapshot explanation
and a call to the existing disclosure helper. That helper already uses a
selectable TextView, local expansion motion and the Activity's saved expanded
set. No new UI abstraction is introduced. The previous display boundary is
retained: snapshot or legacy static kind, a nonempty preview state other than
unavailable, and a nonempty version. Live-preview explanation, eligibility,
links, expiry, renewal, permissions and version binding are unchanged.

The value is neither shortened nor rewritten. `ReportText.render` adds its
existing display newline; all 64 characters remain exact in the TextView and
its native accessibility node. No clipboard operation was performed.

## Native reproduction and verification

The existing non-exported debug preview fixture accepts an explicit
`snapshotVersionProbe` intent input. It seeds a synthetic 64-character value,
snapshot/ready state, an invalid-domain URL and expiry into its memory sample
before production rendering. Recreation uses the same intent. No new bitmap is
created by this probe. Existing navigation, API, credentials, preference edits,
stored-task, cache/outbox, file and service guards remain active.

- Red: the new method failed **1/1 in 2.388s**, specifically because the main
  result explanation exposed the full digest. The marked native PNG, original
  sources, development APKs and failure log are retained.
- Green build: **31s** in Gradle, host 32.1432418s. JVM **24/24**, zero
  failures/errors/skips; lintDebug **zero errors / 30 warnings**. All 100 Android
  source hashes matched before and after the build.
- Combined native run: **5/5 in 48.2s**, host 49.256837s. It includes the new
  version-disclosure method, the existing preview-action eligibility method and
  all three existing thumbnail methods. No Settings, Pair or full-app matrix ran.
- Capture correction: review found the 9sp diagnostic marker clipped in four
  200% captures. Those originals are retained. Only the test overlay changed to
  9dp, with explicit native text-layout completeness assertions. The product
  text still uses the actual Activity font scale. The new method alone then
  passed **1/1 in 29.01s**, host 30.3013318s, and captured a fresh series.
  This is separate from the earlier 5/5 run. JVM/lint and the four unchanged
  methods were not repeated; the development App APK remained byte-identical.

The new method covers EN/ZH × light/dark × Activity fontScale 1/system 2, eight
configurations. Each starts collapsed, reveals an individually reachable
>=48dp header, expands locally, reads the complete selectable value, recreates
and confirms expanded state, collapses, then recreates and confirms collapsed
state. It checks native node text, no ellipsis, complete line coverage, actual
scroll/physical bounds, the section order and unchanged memory preview fields.
Only the local disclosure header is activated. The guard count stays zero.

The existing preview method retains ready/expired/stopped/reopening/live and
busy eligibility and primary-action checks. The thumbnail methods retain
minimum target height, uncropped aspect ratio, disabled busy focus/action,
stale callback suppression and 200% individual-control reachability on the new
combined Task source. Their colored images and click counts are synthetic
memory fixtures, not actual delivered files or business navigation.

## Actual captures and retained evidence

The final **12 raw 320×640 PNGs** were individually inspected. Eight show normal
text, collapsed main-card explanation or expanded version, across both
languages/themes. Four show the expanded value at 200%. The full value and the
“UI probe · memory version · no task” marker, or Chinese equivalent, are visible.
These show the relevant scrolled regions, not every page control or system-bar
state. The synthetic value and sample report do not claim a real Codex result.

Private originals remain under `.local/ux-oct6-preview-copy/`:

- `screens-red/`: one original PNG/JSON pair.
- `screens-green/`: twelve first-pass pairs, including the four rejected large
  text markers. Their product controls passed the native checks.
- `screens-green-marked/`: twelve accepted pairs. Names use
  `snapshot-{en|zh}-{light|dark}-font1-{collapsed|expanded}` and
  `snapshot-{en|zh}-{light|dark}-font2-expanded`.
- `build-red/`, `build-green/`, `build-capture-marked/`: immutable source and
  APK copies, inventories and original build logs. Native logs and device
  observations are kept separately for each phase.

All **50 original PNG/JSON files** have matching device/host SHA-256 hashes.
The final 100-file source inventory matches the last build. Only the new test's
capture helper changed after the combined native run; production source and
App APK did not. The earlier [thumbnail record](ui-task-thumbnail-2026-10-06.md)
describes its own 5/5 run with Task hash `5a2d17ed…`; that evidence is preserved,
not relabelled as the current Task source. This phase starts after curated
commit `c35d0358240f0a310c5378d0138825c7ea15b617`.

Device settings stayed API 35, 320×640/160dpi, system fontScale 2.0 and the
original animation/IME configuration. Normal text is local Activity fontScale 1.
Crash-buffer bytes match. Six new `Unable to freeze binder` events are retained;
no new ANR/FATAL was observed. No reset, process kill, data clearing, device
setting change or previously rejected workflow was used.

## Frozen identities

| Local artifact | SHA-256 |
| --- | --- |
| `TaskActivity.java` | `b0e3b3aeb8c62f585e561d7d738dbf7dae2aa083c2dcf2c591ac07dcbf3646fe` |
| `DemoTaskPreviewActivity.java` | `664cad9632e67a7098bda98cb4551075713f13907fd23993202685d29c24eb12` |
| `TaskPreviewFeedbackTest.java` | `da8e6893d5b4262fcd5cce66b8d973fbfb500fe0dacb8c9742bf423f2efa7bbe` |
| New test, combined 5/5 stage | `2a3ed90b2e6dce09c31a5c6206b8952a69f8a8d3edacde7c5606a4fafad4261b` |
| New test, final marker stage | `5a43b687c7320a142bdf731ba9669d1df55671bae7b8ba4f47f8549281db432c` |
| Red development App APK | `21a154c6101e6a52535e34540b7d6ee3e97395b1e49f60dadaa1741b7efe9c5f` |
| Green/final development App APK | `b872da5824cb68c312f801606438acb0511c6e5e14b0d38c60aaac0b3387658a` |
| Combined 5/5 test APK | `ee43f7791ffbe73059d4d1e4e217735290fcc4bd176521eb6fee50e26907123f` |
| Final marker test APK | `42a68ca2975f3dcddec99db7b95563979fcea56f86fd1ef9d84977ea7bc61022` |

The Android source/device lease is released after this freeze. Physical touch,
TalkBack, clipboard use, performance, genuine execution, downloaded files and
real preview/version verification remain outside this local check. Existing
launch gates are unchanged; this pass changes no version, public package,
website, README or marketing-readiness claim.
