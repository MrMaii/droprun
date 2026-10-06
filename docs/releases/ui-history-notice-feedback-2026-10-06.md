# History notices: feedback follows the action

Recorded October 6, 2026. Development UI `0.5.11-dev`, Android code 24;
these changes are not in the signed [candidate 11](0.5.11-rc.1.md).

A History read failure has a details action. Removal status, action errors and
“Retry requested” are text notices. Previously those static notices could retain
enabled press feedback; the retry text could also inherit the previous details
entrypoint. The same notice now enables clicking, keyboard focus and press
feedback together only when details are available. Switching to a static or
hidden notice cancels its view animation and resets scale to 1.

Text, amber color, 48 dp minimum height and polite accessibility live region
remain. The retry callback keeps its existing request order and announcement.
No deletion, retry, refresh, permission or task-state behavior changes.

## Actual bounded checks

The offline build returned 0 in 27.219 s: 73 actionable tasks, 18 executed and
55 up to date. Its 333-byte stderr contains compiler deprecation notes, retained
with the original output. Actual retained XML reports contain 27 JVM tests,
zero failures/errors/skips, and lint with zero errors/fatal issues and 28 warnings.

| Binding | SHA-256 |
| --- | --- |
| Frozen 134 Android sources / four Gradle configurations | `0023a672504ff123d1902453f029be48e9c2f1f6b5e294e6cfaa3167b5b181a4` |
| Debug app APK, 2,183,717 bytes | `7ac4046a80066bb7079e3be216263b1cf0e359d93121a42c3b3274477f22110d` |
| Debug test APK, 2,026,603 bytes | `da31df061d6d5e11cef8e52d453eadc0c420251d602a841f0118e3798fa72c1b` |

`HistoryShareStateTest#actualHistoryReadKeepsUnknownEmptyAndFailureDistinct`
passed 1/1 in 5.239 s. Its three existing memory windows—English/light/normal,
Chinese/dark/200% and English/light/cached—complete six memory reads, emit 21
existing guard snapshots and all reach DESTROYED with both executors terminated.
The 27 ADB commands returned 0 with empty stderr; installed APK hashes, source
closure and ten read-only device observations match before/after.

Each window emits one new notice record covering five cases: details, a static
action error, removal status, direct readonly retry feedback and hidden reset.
All 15 cases pass. The native accessibility nodes retain their text/live region;
click, enabled, focus and ACTION_CLICK match the actual details state. Static
notices retain amber text and full opacity, and reset synthetic 0.975 scale to 1.
Original errors, notice text, focus, adapter, rows, cache and read counts are
restored; forbidden actions remain zero.

The removal case constructs an unrun memory object. The retry case only invokes
the UI helper; it does not execute the real retry callback. No genuine send,
removal, service, filesystem persistence, original Demo/Pair, OS/IME change or
process recovery occurs. Scale is deliberately seeded for reset checks: this
does not measure interruption of an actual running press, input latency,
TalkBack speech, pixels or physical-device performance.
