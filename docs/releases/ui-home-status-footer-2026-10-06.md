# Home status and readable dates — October 6, 2026

Home cards now update their status text and translucent fill together. A reused
card can move from delivered to active, attention and delivered again without
keeping the previous grey background. The existing attention-first priority stays
the same. At large text sizes, the date sits below the status and wraps naturally;
normal text retains the compact horizontal footer.

Only two production statements change in `MainActivity`. Project identity,
counts, ordering, pending entries, navigation, focus and reading-position behavior
remain outside the patch. The fill uses the existing History drawable setter.

## Verification

- First red: two failures in 2.705s. One exposes the vertical-layout problem;
  the other is a test assertion that incorrectly included TextView's automatic
  vertical gravity. Two configurations entered; none completed; both closed.
- After correcting only the two horizontal-gravity assertions, a fresh red run
  has two target failures in 2.596s: stale active-state fill and horizontal
  large-text footer. Its original logs and APKs remain preserved.
- Applying the two production statements gives two passing methods in 9.953s.
  All eight English/Chinese, light/dark, normal/double-text configurations complete
  and their Activity windows reach `DESTROYED` before the next launch.
- JVM: 24 passed, no failures, errors or skips. Lint: zero errors, 30 warnings.
- The red2 and green test APKs are byte-identical. Production APKs differ as
  expected. Of the preceding 113 sources, 112 remain byte-identical; the new test
  makes 114 sources. All four Gradle configuration files remain unchanged.

Normal-text checks reuse one attached holder through four status states. Large
text checks cover three rows with long dates, complete names, stable identity,
counts, focus and a visible reading anchor. Dates remain individually reachable
inside the ListView and window frame; project targets retain at least 48dp.

## Evidence boundaries

The test explicitly opts into the existing nonexported, memory-only Home fixture.
It checks zero forbidden actions and synchronization calls. There are no business
clicks, screenshots, real Store writes, network requests or OS setting changes.
The old long recovery and process-loss routes are not entered.

This is local development UI evidence, not a physical performance, TalkBack,
keyboard, motion-off or signed-package runtime check. Normal-layout pixel equality
was not recaptured. Existing dated screenshots retain their original scope.

Production source SHA-256:
`d805884d9bb5ab802cbf0fa83cc95d6d7838acb17ad42ecd4abba9108d2345a2`.
Green development APK SHA-256:
`c879773cc9c8f4d384beda20364e246ecfd6ecf140196a3b5dfd66176126d237`.
The signed [0.5.7 candidate](0.5.7-rc.1.md) remains a separate immutable release.
