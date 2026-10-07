# Settings: one recognizable project-access group

Local development after source `a328357`, 2026-10-07. The signed candidate14
does not contain this change. Verification is limited to the memory UI below.

Project access previously sat on the bare page below the grouped model controls.
Use the same flat, opaque theme surface for the whole access section, with 26dp
corners and 16dp horizontal/12dp vertical padding. Keep the label outside, all
project rows inside and the existing disclosure. Individual rows get no extra
cards. Project names naturally wrap in full, including distinguishing ID prefixes.

At 320dp, inner width changes 280→248dp. Normal text shares that width with the
permission control and 10dp gap; large text keeps the full 248dp, with the control
below. This trades horizontal room for a clearer group. It does not reduce text
size, hide identity or change permission confirmations, callbacks or pending state.

## Actual local checks

A fresh 134-source/four-configuration freeze passes build, 27 JVM tests and lint
with zero errors and 28 retained warnings. The compiler's 133-byte warning is
retained. Build/install/native have 1/26/33 direct children, all returned 0.
Exact app/test build bytes match their installed copies.
An independent pure-file reader rehashes all 120 raw outputs from the 60 known
returned children, six source snapshots, current 134/4 source closure, installed
APK reads, original PNG/JSON bytes and the actual native event/JUnit sequence.
It passes without running devices, networks or builds. Bound JVM/lint reports
support their stated counts; this does not imply every build task freshly ran.

The existing native reading method passes one test in 38.951s. Six guarded
windows complete and reach known destruction: English/Chinese light/dark at
normal text, English/light and Chinese/dark at 200% fixture text. Its 56 lifecycle
records include 24 zero guard receipts. Complete project names and ID prefixes,
surface/padding, large-text 248dp width, first/last line reveal, 48dp permission
controls, busy/idle presentation and fixed preference focus are source/JUnit
assertions; they are not separately emitted measurements.

Four original normal-font 320 × 640 PNGs show the scrolled model/access section.
Root and a separate reviewer directly viewed every original: project identity
and permission labels remain readable, functional groups are distinct and system
bars are legible. Model rows appear in all four captures. English access-card
bottom corners fall beyond the viewport; these are partial views, not the whole
card or Settings page. Capture eligibility asserts the first project and memory
marker are fully visible; both rows have geometry metadata. Normal English name
widths are 168/147px, Chinese 175/175px. The images do not show 200% text.

## Boundaries

No permission, mode, disconnect, preference or real business action was clicked.
The sample contains two long same-name projects; it does not cover arbitrary
unique long names. Existing fixed settings-control focus checks do not establish
permission-chip or disclosure-header focus preservation across rebuilding, or
TalkBack behavior. Known destruction is not executor termination. Only ten
read-only device values match before/after; font/language restoration does not
establish complete palette or all-device-state equality. No signed-native,
physical performance, motion or genuine handoff acceptance.

Automated capture acceptance stays false. Separate direct visual review accepts
these four local views; README copies preserve their original bytes.
[Exact selected receipts and original hashes](ui-settings-access-group-2026-10-07.json).
