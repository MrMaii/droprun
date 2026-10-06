# Settings, project history and interrupted disclosure

Verified October 6, 2026 in the dedicated API35 emulator. This is development
UI with guarded, memory-only samples. It does not establish physical-device
performance, genuine Relay/Codex work or a new signed release.

## Changes

- Ordinary Settings sections share the page reading edge. The connected computer
  keeps its distinct dark surface and original padding.
- Project history uses continuous rows and one separator between records. Titles
  now have 280dp instead of 240dp at the measured 320dp viewport.
- At 150% font and above, the project access action follows the project name on
  its own line. At 200%, duplicate project names and their distinguishing IDs
  retain 280dp, with the action left-aligned at least 8dp below the full name.
- Permission controls now dim while the page is busy, matching their existing
  disabled state. Existing permission confirmation and callbacks remain.
- Interrupted expansion resumes from its current opacity. Reversing an opening
  or closing disclosure no longer jumps to a hard-coded alpha value. Height,
  duration, terminal state and the motion-off path remain unchanged.

## Actual checks

| Check | Before change | After change | Scope |
| --- | --- | --- | --- |
| Settings | 1/1, 16.225s | 1/1, 16.987s | Six language/theme/font windows; four normal-font stills; complete preference rows, duplicate project identity, scroll/state, 48dp/ripple/focus; accepted busy/idle feedback and 200% stacked actions |
| Project history | 1/1, 17.210s | 1/1, 17.111s | Six windows; 36 status rebinds; stable row identities, anchor, focus and separators; four normal-font stills |
| Disclosure opacity | 1/1, 4.258s | 1/1, 4.195s | English/light and Chinese/dark; four local header clicks per window; reverse open/close while the animator is running |

Before the fix, the four observed immediate reversal alpha changes were
`+0.376486`, `+0.361866`, `+0.478876` and `-0.409106`. After the fix, all four
were exactly `0` in the recorded observations. Direction, final visibility,
height and four callbacks per window also passed. This is an opacity continuity
check, not a frame-rate or press-latency measurement.

All fourteen accepted windows reached known `DESTROYED`. Actual guard rows:
Settings24, history18 and opacity6, all zero forbidden actions. No task, permission,
preference or removal operation was invoked; busy and project examples were
rendered in the guarded fixture's memory. Source closures, installed APK hashes
and eight device facts matched before/after. OS font, animation and IME settings
were unchanged. Android JVM24 passed; lint had zero errors and 30 existing warnings.

Three failed predecessors are retained in private diagnostics: the Settings
helper incorrectly compared a rotated arrow's local rectangle with its
untransformed coordinates; the first opacity probe waited for UI idle and missed
the animation; its successor used an unnecessarily narrow alpha sampling band.
The final probe samples and reverses inside the same main-thread callback when
height, alpha and animation fraction are interior. These probe failures are not
reported as product failures or functional passes.

## Source and captures

Source SHA256 for the original fourteen-window capture pass:

- Settings: `08fb1bdf8776f1874b26fd50d1995905b4ca371727075cb73e6fb1565bd670cb`
- History: `e324bd86a87775cda58583b1b3dc86eaa1157c95997e77b4a261b19d551df898`
- Shared Ui: `1a9b61f0d19d89412d4e0ac3e939ca7def6a725f54d3db6546f8c9ad830d9c1d`

The accepted build contains 120 Android source files. Four Gradle files remain
unchanged; debug version is 0.5.8-dev/code21. App APK SHA256:
`75f772c27593a0d6bf00b87e67d195f6aa518df44ca9b3d28ec28753beb9b637`;
test APK: `d64c9fecea619aa21abcc3840336021d0f45e209e93fbb4eebfd8b07b7fe8234`.

Eight raw 320 × 640 normal-font screenshots were directly viewed and copied
byte-for-byte to the brand gallery. Memory markers and system pixels are retained.
Settings captures show the scrolled model/access area, not the entire settings
page. History shows three sample records with sample dates, not actual work.

- [Settings, English/light](../../assets/brand/source-settings-reading-en-light-20261006.png)
  · [dark](../../assets/brand/source-settings-reading-en-dark-20261006.png)
  · [中文/浅色](../../assets/brand/source-settings-reading-zh-light-20261006.png)
  · [深色](../../assets/brand/source-settings-reading-zh-dark-20261006.png)
- [History, English/light](../../assets/brand/source-history-reading-en-light-20261006.png)
  · [dark](../../assets/brand/source-history-reading-en-dark-20261006.png)
  · [中文/浅色](../../assets/brand/source-history-reading-zh-light-20261006.png)
  · [深色](../../assets/brand/source-history-reading-zh-dark-20261006.png)

The signed 0.5.8-rc.1 downloads retain their earlier immutable source. Current
reading refinements are development source. Genuine end-to-end acceptance,
physical accessibility/performance and clean Windows/Cloudflare setup remain
open. That original pass covers duplicate project names. The independent check
below resolves the remaining unique-name case.

## Follow-up: complete unique project names at large font

Unique project names now retain their complete text at font scale 1.5 and above.
Regular-size names keep the previous two-line cap. At 200%, the permission
action stays below the name; a long name can be read through scrolling before
the action is reached. Project labels, permissions and confirmation are unchanged.

The focused native baseline passed 1/1 in 3.133s and recorded actual two-line
ellipsis hiding 109 English characters and 40 Chinese characters. The corrected
check passed 1/1 in 3.395s: seven English and six Chinese lines had no ellipsis,
and all thirteen complete lines were individually reachable inside the safe
scroll viewport. Both names retained 280dp width; permission controls retained
their labels, state, ripple, focus and at least 48dp targets.

Each phase used only English/light and Chinese/dark memory windows at font2.
All four windows reached known `DESTROYED`; four actual guard rows per phase
were zero. No business control was clicked. Font/language state was restored;
device facts, OS/IME settings and source closures matched before/after.
This focused check does not add new screenshots or physical-device evidence.

Settings SHA256 after this one-statement change:
`c1fe0eae1540345616eded25aedd2bf6b5d2e3810b03e0396b447fdd229d05ac`.
The corrected build contains 121 Android source files with four unchanged
Gradle files, still 0.5.8-dev/code21. App APK:
`b0076ad3283a5a270449b88636b8209dfa9056a1ba648f998bc95c8b3673305c`;
test APK:
`3f1639aeeeb3e3beb7ee047a90c45d66b68514725a62ae03c9d5f28022899138`.
The earlier capture hashes and signed 0.5.8 downloads remain unchanged.
