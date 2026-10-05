# Settings feedback stays with the decision — October 5, 2026

Execution-setting progress previously belonged only to the Settings Activity.
Recreating the page lost Saving and enabled both choices while the old work could
continue. A readonly native red reproduced that UI loss. The change now lets a
new page observe the same operation and its eventual result. Saving, success and
failure now appear inside the Execution card, beside the affected choices.

## Implementation and UX

A mode-specific FutureTask holds the submitted work and scope hash; its observer
is a weak Activity reference. The worker captures an application-backed Store,
not the Activity. Configuration retention passes that same operation to the new
page. No new request is created by restoration. The pending controls stay
disabled and unfocusable, with the confirmed cached selection intact.

Completion is delivered on the main thread, checking the current operation,
page lifecycle and live preference-derived connection scope. A changed connection
ends observation and asks the user to reopen the screen. Disconnect clears
ownership before showing its own progress. Finished-result rendering and queued
success announcements are deduplicated; announcement waits for attachment and
checks ownership again. Already displayed success is restored without another
announcement. Existing direct-execution confirmation, request values and server
rules remain. There is no process-death persistence or settings outbox.

## Native scope

The original red failed1/1 in1.95s: Saving disappeared and both choices became
enabled/focusable. That older fixture overrides the entire save handler, so its
evidence establishes UI-state loss only. Its source/APKs/log remain frozen.

The independent green fixture substitutes only controlled work and background
refresh over guarded readonly memory. Production choice handling, save,
retention, observation and completion run unchanged. It never edits actual
preferences, calls a Relay, reads credentials, saves a task or accesses an outbox.
Simulated success does not change the confirmed cache or establish server receipt.

The first lifecycle run passed9/9 in31.710s: seven new methods and the original two
SettingsBusyTest methods, restored without edits. Checks cover pending success
and failure through recreation, completion queued before new-page attachment,
once-only actual accessibility announcement events, repeated result rendering,
operation identity replacement, disconnect-like memory feedback ownership and
live-scope changes. The replacement and disconnect checks are controlled memory
scenarios, not genuine duplicate taps or a real disconnect.

Those captures exposed a second problem: revealing the complete choices scrolled
the global Saving/result notice outside the safe area. A separate strict native
red failed1/1 in3.071s, before creating any new PNG. The fix moves only mode-change
feedback into that card; disconnect feedback keeps its separate ownership.

The final inline-feedback run passed the complete9/9 in45.058s (host46.710763s),
with12 EN/ZH lifecycle configurations. Four normal-text pending/success/failure
captures require the caption and Review choice to be fully visible together;
all four pass. Six individual targets at system fontScale2 retain complete-text
and safe-screen checks. The whole section need not fit simultaneously at200%.

The four final raw PNGs carry a temporary instrumentation-only overlay saying
"UI probe · memory only · no request". It is not a product element. The actual
scroll excludes the Settings topbar; the overlay partly covers the Computer
section label but covers neither Execution choice nor its caption. These images
prove that region, not an entire page or genuine settings receipt. Original four
unmarked, clipped captures remain separate. Device and downloaded PNG/JSON hashes
match. Settings and crash buffers are unchanged; two binder-freeze event warnings
were added, with no new ANR/crash observed in this run.

The first green build passed33s, JVM24/24 and lint0errors/30warnings. Before any
native execution, static review corrected a geometry helper that would treat an
intentionally invisible decorative check as visible. Both build-only revisions
remain; the final predicate is limited to an invisible, non-accessible ImageView.
Rows, all text and visible icons keep their complete geometry assertions.
These revisions were not failed native runs and changed no product or fixture.

The inline build passed45s, JVM24/24 and lint0errors/30warnings. A subsequent
test-only19s compile moves overlay creation and assertions inside the same
try/finally cleanup; it changes no product APK and does not repeat JVM/lint.
Final Settings source SHA-256:
`81a58fe508178f4f8b02a24573815bc28dd4ec22c2fe643c9caeeef7076211e6`.
Final local debug App/test SHA-256:
`98623b107e3de51b3d30ce349322c9c717cc300fb87d6066c6f9f79bf294d245` /
`a4e4c4a4d6ae14d1890838dc1b695efde2798b2969863e7e96dd3f27b86c9a05`.
All six checked production/debug/test sources are frozen and unchanged. These
are local regression artifacts before the next runtime-version bump, not public
signed-package identities.

Physical Android, TalkBack use, real IME, real settings receipt, genuine pairing,
process loss and performance remain unverified. This is a local UX lifecycle
fix, not a stable-release or marketing-readiness claim. Current0.5.3-rc.1 packages
and the website have not yet been replaced.
