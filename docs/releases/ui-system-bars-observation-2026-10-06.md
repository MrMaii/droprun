# Dark system bars — screenshot timing observation

Date: 2026-10-06 UTC. This is a local emulator observation, separate from the
Home card refinement and the unchanged 0.5.5 candidate. No production UI code
changed in this investigation.

The first Home screenshots showed dark status-bar glyphs on a dark background.
A focused, non-exported, memory-only native fixture then recorded the window's
theme, appearance request, focus and actual pixels. It did not pair, navigate to
business screens, send tasks, read business storage or change device settings.

## Two separate observations

| Observation | After window focus | Dark appearance / legacy flags | Light foreground pixels |
| --- | --- | --- | --- |
| First, light then dark | 361ms | 0 / 0 | 0 |
| Later, single dark window | 2051ms | 0 / 0 | 1317 |

Both dark windows were attached and focused, with `windowLightStatusBar=false`.
The first native method failed only its visible-light-pixel assertion in 6.189s;
its theme, request, focus and guard assertions passed. The later single method
passed in 6.308s, and its unedited screenshot shows white status-bar glyphs.
These are separate runs, not a combined 2/2 acceptance result.

The later image supports a screenshot-timing/SystemUI-transition explanation.
It does not establish the exact platform cause: the first run changed from light
to dark, while the later run opened one dark window. The appearance getter
reports the requested mask, so its value alone does not prove rendered contrast.
No change to the existing `Ui` appearance logic was justified by these results.

## Evidence and boundaries

All original screenshots, lifecycle JSON, first failure and later result remain
in their dated private evidence archive. Device/host image hashes match. The
later raw 320×640 PNG SHA256 is
`11296e466bd8461ca150a12b135daf95010e2fd074faf672677d75b6c4f27bbc`.
Both observations used the identical debug app SHA256
`dea46bc4e3b77c6ec840e5d3d71e18fbe8a4e4a6b23600397cf29a5dd3e8cfd5`.

The temporary diagnostic test was archived privately after verification. All
101 remaining Android sources match the frozen Home green build. No deliberately
failing diagnostic test was added to public CI. A conditional window-dump branch
did not execute. No screenshot alteration, reset, process kill, font/IME/animation
setting change or failed-path replay occurred.

Crash-log bytes remained unchanged. Four additional ActivityManager binder-freeze
`-11` lines across the two observations were retained; no new ANR/FATAL entry was
observed. This does not close historical failures, establish physical-device
performance, or certify every dark-mode screen.

Official API documentation checked 2026-10-06:
[edge-to-edge system bars](https://developer.android.com/develop/ui/views/layout/edge-to-edge-manually)
and [requested system-bar appearance](https://developer.android.com/reference/android/view/WindowInsetsController#getSystemBarsAppearance()).
