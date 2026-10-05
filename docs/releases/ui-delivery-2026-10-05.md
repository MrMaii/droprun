# Delivery clarity and action hierarchy — October 5, 2026

These source refinements are newer than the downloadable
[0.5.3-rc.1 candidate](0.5.3-rc.1.md). Its packages, tag and dated website/README
media are unchanged. This record describes an independent local UI diagnostic,
not a genuine returned task or a demonstration of the current release package.

## User-facing changes

Inspecting the result is the primary action. A ready preview retains the lime
“Open preview” button. Other terminal states emphasize “Screenshots & delivery
files”; “Follow up” becomes secondary. Active tasks keep their existing approval
and execution actions. The styling changes no permission, URL, cancellation,
preview-reopen or follow-up eligibility.

A terminal task without a preview now says none was attached and points to the
files entry. An unavailable preview does not claim to be live or expired. It
points to files only when that entry actually exists: a terminal task or a
nonempty report. An active task with no report simply says the preview is
unavailable. Reopening a snapshot does not acquire a new blanket online-PC
requirement. The existing server checks remain authoritative.

## Local evidence

The non-exported debug fixture renders actual TaskActivity code over readonly
memory. Its preferences, files, credentials, API, services, pairing, task save,
outbox and business navigation/actions are guarded. The sample visibly says
“UI probe · memory only · no work sent”. No task is imported or submitted; no
preview, files or follow-up button is activated. Instrumentation alone writes
diagnostic screenshots.

The final native run passed **4/4 methods**, in 15.038s (host 16.0454176s):

- 78 semantic configurations across English/Chinese, terminal/active states,
  missing/unavailable/ready/reopening/expired/stopped previews, live/snapshot,
  cancellation and missing-report boundaries. Actual enabled-state colors and
  elevation verify one primary delivery action and secondary follow-up. Busy
  Open, Files, Reopen and Follow up remain disabled.
- Six normal-text delivery layouts: both languages × missing, unavailable and
  ready snapshot. Text is complete without ellipsis. Controls intersect the
  actual ScrollView viewport and physical screen in full, after native window
  focus, settled entrance, two native frames and idle.
- Four system-200%-text configurations, with 12 individual reachability checks
  for preview detail, Files and Follow up. Each is scrolled into view separately;
  buttons are complete and at least 48dp. This does not require the whole region
  to fit one enlarged-text screen.

The six raw PNGs are 320×640, uncropped, unretouched and unscaled. Device/local
hashes match. Root inspected every image: Files is lime without a ready preview;
Open is lime for a ready preview; Follow up is white. Real scrolling puts some
or all of the top bar outside the screenshot in several cases. These captures
verify the delivery region, not whole-page/top-bar completeness.

Normal-text screenshots use fontScale=1 only in the diagnostic Activity's own
base context. The emulator's system fontScale stayed 2 before and after. API35,
320×640/160dpi and original animation/IME settings were unchanged. Forbidden
actions were zero, and no new ANR/crash line appeared in the final run's logs.
No physical-phone, performance, TalkBack or real-keyboard result is implied.

The corresponding debug/test builds passed in 45s. JVM24/24 passed with zero
failures/errors/skips; debug lint reported 0 errors/30 warnings. Eleven source
hashes and readonly copies were unchanged through build/native verification.

| Final artifact | SHA-256 |
| --- | --- |
| TaskActivity.java | `d42a0f71a6dadc2c07f81b9bb1f6f7ea532999043f1edd32317af900908b75ca` |
| TaskPreviewFeedbackTest.java | `ea1fe31b518582d125c63a0da7260791867b1e4fac8fecf2f61f10858c81fca6` |
| Debug APK | `8813534efa3d7f474cb5caa97db9421a02580d17d451448b6d149e88740f6cef` |
| Test APK | `7330a66e8cf05904d251fabb7dd76c619b955c3169d6b67a62cc2c7a74f95ee6` |

Private originals are under `.local/ux-oct5-task-preview/`: final build/native
records, readonly source/APKs, all six screenshots, the prior four baseline
screenshots and the 60-file hash index. The baseline files remain unchanged.

## Preserved failures and limits

The first 2/2 native checks failed because the test finder omitted ReportText's
single trailing LF. It now accepts exact text or exact text plus that LF; no
partial match replaces an assertion. A second finder-only build was not run.

The third diagnostic creation attempt failed with
`IllegalStateException: getResources() or getAssets() has already been called`:
the fixture tried applying its configuration too late in onCreate. It completed
zero methods and produced zero screenshots, then reported `Process crashed.`
Original sources/APKs/event logs remain. Moving only the independent fixture's
configuration into its base context resolved that creation error. It is not
counted as a product layout failure or a passing run.

The fourth run passed 3/3 in 9.183s before the button-emphasis change. Its four
raw baseline screenshots and separate APKs remain historical. Two host preflight
script mistakes stopped before their respective install/native operations; their
original errors remain. No timeouts, device settings or product behavior were
changed to hide these failures.

Real files, preview renewal, source-App return, genuine handoff/delivery,
physical-device accessibility/performance, installation and process-loss gates
remain open. This work does not retry rejected operations, modify the owner's
Cloudflare instance or establish stable release/marketing readiness.
