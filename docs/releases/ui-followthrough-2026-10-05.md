# UI follow-through — October 5, 2026

These source changes follow the published **0.5.2-rc.1** candidate. They are not
in its existing download files. Package evidence remains in the
[candidate record](0.5.2-rc.1.md); no release tag or asset was replaced.

## What changed

Android's default keyboard-focus highlight tinted an entire focused ScrollView
or share root. The earlier large-text screenshots contained approximately12%
lime over the content; Home's focused settings button did not recolor its page.
Screenshot pixel samples and native focus XML agree with the
[Android focus behavior](https://developer.android.com/about/versions/oreo/android-8.0-changes#input-and-navigation)
(checked October5,2026). Font size does not select a different palette.

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

- Native API35, dedicated DropRunTest emulator,200% font,320dp wide:8/8 focused
  checks passed with animator/window/transition scales all zero. Three cover page
  colors, overlay-root colors and localized button focus plus actual Enter
  activation; two cover Settings focus/visibility; three cover manual pairing.
- Pairing checks exercise English/Chinese, invalid Relay/instance/code/link,
  retained input, complete visible error text and valid destination normalization.
  The independent fixture records valid destinations locally and forbids pairing
  and network requests. No connection confirmation was sent.
- A further4/4 capture run at720×1440,density360 (320×640dp),200% font and motion
  off saved six unmodified native PNGs. Settings and EN/ZH validation views were
  visually inspected; these are internal probes, not public marketing media.
- `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest`, `lintRelease`
  passed. JVM24/24, no failures/errors/skips; release lint0errors/28warnings.
- Original device settings were restored and read back:320×640,160dpi,font2.0,
  animator unset, window1, transition1. Existing share drafts were not removed.

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
