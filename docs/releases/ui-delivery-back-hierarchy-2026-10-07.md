# File preview: return through delivery files

The preview's top Back used to leave the delivery Activity, while its body Back
returned to the file list. Idle preview Back now reuses that list reload and
cleanup path. List Back still exits; choosing a save location retains its existing
exit branch. An active save keeps the existing Stay/Leave confirmation first.
Only one production branch changes; download, verification and save logic do not.

The new nonexported debug fixture directly subclasses Deliverables. It uses the
real list/preview and Back/Stay listeners with a fixed memory list, fallback
preview, picking flag and never-run SaveOperation. It does not use or change the
older restricted Demo fixtures. No task, file, picker or save worker is submitted.

The first take has one actual JUnit failure in 7.508s: its immediate
`isFinishing()` assertion follows a list Back click too early. Preview top Back,
body Back and saving Stay have already passed; cleanup closes its one window and
executor. The original outputs remain failed. A test-only successor waits for
the framework's actual DESTROYED state and executor termination before cleanup;
it does not substitute `scenario.close()` for naturally completed Back.

Fresh source freeze `a3ff8ff2842464eba62fe519c8789b6695d5d5801720a1d4808747e6ff2c4ee5`
binds 137 source files and four configurations to debug 0.5.15-dev (version code
28). Build, install and native actions return 0 with 1/26/25 direct children.
Reports contain 27 JVM tests without failure/error/skip and lint 0 errors/28
warnings; retained reports
do not imply every Gradle task reran. Saved/current APKs and four installed-byte
reads match. These are debug packages, separate from signed candidate15.

The selected native test passes in 13.658s. Four EN/light and ZH/dark windows
produce 22 ordered events, 10 list renders, 8 preview renders and zero forbidden
actions. All four framework Back closes and executor terminations precede test
cleanup. The test restores its language/theme/palette; ten read-only device values
match before/after. Independent retained-file review rehashes 104 raw outputs,
141 source snapshots and six source receipts without replaying device actions.
[Selected evidence](ui-delivery-back-hierarchy-2026-10-07.json).

No screenshot or motion acceptance is added. This proves local listener routing
and the owned Stay dialog in synthetic states, not physical/system Back gestures,
real file verification, cache cleanup/network reload, list-scroll restoration,
picker callbacks, saving continuation, process recovery or a genuine handoff.
