# Matte primary actions and aligned task headings — local UI

Recorded October 6, 2026. These are unreleased development changes, separate
from signed candidate 0.5.10. Genuine handoff and physical acceptance remain open.

## Changes

Primary actions use one lime surface without the persistent 2dp shadow. All
existing pressed, focus and disabled colors, outlines, sizing and press binding
remain. This applies to every shared primary button. Container/sheet depth stays.
Task disclosure headings now follow the result's reading edge; their 52dp target,
meaning, expanded state, callbacks and motion remain.

## Actual verification

One offline debug build succeeded in 1m26s, with 127 frozen native sources and
four Gradle configurations, SHA-256
`c6399210541ca3dcf0b8321b35d239a0d08fdb0f3b59970adbeb38db841f4d93`.
Only Ui elevation and Task header padding differ from the prior freeze; the
other 125 sources and four configurations are byte-identical.
Metadata: 0.5.10-dev/code23. JVM: 27 tests, zero failures/errors/skips.
Lint: zero errors, 30 existing warnings.
App APK `61ad232d05b46117f887f67355060d75e0ac64a861c8e0839f9ec6f341e17a49`;
test APK `f93ba91f234b9e80cf67bc7cb6fc72ddfc5f2bfbd9dae2cf420d4f7a3b74c2d1`.

- Existing Share test: 1/1, 23.964s; six known destroyed windows, four normal
  EN/ZH light/dark captures and two font2 destination-reading windows. Complete
  long labels and Send remain reachable; 19 lines, 32 ID-hint characters and 47
  eight-counter guard snapshots are verified. This is fixture layout, not a
  real project-catalog integration.
- Existing Delivery capture test: 1/1, 17.713s; four known destroyed windows,
  four raw images and four actual forbidden-action counter samples at zero.
  Other zero business/navigation fields describe source non-invocation rather
  than measured callbacks. No business action is invoked by these tests.

Installed APK hashes and ACKs match the build. Seventeen device-produced files
(eight PNGs, nine JSONs) match device/host hashes. Device settings and source
closures match before/after; no OS, IME, font or animation setting was changed.
Native report SHA-256:
`da18cd3d80d241b84cd1f1a6bd5c2cb8a920afbb910e9be379e929d6497cc2ad`.

## Raw media and limits

Root directly reviewed all eight originals, including markers and system bars.
These brand copies are byte-exact. Primary actions are readable in both themes;
English Snapshot version aligns with the result. The Chinese version continues
below the first viewport. No retouched or fabricated App UI is used.
The bilingual README selects these new Share/Delivery images; its prior local
layout preview is documented in the [previous pass](ui-share-destination-feedback-2026-10-06.md).
The image-only substitutions keep 320×640 canvases and the 260px display width;
that earlier six-scene check is not relabelled as a fresh render of these images.

| File | SHA-256 |
| --- | --- |
| [source-share-matte-en-light-20261006.png](../../assets/brand/source-share-matte-en-light-20261006.png) | `82fb8e5caaca106681315f2c4e0dabd166c38dc77189b75409b9f26acafd296f` |
| [source-share-matte-en-dark-20261006.png](../../assets/brand/source-share-matte-en-dark-20261006.png) | `47130dda2b5b46b0124905a4a1d28c3930292fb39d3ebe8fce0931d33da2f1e8` |
| [source-share-matte-zh-light-20261006.png](../../assets/brand/source-share-matte-zh-light-20261006.png) | `708753da762e2fe32ab37a61628c7a02e58cf5503e16e1b4b150faec62000784` |
| [source-share-matte-zh-dark-20261006.png](../../assets/brand/source-share-matte-zh-dark-20261006.png) | `b9129e2751927b2a07cbeaa7b15128f5c00048225d036fdde10e71ebadbc52e7` |
| [source-delivery-matte-en-light-20261006.png](../../assets/brand/source-delivery-matte-en-light-20261006.png) | `0562ba46d380fb96a1c54855a931d45ac4c4fe6d5a90b79140f2c4dee22453a9` |
| [source-delivery-matte-en-dark-20261006.png](../../assets/brand/source-delivery-matte-en-dark-20261006.png) | `25a5525787c4b7534ecc6135adda2aee82bcf5adcc3192009fecfcb6daf8f3cc` |
| [source-delivery-matte-zh-light-20261006.png](../../assets/brand/source-delivery-matte-zh-light-20261006.png) | `d00b8e18c0eaecd02ea43e7114fb23b725d70a507daa0e8295168c172d11abd6` |
| [source-delivery-matte-zh-dark-20261006.png](../../assets/brand/source-delivery-matte-zh-dark-20261006.png) | `4f3a4c24b33466b8d3bce72b3f909616556871d1e7a1b02190a096e1bb9cee02` |

These checks do not measure press latency, spoken TalkBack, reduced-motion
playback, physical performance, signed-package pixels, genuine submissions or
installation-to-delivery acceptance. Source preservation of interaction states
is not an exhaustive runtime interaction test. Earlier failed records remain.
