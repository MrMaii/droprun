# Project history and disclosure motion — local verification

Date: 2026-10-06 UTC. Source work after 2cbcb34; not included in the signed
0.5.7-rc.1 candidate. Native Java Views and existing business behavior remain.

## Changes

- Separate the project name from Back/Refresh navigation: 24sp, two lines normally
  and three at fontScale >= 1.5. Keep the full accessibility description; a 48dp
  information control opens the complete name and ID. Very long names deliberately
  ellipsize in the fixed header. Same-name ID hints remain.
- Recycled task cards update the existing status-pill background and text from
  the same status color. Labels, counts, polling, paging, removal, callbacks and
  scroll-anchor logic remain unchanged.
- Common disclosure arrows use 180°/270° endpoints, shortening the requested turn
  to 90°. Only two numeric targets changed in Ui; timing, expansion, callbacks,
  accessibility and the existing motion-off path remain unchanged.

## Verification

Four separate native runs are retained, including every diagnostic failure:

1. Red: two expected failures in 9.338s. The project title had 164dp and seven
   ellipsized characters; a running card retained the old grey background.
   Arrow samples ranged from -90° to 180°.
2. First green: 1/2 in 13.819s. Motion passed; History failed a test that mixed
   dialog-local and screen coordinates. Only that test comparison was corrected.
3. Second green: 1/2 in 23.428s. Motion passed with state and visibility checks.
   History then failed the test's linear SP conversion at font2. The test now
   uses the platform TypedValue.applyDimension conversion.
4. Focused History: 1/1 in 78.119s. Motion was not repeated; its method and helper
   are byte-identical to the passing second-green snapshot. This is not a final
   same-batch 2/2 result.

History covers EN/ZH × light/dark × Activity font1/2, normal and extreme names,
three synthetic rows, same-name/revoked-catalog identity, full dialog ending,
Close visibility, recycled status tint and preserved refresh focus/read anchor.
The title column is 232dp; normal suffixes fit in all eight configurations.
At font2, normal EN/ZH list viewports are 243/287dp, retaining a whole card.

Motion covers eight phase traces across EN/light and ZH/dark: opening, closing,
initially-open closing, and reversal after observing an interior angle. Actual
timestamped samples stay within 180°–270°, uninterrupted phases are monotonic,
and endpoints, body visibility, state and callbacks match. These are sampled
property reads, not all rendered frames or a frame-rate/physical-performance test.

Build-green JVM: 24 passed, no failures/errors/skips. Lint: 0 errors, 30 warnings.
Later builds correct tests only; the production/fixture app APK remains identical.
All 107 source hashes match the focused build; 102 baseline sources, four Gradle
configs and guarded History behavior segments remain unchanged. Ui differs only
at the two endpoints. 63 transferred files match device hashes; 778 original
files are sealed read-only. The earlier 670-file Material archive is unchanged.

## Capture and acceptance scope

All 24 accepted raw 320×640 PNGs were inspected; 16 main-page frames retain readable
memory markers and system bars. Four font2 information dialogs cover the background
marker, so dialog frames remain private evidence, not public product illustrations.
No cropping or repainting. The fixture rejects business reads/writes, APIs, sending,
pairing, removal, services and navigation; guards remain zero. Device settings and
crash log are unchanged. Filtered system event differences are preserved separately.

No genuine tasks, process-loss retry, IME, OS setting change, TalkBack, reduced-motion
runtime or physical-device acceptance was performed. The current candidate and its
remaining acceptance gates stay in [the release record](0.5.7-rc.1.md).
