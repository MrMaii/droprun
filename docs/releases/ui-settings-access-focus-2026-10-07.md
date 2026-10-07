# Settings: keep the keyboard user's project in place

Development after public source `525f55ce`, 2026-10-07. This change is outside
signed candidate14. It changes focus presentation inside the same Activity;
permission policy, callbacks, confirmation and disclosure motion retain meaning.

The access disclosure gets a stable header ID. Each permission chip uses a
nonlocalized tag containing the complete project ID. A rebuild matches that
entire tag, so same-name projects and reordered rows retain the correct target.
Only existing non-touch keyboard focus is restored. If its chip is missing,
disabled or inside a collapsed group, focus returns to the access header.
Later completion follows the user's current focus and does not retain an old
chip request that could steal it back. The chosen control is revealed after layout.

## Local verification

A fresh 134-source/four-configuration freeze passes the debug build and exact
installation. Bound reports contain 27 JVM tests without failures and lint with
zero errors/28 retained warnings; not every build task is claimed newly executed.
The native method passes one test in 34.565s, completing and closing all six
windows. Its 62 ordered events include 24 zero guard receipts and six touch-mode
restoration setter returns after known destruction. All 60 direct children return0.
An independent file-only reader rehashes 120 original outputs, six source snapshots,
current closure, original PNG/JSON and exact built/current/four installed APK reads.
[Selected receipts and retained failure](ui-settings-access-focus-2026-10-07.json).

The existing guarded Settings reading method covers six fixed memory windows:
English/Chinese light/dark at normal text, English/light and Chinese/dark at
200% fixture text. The added assertions enter actual instrumentation keyboard
mode, require focus requests to succeed and compare the real focused View.
They check rebuilt headers and both chips, same-name project reorder,
synthetic pending/idle and collapse/reopen. Cache values and synthetic flags
are restored before the existing capture path. No permission action is clicked.

The original fresh attempt fails its new recursive header-child visibility
check in 5.173s. Header identity/focus/Boolean assertions precede that failure;
the raw stack identifies a child predicate, without geometry identifying which
child or whether there was actual clipping. One window closes; other windows,
captures and native after-device reads do not run. Its 42 returned children and
84 original outputs are independently read and retained. The sole correction
uses the existing transform-aware preference visibility check for the new header.
Production and the legacy helper are unchanged; visibility is still required.

## Boundaries

Focus assertions exercise direct production render in a memory fixture. They
do not establish a real polling/permission callback race, Activity recreation,
TalkBack or physical keyboard performance. Missing-project fallback is source
coverage only. A tag is not a saved-hierarchy ID for recreation. Individual
focus assertions are source/JUnit checks, without per-assertion telemetry.

Four normal-font capture files from this run stay private and pixel-unaccepted.
README retains the separately reviewed access-group images from the previous
source. Known destruction does not prove executor termination or complete Ui
restoration; touch-mode restoration events prove the setter returned, without
a fresh framework-mode observation. Ten read-only values do not prove all
device state unchanged. No real permission, task, signed-native or full UX closure.
