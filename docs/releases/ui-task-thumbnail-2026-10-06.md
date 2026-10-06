# A usable thumbnail entry — October 6, 2026

The existing delivery thumbnail now has a minimum 48dp-high action area and
follows the page's busy state. This is a bounded source change after the
[delivery-surface grouping](ui-delivery-surface-2026-10-05.md), not a new public
package or a genuine screenshot-delivery demonstration.

## Problem and minimal fix

The previous `ImageView` used `adjustViewBounds` with no minimum action area.
A valid 1200×40 bitmap rendered as a 240×8dp clickable strip on the actual test
layout. Its callback and keyboard-focus eligibility also remained active while
the delivery buttons were disabled. The affected real scenario is a delivered
task with a screenshot while deleting the record or renewing its preview.

A small `FrameLayout` now owns the existing action, description, background and
press feedback, with minimum height 48dp. The child keeps `adjustViewBounds` and
`FIT_CENTER`, centered without changing its aspect ratio. The ordinary screenshot
height remains natural; screenshots are not forced into a 48dp crop. The child
does not introduce a second accessibility stop. The parent is disabled and
unfocusable while busy, and its callback explicitly checks the current busy
value, including a stale view activated before rendering catches up. Idle state
restores the existing delivery-files entry.

Only this thumbnail branch in `TaskActivity` changes. Source download, hash
verification, selection of the first image, file opening, preview permissions,
reports and other controls are unchanged.

## Native reproduction

The existing non-exported preview fixture gains an explicit `thumbnailProbe`
intent flag. It creates only a 1200×40 or 600×400 memory bitmap with three color
bands. Under that flag, the delivery callback increments a memory counter. Its
default behavior still forbids navigation, preserving the older fixture. API,
credentials, saved tasks, pairing, cache/outbox, business files, service starts
and preference edits remain guarded. The bitmap is recycled on fixture teardown.
No actual download, file verification, business operation or navigation occurs.

- The initial three-method native run failed two methods in 8.919s. The narrow
  target measured **240×8dp**. Busy remained enabled/focusable, and stale/current
  callbacks produced two extra memory increments. The existing-size 200% text
  target scenario passed. Original sources, APKs, log, PNG and JSON remain.
- A separate trial on the **same installed APK** called `setMinimumHeight(48dp)`
  on the actual `ImageView`. It still measured 240×8dp and failed 1/1 in 2.073s.
  This failure and a separate raw capture are retained. It established why the
  small action container was needed; the failed trial was not a product change.
- The final build passed in 30s: JVM **24/24**, zero failures/errors/skips;
  lint **zero errors / 30 warnings**. All 99 source hashes matched before/after.
- The final native run passed **5/5 in 41.098s** (host 42.4799723s): three new
  thumbnail methods and the unchanged existing preview-state and delivery-surface
  methods. It did not run the entire application test suite.

The new tests cover EN/ZH and light/dark: narrow/ordinary image geometry and
uniform image-matrix scaling; idle, busy and restored action/node state; zero
stale/current busy callback dispatches; activity recreation with an idle memory
image. At system fontScale 2.0, the thumbnail and files button remain individually
reachable, with complete files-button text, before/after recreation. This does
not claim persistence of a genuine in-flight Task operation across recreation.
The existing two methods retain preview/reopen eligibility and the result-card
grouping across their original state/theme/text cases.

## Actual image evidence

Twelve final raw 320×640 PNGs show the native result region with a visible
“UI probe · memory bitmap · no task” marker or its Chinese equivalent. Eight are
idle narrow/ordinary images across both languages/themes; four show the ordinary
image in controlled busy state. All twelve were visually inspected. The marker
and synthetic bands are deliberate; these are not returned Codex screenshots.

In all final normal-size captures, the narrow image remains **240×8dp**, centered
in a **240×48dp** action area. The ordinary image and action remain **240×160dp**.
No full-page claim is made: some text and controls lie below the viewport. Busy
is set only in memory for this UI probe; screenshots do not prove server work.

All 28 files (two original PNG/JSON pairs plus twelve final pairs) have matching
device/host SHA-256 hashes; guards report zero forbidden actions. Private original
sources, logs, APKs, measurements and images remain under
`.local/ux-oct6-task-thumbnail/`, with `screens-red/`, `screens-minimum/` and
`screens-green/`. The test source and test APK are identical from red through green;
only `TaskActivity.java` changed between their 99-file source snapshots.

Device settings remained API 35, 320×640/160dpi, system fontScale 2.0, and the
original animation/IME settings. Normal-size captures use only local Activity
fontScale 1. Crash-buffer bytes match. Five new `Unable to freeze binder` events
are retained, with no new ANR/FATAL observed. No reset, process kill, data clearing,
IME change or previously rejected workflow was used. The device lease was released.

## Frozen identities

| Local artifact | SHA-256 |
| --- | --- |
| `TaskActivity.java` | `5a2d17ed8ffa7f55d692b504824e252fc95aa384536c11c778b09c38e6261a6c` |
| `DemoTaskPreviewActivity.java` | `2c387e33cc925457fddec8409f194401e21e4eba02462f6c50e833cda6b92d2e` |
| `TaskThumbnailTest.java` | `0782d45f6f73af1a09f6b63ef4d89460b6e289c82e6eeab0e669417d589c28ec` |
| Red development App APK | `8da622b31e5532e3784747ca113014c9fe3ab1301ca8d1b37350fd6d01415ddf` |
| Green development App APK | `083cad4e924ce59f38115ad765111a3a5e8c2fff857a1b60509b2ef35cc82805` |
| Unchanged test APK | `ca79c79e4c98e12826b202f33ced0cbebaa2951af3eb30fed5af9bbecb9ced40` |

Physical touch/TalkBack, actual download/hash verification, real navigation,
real decisions, performance and release installation remain outside this check.
Existing real-device/real-source launch gates are unchanged. There is no version,
public package, website, README or marketing-readiness change in this pass.
