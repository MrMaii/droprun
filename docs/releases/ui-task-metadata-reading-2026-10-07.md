# Task reading continuity during metadata refresh

October 7, 2026. Later development after [signed candidate 0.5.14-rc.1](0.5.14-rc.1.md).
That immutable download does not include this change.

Previously, changing only a receipt's `updated_at` made Task rebuild its body.
Expanded sections reopened, but the report TextView, selected buffer and keyboard
focus target were replaced. The presentation comparison now excludes only that
timestamp. The shared-date caption updates separately in its existing TextView.
Store cache receipt ordering and every other task field remain unchanged.

The new bounded native method checks English/light and Chinese/dark in the
existing memory fixture. After expanding a 24-paragraph report, it sets a text
selection programmatically, reveals paragraph 12, then invokes actual render with
`updated_at=2`. The same report View, buffer, selected range, disclosure and reading
offset remain. It focuses the actual report heading, updates to `updated_at=3`, and
checks the same focused View and visible offset. Both windows also replace the
caption with synthetic `Stale`, verify actual date rendering, and change the report
as a positive control: the new real text is rendered in a new View.

Fresh build passes: 27 JVM tests, zero failures/errors/skips;
lint has 0 errors and 28 existing warnings.
The 333-byte build stderr is retained.
Both installed debug APKs match the newly built bytes. One native test passes in
7.901 seconds; its 25 actual events contain 20 fixture-bearing
receipts, four metadata updates, two fully completed windows and two known
DESTROYED lifetimes. All 52 direct commands returned zero. Source closure covers
134 Android files and four configurations before and after each phase.

The fixture asserts font scale 1; the device's global scale 2 is untouched. Ten
read-only identity/settings values match before/after installation and native
checking. Instrumentation requests non-touch mode; both windows were already
observed in that mode. After known scenario closure, the setter restoring the
saved mode returns. That restoration is not reobserved
and does not mean every device state remained unchanged. Destruction receipts
contain lifetime and guard values, without invented post-close geometry.

No genuine submission, polling schedule race, selection gesture, minute wait,
rotation/process loss, 200% reading, signed-package acceptance, physical Android,
frame or press-latency claim. Prior failures remain in their dated records.
[Machine-readable scope and actual receipts](ui-task-metadata-reading-2026-10-07.json).
