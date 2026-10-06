# Home and delivery: a continuous reading layout

2026-10-06. Unreleased native development UI; signed 0.5.8 downloads stay unchanged.

Home uses continuous project rows with light separators. Names, counts and state
share the page edge; the former gap inside each card is reduced. State pills,
complete project identity, stable IDs, click targets and reading anchors remain.
Delivery gives the result and its existing actions the page width: a quiet 12sp
section label, an 18sp summary and the original full-report disclosure. Its
background comes from the opaque page, keeping the reading surface continuous.

## Actual checks

- Home: focused native 1/1, 18.097 seconds. Four normal-font EN/ZH light/dark
  windows and EN/light, ZH/dark at 200%; all six closed and confirmed DESTROYED.
  Name width is measured at 264dp on this 320dp viewport. Twelve observed guard
  rows remain zero. Thirty-six local rebinds retain identity, counts, actual focus
  and reading offset; state text/tint and large-font dates remain consistent.
- Final delivery: capture-only 1/1, 12.340 seconds, four normal-font windows;
  separately, reading/plan-eligibility 1/1, 3.715 seconds, two actual 200% windows.
  All six have known destruction. Complete sample summary/report, preview/file/
  follow-up targets and plan approval/rejection eligibility are individually
  reachable; busy disables the plan actions. No action is activated. Actual
  font2 forbidden/thumbnail-open counters remain zero.
- Eight accepted raw 320 × 640 originals were directly viewed after two native
  pre-draws and two-second settling. Original sample markers and system glyphs
  remain. All 17 final PNG/JSON/result transfers match device hashes; brand and
  landing copies are byte-identical. These are initial viewports, not full pages.
- Final build and 24 JVM tests pass; lint has zero errors and 30 existing warnings.
  Android configuration/version files stay unchanged: debug 0.5.8-dev/code21.

The first draft build failed at two test-only CharSequence/String selectors;
adding `.toString()` corrected them. Initial delivery functional/capture runs
passed, but visual review found an unnecessary background rectangle. Those four
stills remain private and unaccepted. Removing that one fill produced the final
captures and separately repeated delivery checks above. Home production/tests
remain byte-identical through the subsequent build; its completed run is not
reported as repeated. Original diagnostics are preserved.

Only nonexported guarded memory fixtures run. No genuine task, preview, file
opening, API, saved data, IME or OS setting is used. This does not establish
physical performance, TalkBack, continuous motion, process recovery or real
handoff acceptance. Unchanged approval/expiry/reopen/thumbnail business behavior
outside the UI line is source-reviewed, not additional runtime coverage.

## Normal stills

| Language | Home | Delivery |
| --- | --- | --- |
| EN/light | [PNG](../../assets/brand/source-home-rows-en-light-20261006.png) | [PNG](../../assets/brand/source-delivery-reading-en-light-20261006.png) |
| EN/dark | [PNG](../../assets/brand/source-home-rows-en-dark-20261006.png) | [PNG](../../assets/brand/source-delivery-reading-en-dark-20261006.png) |
| ZH/light | [PNG](../../assets/brand/source-home-rows-zh-light-20261006.png) | [PNG](../../assets/brand/source-delivery-reading-zh-light-20261006.png) |
| ZH/dark | [PNG](../../assets/brand/source-home-rows-zh-dark-20261006.png) | [PNG](../../assets/brand/source-delivery-reading-zh-dark-20261006.png) |
