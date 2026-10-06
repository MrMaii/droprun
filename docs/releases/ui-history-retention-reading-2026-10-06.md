# History and retention reading — October 6, 2026

History previously placed its date before its current state. Rows now read
title → state → complete source/date. The status pill keeps its natural width
inside a weighted container, reserving8dp plus the18dp trailing arrow; metadata
keeps the complete reading width. Existing continuous rows, separators, colors,
stable IDs, pending actions, navigation, pagination and Refresh remain unchanged.

Retention previously made “Last synced” stronger than the actual periods.
One matte Settings group now gives actual periods and unknown-policy recovery
15sp primary text; report/preview/cleanup rules use13sp primary text, with cache
and deployment context subordinate. Strings, parsing,1–365day validity, requests,
storage and read-only policy remain exact. This follows
[ADR0019](../decisions/0019-semantic-content-groups.md).

## Separate source/build bindings

| Binding | SHA-256 |
| --- | --- |
| History production | `31e868630a65be2d77ae62ddbabd85b3d7d00e4aff6596ca4f7609c21f0f00f0` |
| Existing HistoryRows test, geometry only | `f6a4c52a9a157d80ce60149a1209d65a8e890968ed1929c0d1ee685abc370be1` |
| Unchanged three-row memory fixture | `5a4105c7185d8b16eab9f17aea7835be2a28e22c10109a0220e0b239bb331cd2` |
| History freeze,133native/4configs | `b9dd7171d8b636f78e91e0a3039e228374484c390c519dd59a92ee5647d6d896` |
| History debug APK,2,165,499B | `29a9d72c170e849981f4e04cd9d783839dc0aef79fb51cea4046649ff1d70cea` |
| Test APK,2,015,671B | `f22cdc73830ab66e66c667dd453ce757db3e39e169a25efc9d33918d3c30c5ae` |
| Later retention production | `0a1882603d4f42459425183d9ee55ae4c301321e57a070e62f504420315e7f12` |
| Retention freeze,133native/4configs | `05ace8818b222d31e37aad1f1c629f3ee4ecd48c877e9d961dd73c3f11d5bd8d` |
| Retention debug APK,2,165,587B | `787c1737bdb378ff259218499719536a0ef2e95b80b0bf0bb488e13a3fd9c124` |

Only two Java files changed from the
[History Refresh freeze](ui-history-refresh-feedback-2026-10-06.md).
Actual offline History build completed in47.563s, exit0, retaining402B of compiler
deprecation notes. Root archived its reports before the later build:27JVM tests,
0failures/errors/skips and lint28warnings/0errors. Those are report counts, not
new UI tests. Retention then changed only SettingsActivity and built in38.813s,
exit0/stderr0; its separate archived reports also show27/0 and lint28/0.
Its test APK remains byte-identical. Versions stay0.5.10-dev/code23; neither
build changes the [signed candidate](0.5.10-rc.1.md).

## Actual History native check and originals

Nonce `652ad8e5-84d9-407a-90b9-2b8624201455` selected only
`HistoryRowsPresentationTest#historyRowsPreserveReadingAndBindings`.
JUnit passed1/1 in25.417s; the known ADB child returned0 in27.25s.
All46ADB children returned0/stderr0; installed APK bytes, all ten device
observations and frozen source matched. Raw8,435-byte output SHA-256:
`9850089611c7017b0cc70086607e0278cfe42b87f16dc4cd99600c77881071ad`.

Six windows completed and reached DESTROYED: EN/ZH light/dark font1 and
EN-light/ZH-dark font2.43actual lifecycle events,18emitted guards0 and the
captured summary bind6entered/6completed/6destroyed/36rebinds/4captures.
Original fixture, wait/close/unknown-lifetime gates and screenshot writers
remain unchanged. Geometry, complete/readable sample text, recoloring,
identity, focus and reading offset passed. Executor termination and full
palette restoration were not measured by this fixture.

Root directly viewed and accepted all four raw320×640 normal-font stills;
the README copies retain every byte, memory marker and system bar pixel.
Original metadata remains `capture_accepted:false`; visual acceptance is a
separate record, not a rewritten measurement.

| Raw still / exact brand copy | SHA-256 |
| --- | --- |
| [EN/light](../../assets/brand/source-history-hierarchy-en-light-20261006.png) | `f415948efb543fb98737ea5980d0b377d4ad2b34f2fe1c338e3782694e66e469` |
| [EN/dark](../../assets/brand/source-history-hierarchy-en-dark-20261006.png) | `7b889fcce0921abb28b820e9b8311548af5a2422939316793321e4e74c68fe8f` |
| [ZH/light](../../assets/brand/source-history-hierarchy-zh-light-20261006.png) | `351b93bd4cb9722d4ace3b13a9ce25a975ec4f886efb62d2192a197e2eef5b7d` |
| [ZH/dark](../../assets/brand/source-history-hierarchy-zh-dark-20261006.png) | `74b76183f3c675fbbfa80805e5035c351c7a074cb7651692529139882f2631fb` |

## Limits

The fixed samples cover Shared and short running/completed/failed states.
Longest-state wrapping, Follow-up and pending errors remain protected by source
constraints, not newly measured sample cases. Font2 readability checks are not
font2 screenshots, physical accessibility or frame/press timing.

The earlier retention build above retains its original binding. A later
[code 24 debug reading check](ui-settings-retention-reading-2026-10-06.md) supports
unknown/cached-policy paragraph layout and first/last line reachability; it does
not establish signed-package or retention screenshot acceptance. Settings images
retain their older separate binding. No genuine Relay,
Codex task, OS/IME change, old HistoryIdentity matrix/motion/dialog,240-row or
process-loss test was invoked. Stable and marketing gates remain open.
