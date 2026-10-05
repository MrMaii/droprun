# UI follow-through — October 5, 2026

These source changes follow the published **0.5.2-rc.1** candidate. They are not
in its existing download files. Package evidence remains in the
[candidate record](0.5.2-rc.1.md); no release tag or asset was replaced.

## What changed

Android's default keyboard-focus highlight tinted an entire focused ScrollView
or share root. The earlier large-text screenshots contained approximately 12%
lime over the content; Home's focused settings button did not recolor its page.
Screenshot pixel samples and native focus XML agree with the
[Android focus behavior](https://developer.android.com/about/versions/oreo/android-8.0-changes#input-and-navigation)
(checked October 5, 2026). Font size does not select a different palette.

`Ui.page()` and `Ui.frame()` now disable the default container highlight while
retaining focusability. Controls keep their own focus rings and keyboard actions.
The original screenshot files were not retouched or replaced.

Settings now restores an appearance or language control after its content is
rebuilt, as it already did for model and effort. Existing IDs and focus scrolling
are reused; execution-mode and project-access controls were not generalized.

Manual pairing now shows an accessible error after the fields and scrolls it
into view. Invalid entries remain available for correction. Strict destination
validation and the separate source confirmation remain unchanged.

README moves its existing demonstration explanation before the tall tour and
aligns English project-home wording with locally saved shares.

## Local verification

- Native API 35, dedicated DropRunTest emulator,200% font, 320dp wide: 8/8 focused
  checks passed with animator/window/transition scales all zero. Three cover page
  colors, overlay-root colors and localized button focus plus actual Enter
  activation; two cover Settings focus/visibility; three cover manual pairing.
- Pairing checks exercise English/Chinese, invalid Relay/instance/code/link,
  retained input, complete visible error text and valid destination normalization.
  The independent fixture records valid destinations locally and forbids pairing
  and network requests. No connection confirmation was sent.
- A further 4/4 capture run at 720×1440, density 360 (320×640dp), 200% font and motion
  off saved six unmodified native PNGs. Settings and EN/ZH validation views were
  visually inspected; these are internal probes, not public marketing media.
- `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`, `lintRelease`
  passed. JVM 24/24, no failures/errors/skips; release lint 0 errors/28 warnings.
- Original device settings were restored and read back: 320×640, 160dpi, font 2.0,
  animator unset, window 1, transition 1. Existing share drafts were not removed.

Reproduction evidence is retained under `.local/ux-oct5-color-audit/`:
`native-before-keyboard.log` has the two original color failures;
`native-mixed-baseline.log` has both Settings focus failures;
`native-form-baseline.log` has the two actual missing-form-error failures;
`native-final-motionoff.log` and `native-capture-motionoff.log` contain the passing
runs. Initial probe problems were corrected before final validation: touch mode
did not exercise default keyboard highlighting, an exact-color ring check ignored
ripple blending, and immediate dialog clicks preceded its asynchronous listener.
The initial wrong drawable name was also corrected; all original logs remain.

The tested debug APK SHA-256 is
`7223c013e2874c3c863b70ac4630a3d099dea235bed4ad87f422341b95446a1d`.
Native code hashes:

| Source | SHA-256 |
| --- | --- |
| Ui.java | `7ef703f78384d73d73f9112c583a114abf62824689f2380aa2890acb1c0e1596` |
| SettingsActivity.java | `8211df9512ab3386c8c21da116e9ef39558b6ca4308272156e65347e159e391e` |
| PairActivity.java | `1e3a1be50a8ec16deb2af0ccad8c20236266089ea8aa49f8b03176f68cd91cad` |

These checks establish specific local rendering and input behavior. They do not
establish physical-device performance, TalkBack completeness, clean installation
or genuine handoff acceptance. The rejected normal-Pair/draft-discard command
and process-loss probe were not retried. Stable launch gates remain open.

Source f2c8bfd's [CI37340478370](https://github.com/MrMaii/droprun/actions/runs/37340478370)
passed Android and failed core 223/224. The preview-retry positive fixture's second,
valid loopback handshake timed out within its 40ms budget (TimeoutError code23).
Only this positive test budget becomes 5000ms; its initial failure stays 40ms and
all handshake/rebuild/cleanup assertions remain. Production's 45s budget is unchanged.
The focused preview suite then passed 15/15, no skips, and the complete local
Node suite passed 224/224, no skips, in 34.818s. The original CI failure remains
recorded; runtime UI source and package provenance were not changed.

## Share feedback, text scaling and pending settings

Offline and disabled-notification explanations previously disappeared after
100ms with motion off, or 280ms including a 220ms fade with motion on. They now
have a 4000ms base reading window, extended by the existing accessibility timeout.
Close, Back and backdrop clicks still leave saved feedback immediately. Ordinary
saves keep their short return. The offline headline says it is waiting for a
connection; the notification-off explanation leads with where to check updates.

Flight completion recomputes the explanation. Native reproduction showed an
earlier offline message surviving reconnection during flight. This change only
describes phone persistence and future delivery; it does not acknowledge receipt.

The 200% screenshots also exposed Codex wrapping and clipping inside its fixed
48dp circle. A capsule now measures its width from the one-line label, retaining
the same height and success pulse. Originals remain under
`.local/ux-oct5-share-feedback/screens/`; new captures are recorded separately.

Execution choices previously dimmed while saving but remained enabled and
keyboard-focusable. The existing busy flag now disables both properties. The
confirmed selection, notice, duplicate-action guard and server behavior are unchanged.

README presents the three-step screenshots before a 240px-wide native tour;
the existing Demo label and dated capture notes remain. Both languages define
task/dispatch counts and name `DropRun.cmd` as the portable launch entry.

Local reproduction and verification are retained in
`.local/ux-oct5-share-feedback/`: the initial five feedback tests failed four
times on timing/stale copy; the additional badge check failed on clipped lines;
both pending-settings checks failed on enabled state. A later review strengthened
Close coverage with a nonempty in-memory note. Final feedback tests use the real
plane callback, text layout, system timeout, Close and Activity destruction.

The independent debug activities do not import, save, submit, seed preferences
or pair. The share probe takes the restored-sent presentation branch before
`ShareImport.open()`; API, task save, pairing and draft/outbox access are forbidden.
The settings probe only models busy/result states in memory and reads existing
cache. Its success notice proves controls become available, not a server-confirmed
preference change. Genuine saves and source-App return remain unverified here.

Final native matrix: API35, 720×1440/density360 (320×640dp), 200% font,
15/15 checks with motion off and 7/7 affected share/settings checks with motion
on. Eight unmodified share PNGs are retained in `screens-final/`; all four EN/ZH
motion-on attention views were inspected. The complete title, explanation and
Codex label fit; Close remains reachable. The probe notice over the launcher's
background is diagnostic content, not part of the product or marketing footage.
The on/off runs also cover a real four-second return, immediate Close with a
saved nonempty in-memory note, and simulated changes during the flight.

Builds, JVM24/24 and release lint (0errors/28warnings) passed. Original size,
density, font and animation settings were restored and read back. Final debug
APK SHA-256: `a9693721d25250330f9b6707ee48b708c78aa0ad15782a21f4f400c9386b96be`.
ShareActivity SHA-256: `6478dd4a333465164efc31889cebf2b37dbfaa373d06f1f4be502c669b5539e4`;
SettingsActivity: `ef8fa8c59586b487593bc2029a0721d82c0c919f6e669b1707f0f7dfca83fe94`.
These changes remain newer than the downloadable candidate; no release artifact
or site media was replaced, and no blocked operation was repeated.

### CI fixture follow-up

[CI37342682475](https://github.com/MrMaii/droprun/actions/runs/37342682475), source
72686fc, passed Android and the preview retry. Core had 223 passes and one
cancelled browser positive after its 45s test deadline. The first browser case's
outer budget was shorter than the existing 20s launch plus 30s load budgets.
Its test now allows 90s for the lifecycle, with all six 1280×900 captures and
interaction assertions intact, and logs capture timing. Production limits did
not change. Local browser tests passed 6/6 and Node 224/224, no skips/cancellations,
33.531s. The exact CI stage that was slow is unknown; the failed log is retained.

Source 93bc0b4 is public. [CI37348279030](https://github.com/MrMaii/droprun/actions/runs/37348279030)
passed Android and the browser verification (six captures, 12.989s). Core had
223 passes, one failure, no cancellations or skips: the brand fixture's second
PUT reported Miniflare `read ECONNRESET`. Its exact cause is not established.
The original log is retained privately; this is a separate failure from the
previous browser deadline and is being investigated without skipping assertions.

## Pre-send clarity and README architecture

The real editor previously omitted execution settings before Send. A noninteractive
notice now distinguishes a saved direct/review preference from an unconfirmed
setting (`settingsKnown` and no `settingsError`), describes its consequence or
Settings recovery, and explains that the Relay's setting on first acceptance
determines the task mode. It neither changes settings nor prevents local saving.
The short ordinary saved feedback no longer promises a cached mode. Chinese
project permission copy now retains the English execution-setting qualifier.
Collapsed model summaries use localized effort meanings; request values remain exact.

An independent non-exported editor probe uses only in-memory project/model samples,
no import, preferences writes, API, task save, pairing or outbox access. It labels
the note as unsaved and never activates Send. Baseline checks had two passes and
one missing-cue failure. Initial/Send PNGs were identical despite the old visibility
check: pre-draw alone was too early for screenshot evidence. Those original PNGs
are retained and rejected as Send proof. Capture now waits two native frame
callbacks and idle; visibility intersects the physical screen, root and scroll
viewports, and current captures visibly include Send.

Final API35/320×640dp/200% font matrix: 18/18 motion-off (37.070s), 10/10 affected
motion-on (47.581s). Normal-text matrix: 1/1 with six EN/ZH known/unknown variants,
9.264s. All six normal-text Send PNGs were inspected; 52 unchanged PNGs retained.
The editor checks typing, model/effort selection, disclosure, exact values and
scrolling. JVM24/24, release lint0errors/28warnings and builds passed. Original
device settings were restored/read back and compared. These tests do not prove
real persistence, receipt, IME/TalkBack behavior or physical performance.
Private evidence: `.local/ux-oct5-share-editor/` (audit, originals, logs, hashes).
Final debug APK SHA-256: `24bf90b85c0c3ec4d74ca2ae904d5f747da595ba39f6bc7050604ad91c946c6a`;
ShareActivity: `768f2738728234c2db22c10fb087205917b0c9f65994e8899adf010f15fd57e1`.

Anonymous GitHub README inspection confirmed the new three-step order in both
languages. The English Mermaid embed failed in that session while Chinese
rendered; this is not a claim of invalid Mermaid syntax. Both now use original
system-font SVG diagrams with EN/ZH light/dark palettes, alt text and visible
text mappings. All four were rendered and inspected. No native media was edited;
the diagrams are schematic, not real task evidence.

### CI follow-through

The brand fixture left status-only response bodies unconsumed. Miniflare's own
body-consumption check reproduced that defect; consuming responses preserved all
original payload, authorization, exact-byte and privacy assertions. Local224/224
passed. Source4a71f43 [CI37349705478](https://github.com/MrMaii/droprun/actions/runs/37349705478)
passed brand and Android but had three real-browser20s DevTools startup failures
(221/224, no skips/cancellations). The precise OS/resource cause remains unproven.

CI now assigns the four real-browser files to a dedicated Windows runner at one
file at a time; the remaining34 are enumerated dynamically at four. Missing or
duplicate file assignments fail; browser availability/version and runner resources
are recorded. No production timeout, screenshot or assertion changed. Source7b7ffee
[CI37351685649](https://github.com/MrMaii/droprun/actions/runs/37351685649) passed
core210/210 and browser14/14 with zero skips/cancellations, Android builds/tests/lint,
audit, generated-site/public checks and Windows installer/portable construction.
This run predates the current editor/diagram source publication; their matching
run is recorded separately. The downloadable0.5.2-rc.1 remains unchanged.
