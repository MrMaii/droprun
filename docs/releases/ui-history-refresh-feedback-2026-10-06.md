# History Refresh feedback — October 6, 2026

Previously a history read with cached rows could leave Refresh looking available
throughout the request. A repeated click reached the existing busy guard and
appeared to do nothing. Refresh now retains a disabled, half-opacity state and
a localized refreshing description until completion. Cached rows remain readable;
the existing read, cursor, error, polling, task and permission paths are unchanged.
Foreground return also synchronizes the actual busy state, without adding a read.

## Frozen local source and build

| Binding | SHA-256 |
| --- | --- |
| ProjectHistoryActivity | `b10c45787558ae4015e09def8903699a7469e3decc17ed83fcaefe5bef180b96` |
| HistoryShareStateTest | `26bcbf84e9cd8501a83f97d01854646790eaf74ac901e010f271cc5c243d1696` |
| Unchanged protected memory fixture | `00ce0ad1879eddbe580fb760a6210c9c653015fa7fcedb41fd838811df8aed04` |
| Complete source freeze,133native/4configs | `6321e926ec0dc6d4cad1197cecd7c741cbe2eef90682aa8faed5a74a54ef6892` |
| Debug APK,2,165,473B | `a5ef6b390de12def1eafe95f0cbbb5551b3f3b0c22d56c9d74c566872ef3002d` |
| Test APK,2,015,421B | `c2a715fc884ea34fdc47bde2916fbbe86c0e33288917487638c0f02effe02e87` |

Only the two listed Java sources change from the separately tested
[Home card source](ui-home-project-cards-2026-10-06.md). Actual offline Gradle
build completed in42.5s, exit0, with402B of retained compiler deprecation notes.
Archived actual JVM XML shows27tests,0failures/errors/skips. Actual lint XML has
0errors and28warnings; no lint count is inferred from command exit alone.
The build's APKs and installed bytes matched. Version remains0.5.10-dev/code23;
this is separate from the public signed0.5.10 candidate.

## Actual bounded native run

Nonce `f7cdf03d-a764-4fae-8be9-a37b7b128c06` selected only the existing method
`HistoryShareStateTest#actualHistoryReadKeepsUnknownEmptyAndFailureDistinct`.
JUnit passed1/1 in7.91s. Its17,787B raw output has SHA-256
`f33a8fbac84077aa8ad754c42de5e3d5a842be05268042370e8bb3b169f1b433`.
All27ADB children returned0 with empty stderr. All ten device observations and
all133native/4config source bytes remained exact before/after.

The original three windows were fully completed and destroyed: EN/light/font1
empty, ZH/dark/font2 empty, and EN/light/font1 with three cached records.
All21 actual emitted guard snapshots show0forbidden actions and no sync service;
actual completed memory reads total6. Every completed window's two executors
terminated. The fixture and original wait/close/unknown-lifetime gates remain.

At the six existing pending points, actual Refresh is disabled, alpha0.5,48×48dp
and has the appropriate refreshing description; completion restores alpha1,
availability and the original description. Six programmatic `performClick()`
calls reached the original busy guard without another read. This is not a physical
tap or press-latency measurement. Empty loading/failure/confirmed-zero states,
null-detail failed append and the cached null-detail failure remain distinct.

Cached reading keeps the same list/adapter/cache/visible ID/top offset and emits
no adapter notification. Because busy Refresh is now disabled, this scene requests
focus on the existing enabled Project information icon, without clicking it;
the same Info focus survives the callback. It does not establish the former
Refresh-focus scenario. The fixed cached scene covers failure only, not success.

## Limits and preserved originals

Foreground-return behavior is supported by the implementation's static ordering;
pause/resume was not exercised by this three-window run. No new screenshots,
genuine Relay, Codex task, outbox, server recovery, real keyboard/IME, TalkBack,
animation, frame timing or signed-package acceptance is claimed. Older Home,
Settings, Share and media checks retain their own source/APK bindings.

The first private proposal generator stopped at Python syntax parsing, and a
report-helper preparation assertion stopped before archiving. Their originals
remain. A separate report reader archived the successful build's actual XML;
neither failure was hidden by rerunning the build or native test. The older
[history owner parsing failure](ui-history-settings-recovery-2026-10-06.md) also
remains distinct. Stable release and marketing acceptance gates stay open.
