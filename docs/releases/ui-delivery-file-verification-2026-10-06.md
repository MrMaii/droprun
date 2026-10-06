# A quieter file preview — October 6, 2026

File contents now follow a compact **Verified file · size** disclosure. Opening
it reveals the complete selectable SHA-256 and the existing explanation that the
preview does not execute the file. The original permanent technical card no longer
pushes content down the page. Closing it restores the compact view.

This changes four UI regions in `DeliverablesActivity`: the expansion preference,
its Bundle restore/save, and the evidence card. Seven protected business spans
remain byte-identical: cache recheck, cache restoration, list rows, download,
inline rendering, Save/export/Back and cleanup. No dependency or framework changes.

## Verification

- The expected red test failed in 1.943s: one configuration entered, zero complete
  configurations. The original diagnostic remains alongside its counter correction.
- The first green test passed in 16.568s across English/Chinese, light/dark and
  normal/double text size. JVM: 24 passed; lint: zero errors, 30 warnings.
- A capture-only successor added the established two-second compositor wait after
  two native pre-draws. The same eight configurations passed in 63.783s. Its
  production source, fixture, manifest and app APK match the first green build.
  JVM and lint were not rerun for that test-only wait.
- All 24 final PNG/JSON pairs and 48 device-to-host hashes match. The owner viewed
  every original; an independent reviewer inspected eight representative originals
  and checked source, metadata and transfer hashes. Earlier inconsistent capture
  frames remain preserved and unaccepted; no pixels were repaired.

Checks cover the collapsed/expanded hierarchy, full selectable hash, original
explanation, a focusable 48dp disclosure, Save/Back enabled state, collapse after
UI recreation and individually reachable large-text content. The UI preference
survives `ActivityScenario.recreate()`; this is not a physical rotation or process
recovery test.

## Evidence boundaries

The new debug fixture is nonexported and requires an explicit opt-in. It uses a
guarded memory context. A synthetic zero-length file throws before a filesystem
path reaches the renderer; the existing unsupported-format fallback is displayed.
Only the disclosure is clicked. Network, credentials, task submission, download,
cache, Save, provider/picker and navigation are forbidden.

Consequently this verifies **fallback UI, not actual download, SHA verification,
decoded contents or cache recovery**. Twenty complete-marker frames are eligible
only as labelled synthetic UI samples. Four large-text expanded-tail frames stay
private because their marker is partly or wholly outside the viewport.

The checked development app SHA-256 is
`a63be969d30710c624f13efed4be9dd25ac91cdcffdf1af4cddd1338b44cbca7`.
Production source SHA-256 is
`1fa471af65a9c02e37dc493027ae2054220bfb81457abb60e1d990c7ef5f9c1a`.
The signed [0.5.7 candidate](0.5.7-rc.1.md) remains a separate immutable release;
this record does not establish real-device or end-to-end acceptance.
