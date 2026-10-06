# Clearer results and accurate action feedback — 2026-10-06

This pass improves the native Android reading experience while genuine handoff
acceptance remains open. The public signed 0.5.9 candidate is unchanged; source
for the next 0.5.10 candidate includes these refinements.

## Product changes

- Use a 24sp task title and a clear 18sp result heading. Keep the result at the
  page's reading width, with 18sp body text and the existing action order.
- Preserve original result-list lines, normalize inline spacing and retain the
  existing first-paragraph/200-character boundary. Reuse the report renderer for
  existing bullets. Do not invent results or change compact history clipping.
- Place ready-preview explanation closer to its action. Keep its meaning,
  selectable text, preview permission and callback unchanged.
- Classify task actions by whether work threw an exception. Null/empty/blank
  error text cannot become a success confirmation. Keep nonblank original detail;
  otherwise ask the user to check the latest handoff state before trying again.
  The existing callback restores controls and refreshes the foreground task.

## Actual local verification

One offline debug build completed successfully in 1m12s. The original 27 JVM
tests passed with zero failure/error/skips; lint reported 0 errors and 30 warnings.
The frozen build contains 123 native source files and four Gradle configuration
files, version 0.5.9-dev/code22. The subsequent version-only change to 0.5.10/code23
does not turn these captures into signed-package verification.

Debug app SHA-256:
`e27434f0f96e8ff17568714b6d986e705d4760f9be33e40aedfccaab3400fb0a`.
Instrumentation APK:
`0ef7f97296f7ca826dd68258fb1c6eb885958e4871496280d36ffe9000090e68`.

`TaskActionFeedbackTest#closedFailuresRestoreRealActionFeedback` passed 1/1 in
11.327s. Eight windows cover EN/light and ZH/dark, each with fixed null, empty,
whitespace and nonempty IOException text. They exercise the real TaskActivity
executor/callback: pending controls are disabled; settled controls are enabled;
the actual notice and error dialog show the expected failure; one synthetic work
and one memory refresh occur per window. All eight windows reached known
DESTROYED. Forty actual guard snapshots report zero forbidden operations.

The separate nonexported debug fixture accepts only its internally created,
one-use synthetic failure. It cannot accept an arbitrary callback, saved task,
credentials, API, file access, service or navigation. The existing
DemoTaskPreviewActivity.perform denial was kept byte-for-byte. The old false
confirmation/null-crash finding is static source evidence; no native baseline
was launched to crash the App. Toast events were not measured.

The same APKs were reused for visual verification, without another install or
build. `DeliveryStableCaptureTest` passed 1/1 in 12.274s: four normal-font
EN/ZH light/dark windows, all known destroyed, four raw capture guards zero.
`TaskReadingHierarchyTest` passed 1/1 in 4.487s: two font2 reading windows verify
the result, both original list lines, full evidence and plan actions remain
reachable. Their two final guard rows are zero and both windows are destroyed.
Device properties and full source/config closures match before/after each run.

Root and an independent reviewer directly viewed all four original PNGs.
Eight brand/website copies preserve their exact bytes and visible memory marker.
English shows Snapshot version in its initial viewport; Chinese continues below
it. This is not a claim that every page section fits in one screen.

[English/light](../../assets/brand/source-delivery-result-en-light-20261006.png)
· [dark](../../assets/brand/source-delivery-result-en-dark-20261006.png)
· [中文/浅色](../../assets/brand/source-delivery-result-zh-light-20261006.png)
· [深色](../../assets/brand/source-delivery-result-zh-dark-20261006.png).

## Boundaries and next detail

These are native memory-only UI examples, not actual Codex work, report delivery,
signed APK screenshots, TalkBack measurements or physical-device performance.
No real Relay submission, OS/IME change, reset/kill/clear or denied recovery route
was used. Earlier failed originals remain retained with their original limits.
Settings failure announcements and refresh null-error handling remain separate
details to address. Genuine installation/source/performance acceptance and final
marketing handover stay open.
