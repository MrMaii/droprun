# Share compact-window layout

Date: October 6, 2026 UTC. Working-source refinement after the immutable
`7097a0db9297fe14873fcc9f652f5d0bc6e79343` candidate. The signed
[0.5.7 package](0.5.7-rc.1.md) does not contain this change.

## Problem and change

At the actual top of the compact English form, draft feedback showed only7 of
its20dp height. The52dp Send button was already visible; it was not fixed here.
Native measurement also showed the old maximum falling400→345dp when available
height increased400→401dp.

One production statement changes the sheet's maximum height. Compact windows
retain usable space; tall windows gradually retain breathing room. `AT_MOST`
preserves natural wrapping for short content. Header, ScrollView, system/IME
insets, transitions, note/model logic and Send callback are otherwise byte-identical.

## Verification

- Baseline103 Android sources; one new test makes104. Red native1method failed
  as expected in4.782s, with `scrollY=0` and retained original screenshot/JSON.
- A subsequent EN/font2 capture-only probe failed in4.629s because its test
  watermark overlapped the header. The original capture and build remain.
  Only the explicitly opted-in memory fixture moves the marker into the sheet,
  adding18dp in English and21dp in Chinese. Production has no such marker.
- One green native batch passed **2/2 methods in64.122s**, each across EN/ZH,
  light/dark and per-Activity100%/200% font configurations. The second method is
  the unchanged model/effort regression. JVM24 passed; lint0errors/30warnings.
- Four normal-font initial views show complete feedback, Send and the model
  summary. At200%, individual question, note, model, execution context, Send
  and feedback remain scrollable and readable; the whole form does not fit at once.
- EN normal content viewport426→439dp. Its13dp gain is distinguished from the
  additional18dp test marker. No native Chinese red comparison was performed.
- Actual View measurement covers compact and tall constraints, no overflow or
  decreasing height, adjacent boundaries and natural short content. For example,
  available/measured616/616,720/720,721/720,900/774 and1200/1032dp;
  a200dp child remains200dp under a900dp maximum.
- All104 green sources match the working tree;101 other baseline sources and
  four Gradle configuration files remain unchanged. Twelve green raw320×640
  PNG/JSON pairs, one red and one probe pair give28 matching device/host hashes.
  Three app APKs differ; the test APK is identical in all three builds.

Exact sources: ShareActivity
`dd25b5c2bbfb73b15fa82eb9f01109b92762b203dc1e3dd66c591daa570d76f6`;
opt-in fixture
`a61c6741b42ccbb2a016f1b21f7e868758e94f64f0f0eac758fd408e9d197da4`;
new test
`2a26ae62b9559436e2dedc66883bae07d4404e72c1538b62e80ec1675f06f3ed`.
The original record,498-file read-only inventory, build logs and failure captures
are preserved privately.

## Limits

Samples remain in guarded memory. No Send, API, pairing, import, real draft save,
cache/outbox or process-loss flow ran. Notes were set programmatically. System
font, density, IME and animation settings were unchanged. Crash buffer is
identical; six new binder-freeze warnings and seven rolled-out older lines are
retained, without a causal or performance claim.

This does not establish real IME resizing/typing, TalkBack, physical timing,
motion-off runtime, tall physical windows or genuine task acceptance. Motion-off
closure remains source-reviewed through unchanged existing functions.
