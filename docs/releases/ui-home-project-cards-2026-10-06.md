# Home project cards — October 6, 2026

Unreleased0.5.10-dev/code23, separate from the signed
[0.5.10 candidate](0.5.10-rc.1.md). Home now gives each project a matte surface,
decorative initial, identity/count block and state/recency line.
[ADR 0019](../decisions/0019-semantic-content-groups.md) owns the design trade-off.

## Source and build

133 native files/four build configurations were frozen. Only `MainActivity.java`,
`HomeRowsPresentationTest.java` and the old `HomeProjectHierarchyTest.java`'s
five geometry lines differ from the preceding model-group closure. No fixture,
read controller, Ui helper, project statistics or navigation callback changed.

| Binding | SHA-256 |
| --- | --- |
| Source inventory | `dd281e0ba4f45561da7291c39f6c07f96b613ab5c8570682b5573fd4398e4be6` |
| Main source | `03c57f8584ec121ed5e91f557a090871c1c29d8ff87a56e0922fe990634a74e1` |
| Current reading test | `69a0ed04ab9c20ebcfa06efcbc971b3faba1c2333e612b1777ce455dc4bd7c37` |
| Debug app,2165380 B | `9034c5a4ce8eb0a9b5d851288f08713631a12998e8fc8688fe4a0bd4929db922` |
| Test app,2015096 B | `9a38d89ae0da98193b7531695430172b08e9c8bed8c1e0f74c443af1eaa4e455` |

One offline Gradle build returned0 in29.343s: both APK tasks, JVM tests and lint
completed. Archived reports show27 tests with no failures/errors/skips and
30 warnings, zero errors. Stderr333B contains compiler deprecation notes.

## Actual native reading

`HomeRowsPresentationTest#continuousRowsKeepIdentityAndReadPosition` passed1/1,
JUnit19.011s; nonce `7a937514-e6c4-4a17-bf11-b0e2a3294e88`.

- EN/ZH light/dark font1 and EN light/ZH dark font2: all six known windows fully
  completed and reached DESTROYED. All12 emitted guard snapshots were zero.
- Actual summary records36 completed rebinds, preserving stable identity,
  counts, native focus and reading anchor/offset. Delivered/active/attention
  states retain their actual tint and text. This is an in-memory rebind check,
  not real periodic server polling or exhaustive cross-project recycling.
- Complete name/count/state/date text remained checked and individually
  reachable in the safe viewport. Names use184dp normally,232dp with the
  decorative tile/gap hidden at large text. The card retains48dp minimum,
  click/focus/ripple and its accessible project description.
- Both installed APKs matched the build. All44 actual ADB children returned0
  with empty stderr; ten device properties and native source matched before/after.
  Font/language restoration was observed; full palette restoration is not
  measured by this Home test. No task, sync, permission, navigation or reset was
  invoked. The old hierarchy matrix was not run; its earlier failure remains.

## Original screenshots and publication boundaries

Root directly viewed all four320×640 normal-font originals. The three sample
projects, complete headings, counts, dates, memory/sample markers and system
glyphs are visible. Eight brand/landing copies preserve every byte.

| Copy suffix, `source-home-cards-…-20261006.png` | B | SHA-256 |
| --- | ---: | --- |
| `en-light` | 41169 | `ec5cf598a20a8472c22eb54fd541e6fe27f2173b2180a3bf7cdae51f50e02430` |
| `en-dark` | 42114 | `aa26acd2a3a647256f8da0105828fc84bbf4ccbbab56af4e1856855c2a73cf5e` |
| `zh-light` | 48394 | `203fe31e35eb504bdbac9f675af4c2792e7076cacaeb7b3aa9c0dc87a0c0cbbb` |
| `zh-dark` | 49491 | `da6ff9a26e6b6c92e88ee8890b4f70cf239ec4e5a9a539ddf1a84e69b1c699df` |

An initial image/text-sync helper created the eight correct copies, then stopped
on a wrong builder filename before changing README/builder text. Completion
verified the existing bytes without overwriting them and used the actual
`build-landing.mjs` selector. Native checks were not rerun to repair this helper.

These are fixed local-memory projects, not genuine handoffs, signed-package
recordings or new motion footage. Website source selects them; a hosted
publication is separate. Physical performance and genuine workflow gates stay
open. [README layout checks](ui-readme-brand-reading-2026-10-06.md).
