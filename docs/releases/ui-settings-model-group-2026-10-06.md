# Settings model group — October 6, 2026

Unreleased development UI, 0.5.10-dev/code23. The signed public candidate remains
[0.5.10-rc.1](0.5.10-rc.1.md). This pass changes only the model group's surface
and the corresponding geometry assertions in the existing reading test.
[ADR 0019](../decisions/0019-semantic-content-groups.md) owns the rationale.

## Source and build

The frozen closure contains 133 native files and four build configurations.
Only `SettingsActivity.java` and `SettingsReadingPresentationTest.java` differ
from the preceding Settings focus-fix closure.

| Binding | SHA-256 |
| --- | --- |
| Source inventory | `948820348c51d23772595ef9948a55a35f2fbf4e25dc2ec0b3c218ddb8364808` |
| Settings source | `728c021de0eb129fa0b43e0aa913c6d964b46b8954b8b181578fb981eb0ddbcb` |
| Reading test | `d7e6eb2201288fac20c5103df2831e37827a767060cd222bf009a7d0e2516f86` |
| Debug app, 2,165,142 B | `a83e88a9b888a971f8364a4ebadc9f7bca519685d3fcb0ada396fbeace023999` |
| Test app, 2,014,473 B | `2cc96cdf2ffc00b573fed03c16bd2c8a12f0e4609ed0cc5d679a404c3d5ef877` |

One offline Gradle build returned0 in34.515s. `assembleDebug`,
`assembleDebugAndroidTest`, `testDebugUnitTest` and `lintDebug` completed.
Actual XML reports contain27 JVM tests with no failures/errors/skips and
30 lint warnings, zero errors. Build stderr is133B of compiler deprecation
notes; it is not empty. Both installed APKs matched their build hashes.

## Actual local native checks

Nonce `6520338b-d760-40c9-a792-c5a0d9936b79`; DropRunTest API35,320×640,160dpi.

- Reading test1/1 passed, JUnit21.783s. Six known windows completed and reached
  DESTROYED: EN light/dark and ZH light/dark at font1, EN light and ZH dark at
  font2. All24 guard snapshots were zero. Complete preference nodes, transformed
  arrows,48dp targets, unique project identity and busy permission presentation
  remained checked. Font/language restoration was observed; this reading test
  does not observe full palette restoration or physical TalkBack.
- Refresh test1/1 passed, JUnit8.067s. Four known windows completed and closed,
  all25 guards zero. Nine memory read episodes completed through the actual read
  controller, eight UI callbacks and25 memory cache applies. Cached-failure
  keyboard focus moved237→243 to reveal the complete model row; touch reading
  stayed2795→2795. The pending known read drained after close without a late UI
  update; owned executors terminated. This test observed its language/dark/palette
  restoration. Mode-operation and announcement-count checks remain unverified.
- All45 ADB children returned0 with empty stderr. Ten device properties and the
  source closure matched before/after. No OS font, animation or IME setting was
  changed. Neither test performed a real task submission or preference save.

## Original screenshots

Root directly viewed all four320×640 normal-font PNGs, including system pixels.
Model rows were present in every capture. The filled group, availability,
Refresh, both choices, memory-only marker and first access row were readable.
Copies in `assets/brand` preserve every byte; no retouching or UI recreation.

| Brand copy | B | SHA-256 |
| --- | ---: | --- |
| `source-settings-grouped-en-light-20261006.png` | 42541 | `cfdb33ad3178ca8086881630f4b1fb8261aa097ef9cb8cd0df2ed8ccc90231b1` |
| `source-settings-grouped-en-dark-20261006.png` | 42645 | `7942934e496b65d3161321e3a5506e6e1860d21596d8d20bac3a51370f2c7527` |
| `source-settings-grouped-zh-light-20261006.png` | 61019 | `f4999b2e0b017d05ce3f8b097dba588b805aa95db5ac06b1915a5a91cc098844` |
| `source-settings-grouped-zh-dark-20261006.png` | 62212 | `20e934c5fdbc5e60483aa2120bde950a48eb105a20b13a3aae1b7b3032dc971d` |

These are scrolled model/access excerpts, not full Settings pages or font2
screenshots. Preceding text and later rows may be outside the viewport. They
show fixed local memory data, not a genuine Codex result, signed-package UI,
new motion footage or a marketing-ready release. Physical performance and
the real installation/share/delivery acceptance gates remain open.
