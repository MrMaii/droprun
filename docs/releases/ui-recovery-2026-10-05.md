# Home recovery and workflow clarity — October 5, 2026

These are unreleased source refinements, newer than the downloadable
[0.5.3-rc.1 candidate](0.5.3-rc.1.md). Existing release files, tag, website media
and their provenance remain unchanged.

## User-facing changes

Home now shows a short synchronization notice instead of putting a raw error
above the projects. Opening it preserves the exact cause under “Last sync issue”.
Closing details starts no check. “Check again” immediately shows a disabled,
accessible checking state; repeated requests reuse a real check already underway.
A manual request during a cache-only poll must still start a synchronization.
Neither checking nor its completion confirms task receipt or project execution.

At 150% text size and above, project names wrap fully. The actual 200% English
screen previously hid “project 1” after a shared “Local sample” prefix. The
native title had a complete layout but its two-line height hid the suffix.
Ordinary text size and same-name project-ID disambiguation retain their rules.

Both READMEs explain the complete phone-reference, local-Codex, phone-delivery
flow in their three columns. Count definitions remain in the feature explanation;
the exact older-media/current-download distinction now precedes the images.
Original images, Demo labels and download targets remain intact.

## Local native evidence

A non-exported debug Activity renders the real Home with readonly memory data.
It forbids preferences edits, files, credentials, pairing, API calls, navigation
and services. It schedules no background jobs or polling. Three sample projects
are not the three real projects required for release acceptance.

The first notice test failed because its viewport assertion mixed dialog-window
and physical-screen coordinates. Correcting only that test produced 1/1 passing
check in 7.329s. A separate four-method EN/ZH matrix then passed 4/4 in 16.985s,
covering failure, successful recovery and actual scroll offset, in-flight reuse,
and Close without synchronization. Its screenshots exposed the clipped name.
The dedicated name test reproduced 1/1 failure before the production title fix;
the English failure prevented the Chinese loop from running.

After the title fix, five methods passed 5/5, each covering English and Chinese,
on API 35, 320×640dp, 160dpi, 200% text and baseline motion enabled. The test verifies
full text layout, 48dp actions, the disabled accessibility node and polite live
region, one in-memory synchronization, original error text, project IDs and
the actual visible-row anchor/offset. Eight original PNGs are preserved.
Capture-only accessibility quiescence removed the original English modal-exit
residue; core assertions and production timing were not weakened.

**Timing remains unexplained:** this final matrix took 2,680.552s according to
the runner, versus 2,700.019s of host wall time. No new ANR or crash event was found
in that run's recorded interval; previous keyboard/debug startup ANRs remain.
This is functional evidence, not a normal-duration or performance result.
The earlier four-method matrix remains separately recorded, not substituted
for this anomalous duration.

The five-method run used MainActivity SHA-256
`d8e358a83febeb24013c8e80393357fc83e8361c3e92a24435863389cd54610e`,
debug APK `5d3d3675d5513d6c4439ff3bf975756e53fa652e22a1ceac1034c8dfa723ae5b`
and test APK `c3373b8266ec177debb5a903b60a39da499bd3fe095647288429ddcd1ffa1f9d`.
Its build passed JVM 24/24 and debug lint 0 errors/30 warnings. The later cache-only
completion fix and its dedicated regression are tracked separately; this
five-method result is not claimed for that later source.

The later cache-only/manual regression passed 1/1 in 1.542s (host 2.528s), with
capture disabled and no settings change. It holds a cache-only worker, queues the
manual request, then verifies one actual in-memory synchronization, settled state,
project/scroll preservation and zero forbidden accesses. A simulated receiver
flag is guarded against a live receiver and restored to false by `finally`, with
an explicit final assertion. No new ANR/crash event was recorded. This single
English timing-case result does not replace the anomalous bilingual matrix.
It maps to MainActivity SHA-256
`b5d71e2351bb3417164bc03871acb8cf351a8272a9c31139d30517cfdeac3ced`,
with separate immutable APKs and source hashes in
`.local/ux-oct5-home-recovery/native-cache-race-result.json`.

Private originals, immutable APKs, hashes, logs, captures and readonly diagnostics
are under `.local/ux-oct5-home-recovery/`. Device size, density, font and animation
settings were unchanged throughout this Home work.

## Remaining scope

The independent real-keyboard probe compiled and installed but did not reach
typing: its initial 2/2 cases failed before a visible IME, and a later isolated
attempt ended with `Process crashed.` Original logs, ANRs, 20 file hashes and both
immutable APKs remain under `.local/ux-oct5-ime/`. No keyboard layout, Back,
Chinese composition or physical performance acceptance is inferred.

Full TalkBack, physical performance, genuine share/receipt/delivery, source-App
return, installation and process-loss gates remain open. This work neither
repeats rejected operations nor authorizes real submissions. Current package
availability is already verified in the candidate record; stable launch,
genuine execution footage and marketing handover remain separate.
