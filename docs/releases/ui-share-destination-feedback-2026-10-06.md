# Share destination and failure feedback — local development UI

Recorded October 6, 2026. These changes are unreleased and are not part of the
signed 0.5.10 candidate. No complete handoff or marketing readiness is claimed.

## Changes

- The Share destination uses readable 15sp medium text above the optional note.
  The complete name wraps naturally; material, model, note and Send retain their
  existing behavior.
- Settings announces a settled execution-preference failure once, with the same
  operation, connection, attachment and lifetime guards used for success.
- A null or blank task-refresh error displays a localized retry notice. Existing
  cached results, useful error detail and the normal refresh schedule remain.

## Actual checks

One offline debug build succeeded in 38 seconds. Its frozen source set contains
127 native files and four Gradle configurations, SHA-256
`67f70b1f07b2f96c522034aa42af40b0816815c43016da0b91768bc61616aca1`. Actual metadata is 0.5.10-dev/code23.
JVM: 27 tests, zero failures/errors/skips. Lint: zero errors, 30 existing warnings.
APK `dd562b042b0c95954c761f2184fb17a75556c2749729139661174d243ce2b030`; test APK `f93ba91f234b9e80cf67bc7cb6fc72ddfc5f2bfbd9dae2cf420d4f7a3b74c2d1`.

- Settings: one opted-in test / 5.405 seconds; two fixed
  memory failures, two real TYPE_ANNOUNCEMENT events, two known destroyed windows.
  All 12 guard observations are zero. A repeated result display
  produced no second matching announcement within the measured duplicate window.
- Refresh: one opted-in test / 4.644 seconds; four active
  memory callbacks cover null, empty, whitespace and nonempty error detail.
  All four windows are known destroyed; all 20 guard observations are zero.
- Share: one opted-in test / 15.972 seconds; four normal
  EN/ZH light/dark captures and two 200% text reading windows. All six windows
  are known destroyed. 19 natural lines and 32 ID-hint characters were
  measured; all 47 eight-counter guard snapshots are zero. The two long labels
  occupy eight English and seven Chinese lines; Send remains separately reachable.

The same APKs were reused. Installed hashes, frozen sources and read-only device
settings match before/after the successful runs; no OS or IME setting was changed.
The first native owner stopped after the app install because its local path
regex omitted Android's `~` character. All 12 child processes had returned;
no test had started. Its original failed record is retained. The reviewed
continuation verified that app and installed only the test APK before running.

## Raw images

Root directly reviewed all four original 320×640 PNGs, including system bars.
The assets below are byte-for-byte copies, with the memory-only marker retained.
They show a preset project, example.invalid link and empty note; no Send action.
A separate read-only review of root's execution evidence checked all 81 returned
child-command logs and these four raw images. Its report SHA-256 is
`748f74e8865638bb8b00ae242bb84cc3b2fdeb6b49a1b2f9a0c85c180e715b55`.
The reviewer helped prepare the probes; this is an execution/pixel review,
not an independent audit of their implementation.

| File | SHA-256 |
| --- | --- |
| [source-share-destination-en-light-20261006.png](../../assets/brand/source-share-destination-en-light-20261006.png) | `721393aa7f52cca7abcef021391f99d9cb8d3173709a599661379be1671aa853` |
| [source-share-destination-en-dark-20261006.png](../../assets/brand/source-share-destination-en-dark-20261006.png) | `faa91a60eba9d1d2a70673ba6d96e8d47fc0aa73324a8bdadbc1fc22ef2ce39d` |
| [source-share-destination-zh-light-20261006.png](../../assets/brand/source-share-destination-zh-light-20261006.png) | `74cbd80698cf72e18eb09d5702c80e35253e5529aef1e8942c9a3b291037999a` |
| [source-share-destination-zh-dark-20261006.png](../../assets/brand/source-share-destination-zh-dark-20261006.png) | `66b0f664a1890ed95b95ba160c13b399d7ca6e700737609ffd26409758367f59` |

## README layout

The bilingual README now introduces the workflow and larger native screenshots
before the Share motion recording. Its value sentence uses an h2; prerequisites
remain separate. Recording parameters use a disclosure; memory-only provenance
and the signed-candidate download links remain visible and accurate.

Actual local previews cover English and Chinese at 320, 375 and 1280px, with
light appearance and reduced motion. All six have no horizontal overflow;
the three primary images use 260px display width and the existing raw pixels.
All six owned browser endpoints stopped normally. No external content loaded.
Root directly reviewed four hero/gallery captures. The local preview report
SHA-256 is `f71b48f370c7b8e599936d5465da6620cc74005312843b45a046dbd84ce0e0e0`.
This uses approximate GitHub document CSS, not the actual GitHub renderer;
dark rendering and full-motion playback were not measured in this pass.

## Limits

The long-label probe places real ProjectPresentation.label output into a memory
fixture; it proves complete-label layout, not real catalog integration. Settings
observes native announcement events, not spoken TalkBack. Refresh covers active
failure callbacks, not late paused/destroyed callbacks or success. These runs do
not establish signed-package pixels, physical accessibility, frame performance,
real submission, instance isolation or installation-to-delivery acceptance.
Earlier failed originals remain retained.
