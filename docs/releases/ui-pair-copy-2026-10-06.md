# Manual pairing copy — 2026-10-06 UTC

The manual form now states that the link must be blank to use the three values.
Errors follow the actual trimmed nonempty-link versus manual-values branch. A
bad link never silently falls back; the link error names clearing it as the way
to use the retained manual values. Nothing-sent / entries-kept assurance remains.
Strict parsing, inputs, inline error, scroll-to-error, polite live region and
separate server-source confirmation are unchanged. Product change is two lines
in PairActivity.java; the existing ManualPairValidationTest is the only test edit.
DemoManualPairActivity is unchanged, non-exported, local-confirmation-only and
rejects pairing/API/network requests.

## Actual verification

- Red: 3 methods, 1 passed / 2 ComparisonFailure, 7.103s. Failures show old generic
  copy in the manual and nonempty-link paths. The methods fail in their first EN
  iteration, so this red is not separately claimed as four-language screenshots.
- Green: same 3 methods passed, 8.516s. EN/ZH each exercise three invalid manual
  fields, bad-link-plus-valid-manual-values, retained-input repair by clearing
  only the link, and original valid manual/link targets. Guards remain zero.
- Product JVM: 24 tests, no failures/errors/skips. lintRelease: 0 errors / 28
  warnings. These belong to the green product source.
- Green capture: all native layout assertions passed, but two manual PNGs caught
  the native window entry with bottom-page text showing through. All four raw
  files remain in screens-green; the two manual files are rejected as settled
  stills. No device animation change was made.
- Capture correction: one captureUi-only native waitForIdle(500,3000) before the
  existing screenshot call. This is not a product delay or a new mock system.
  Native 3/3 passed again, 14.032s; four settled EN/ZH manual/link PNGs in
  screens-capture-settled visibly show the complete error and reachable buttons.
  Their device/host hashes match exactly. No image edits/cropping were performed.
- Final 99 source hashes match the last frozen build and current Android tree.
  Client snapshot to red changes only ManualPairValidationTest; red to green only
  PairActivity; green to settled capture only the AndroidTest capture helper.
- Four read-only device snapshots are equal: emulator-5554/API35, 320x640 pixels,
  160dpi, fontScale2.0; animator null / window1 / transition1 and original IME
  settings remain. Crash buffer bytes unchanged. Six new binder-freeze error
  lines remain in final-verification.json; no new ANR/FATAL line observed. Their
  cause is not established and this is not a physical performance claim.
- Local verifier first rejected mixed rg path separators rather than a source
  difference. verification-first-failure.txt retains the original failure and
  diagnosis; path normalization fixes only the private evidence verifier.

## Final identities

PairActivity.java SHA256:
`290ce08a236e8ce0427626a657d08d966cfd88d4d270c44db19bf1f145e5a55e`

ManualPairValidationTest.java SHA256:
`b638a41c6939a1a6edd15220b53eb43847440c227f5225aff489f1280a3657dd`

Final development App APK SHA256:
`6b7d3f31558e6ade502f89e040fabaf4fbc6e2a03dfbd02b97f019823f1371c8`

Final development test APK SHA256:
`64a4e1f14217819639a06c19eb0d89e136af0cf72650639a303627d10eb62514`

The product App APK is identical across green and capture-settled; only the test
APK changes for capture synchronization. Public candidate assets were not built,
replaced or published by this subtask. Root owns docs/Git/publication.

## Limits

No genuine pairing or network request, ordinary Pair navigation, IME typing,
draft cleanup, process kill, camera flow, physical performance, TalkBack or dark
theme acceptance. The help caption is source verified; the accepted scrolled
error-region stills do not show it. Existing capture filenames export the last
manual-invalid-code sample per language; six invalid-manual cases are native
assertions, not six distinct PNGs. Prior normal-Pair/draft restrictions remain.
The final AndroidTest-only capture change was recompiled/reverified natively;
product JVM/lint were not repeated after this test-only edit.

Original logs, three build/source snapshots, development APKs, all eight PNGs,
device reads, and verifier failure remain private. final-verification.json and
final-source-closure.json own exact counts, hashes and boundaries.
