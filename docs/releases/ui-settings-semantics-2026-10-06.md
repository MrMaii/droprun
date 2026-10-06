# Settings feedback and accessible option names — October 6, 2026

This bounded refinement follows the [settings lifecycle fix](ui-settings-operation-2026-10-05.md).
The audit and red reproduction began October 5 UTC; verification finished October 6.
Public 0.5.4-rc.1 packages are unchanged by this local work.

## Behavior

The inline execution-setting caption now uses the existing muted text color while
saving, the existing accent after confirmed success, and amber for failure or a
changed connection. Previously all three states used amber, so success looked
like the warning beside it. Color follows the operation branches; it does not
compare translated strings. Text, layout, requests, ownership checks, recreation,
disabled choices and once-only success announcements retain their behavior.

The existing `optionRow` accessible name now contains its visible title, nonempty
explanation and selected suffix. The explanation was previously omitted from the
parent action's explicit name. Empty/null explanations add no separator. This
also applies to the existing model picker; there is no new control or action.

No palette, typography, spacing or animation system was added. The existing
opaque card is the caption background. Relative-luminance contrast was checked
before choosing the accent and asserted against actual rendered text colors:

| Caption | Existing token | Light card | Dark card |
| --- | --- | ---: | ---: |
| Saving | `MUTED` | 5.600:1 | 8.903:1 |
| Saved | `ACCENT` | 7.940:1 | 11.597:1 |
| Failure / connection changed | `AMBER` | 5.782:1 | 10.328:1 |

These numbers cover the caption's configured colors, not physical display or
human readability certification. Words continue to distinguish states without color.

## Reproduction and verification

The existing non-exported `DemoSettingsRecreationActivity` uses guarded readonly
memory, controlled work and no background refresh. Its only fixture change is an
intent-selected light/dark appearance in that memory. It neither edits real
preferences nor calls a Relay, pairs, submits work, reads credentials, accesses
an outbox/cache or starts business services. Instrumentation writes diagnostic
images separately. Simulated success does not change the confirmed cached choice
and does not establish server receipt.

- Red: four new native methods ran in 20.722s; three failed on the expected
  missing explanation, amber Saving and amber Saved. The unchanged warning path
  passed. Original logs, source snapshots and APKs remain intact.
- First green build: 49s; JVM 24/24, zero failures/errors/skips; lint zero errors,
  30 warnings. All 98 source hashes matched before and after the build.
- Combined native run: **12/13 passed in 80.627s**. All nine previous busy/lifecycle
  methods passed, including announcement deduplication and 200% text targets.
  The three new tone methods passed EN/ZH × light/dark: pending, success and
  failure before/after recreation, plus changed-connection warnings.
- The sole combined-run failure was a new test helper: temporary model rows were
  not attached to a window, so Android returned an empty accessibility node. The
  actual visible execution rows had passed. That original failure is retained.
  Only the helper was corrected to attach/remove its temporary rows; the same
  node test also checks recreation. Two test-only compilation passes are saved;
  no application source changed after the combined run.
- Targeted rerun: **1/1 passed in 9.514s**, with 56 exact native-node description
  assertions across both languages/themes, before and after recreation. This
  is not reported as a single 13/13 run. No other native methods were repeated.

All final 98 source hashes match their frozen build snapshot. The only source
change after the combined run is `SettingsSemanticsTest.java`. Existing test
lookup helpers now accept the title followed by its localized separator; the
new node assertions check the complete expected name, including detail.

## Actual captures and device boundary

Twelve unedited 320×640 PNGs and twelve JSON records were captured during the
combined run. Names are `settings-{en|zh}-{light|dark}-{pending|success|failure}`.
All carry “UI probe · memory only · no request” or its Chinese equivalent. Each
caption and both execution choices are completely visible at local fontScale 1.
The marker can cover the Computer section label; the scrolled Settings topbar and
lower sections are outside this evidence. No whole-page acceptance is claimed.

Every raw image was visually inspected. Device/download hashes match for all
24 files; guards report zero forbidden actions. Private originals, JSON geometry,
contrast, sources, APKs and logs remain under `.local/ux-oct5-settings-semantics/`;
the images are in `screens-green/`. No image was replaced by the targeted rerun.

The emulator remained API 35, 320×640/160dpi, system fontScale 2.0, with unchanged
animation and IME settings. Normal-size capture configuration belongs only to
the diagnostic Activity. Crash-buffer bytes match before/after. Six new
`Unable to freeze binder` ActivityManager events are retained; no new ANR or
FATAL exception was observed. No reset, process kill, data clearing or rejected
workflow was used. The exclusive device lease was released after verification.

## Artifact identities

The application APK below is a local development build, not the public signed
download. Both later test-only compilations reused the exact same App APK bytes.

| Artifact | SHA-256 |
| --- | --- |
| `Ui.java` | `5f8a85df42bb24c85f8bbea01360094c05ab4f08e3098beba3d4877e1bde8d10` |
| `SettingsActivity.java` | `c4661c952cb96efaa5f5a8e894e7a44c617f56dec4109cd9cac0e5d70956ef05` |
| Final `SettingsSemanticsTest.java` | `81b6d631ec9102ae1e6026fb091c9475ff81232904ed63470686f9c6a8a4f7e4` |
| Development App APK | `c05ab1995cab804b5f245c1c9068deacc52c42a79c6fd80e7371b5c59fc27a7c` |
| Combined-run test APK | `302f6ac1af775f210c9e8b3d5305b963997f700d730d9f2696f65800c1f5423e` |
| Targeted-rerun test APK | `b727390469392f913b28fb2f0027ddf01728383fba1ddcb8f3b0ca8eb90e9483` |

Physical Android, real TalkBack navigation/speech, motion-off, IME, performance,
genuine settings receipt and release installation remain unverified. Native
node descriptions and local contrast checks do not close those gates. No version,
public package, README, website or release-readiness claim changes in this pass.
