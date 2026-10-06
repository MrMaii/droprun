# Share confirmation and transition — development UI

Recorded October 6, 2026. This is `0.5.11-dev`, Android code 24. The signed
[0.5.11-rc.1](0.5.11-rc.1.md) remains bound to source `c0a49f1` and does not
contain this layout. No stable launch or marketing readiness is claimed.

## What changed

The destination and received-material disclosure now share one matte surface
before the optional note. The complete destination uses 17 sp medium text and a
project initial; at 150% text and above the decoration disappears to preserve
reading width. The group is readonly. Permission, execution setting, material,
model, draft and Send keep their existing meanings.

Choosing an already allowed project advances directly to the note. The outgoing
list no longer reorders the selected row or inserts a 160 ms delay. Existing busy,
step, availability and permission guards remain; no new approval is implied.

The scrim cancels its previous animator and continues from its current color,
with the existing 240 ms entrance and 200 ms exit durations. `ColorDrawable.setAlpha()` multiplies the
base alpha and applies integer rounding, so reusing `getAlpha()` as its next
input is not an exact continuation. `ObjectAnimator.ofArgb` animates `color`
instead; the disabled-motion path sets the target color immediately.
[AOSP ColorDrawable](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android15-release/graphics/java/android/graphics/drawable/ColorDrawable.java)
was checked on 2026-10-06. Correct target alpha is 102 in light mode and 153 in dark
mode; this also makes the scrim darker than the older attenuated 40/92 endpoints.

## Build and native evidence

The final offline debug build returned 0 in 24.875 s with empty stderr:
73 actionable tasks, seven executed and 66 up to date. JVM tests and the main/unit
lint analysis/report were up to date; Android test compilation and Android-test
lint analysis executed. This is not a fresh full JVM/lint rerun.

| Binding | SHA-256 |
| --- | --- |
| Frozen 134 Android sources/four Gradle configurations | `b5a7abdb4a8dd653fe10616ecdd16ab2cdf166fa86ae71d39a0efc6c4b04a8d8` |
| Debug app APK, 2,183,617 bytes | `088e5435766cb37c211d95dd98b3f90c987052ffa1dcc7425d12a321db980c53` |
| Debug test APK, 2,024,178 bytes | `018c864e5f74c71e4ac3920438de117a9f41ee469a4fa7878423b1b6d4719ae5` |

The final native owner has 45 returned child commands, all exit 0/empty stderr.
Installed APK hashes, source closure and ten read-only device observations match
before/after. Three individually selected methods each passed 1/1:

| Method | Time | Actual scope |
| --- | --- | --- |
| `ShareContinuityTest#authorizedPickAndScrimReverseStayContinuous` | 5.839 s | Two EN/light and ZH/dark normal-font memory windows; synchronous non-first selection, repeat guards, unchanged outgoing rows/material/options; 63 timestamped alpha samples. |
| `ShareDestinationReadingTest#completeDestinationLabelAndSendStayReachable` | 19.305 s | Four normal-font captures and two long-label 200% text windows; 22 complete line reads, 32 ID-hint characters, separately reachable Send and 50 emitted zero guard rows. |
| `SharePresentationTest#materialAndModelKeepClearLocalEntrypoints` | 22.625 s | Six known destroyed memory windows, 12 emitted zero guard rows; material/model headers and normal-font initial Send/draft reachability checked by the complete method. |

Motion emitted eight zero guard rows. Its summary reports 77 guard calls,
consistent with the source flow and 63 samples; those silent calls are not 77
recorded value rows. Light/dark reversal was 38 → 38 and 42 → 42, with the old animator
reported cancelled; outgoing samples descend to 0 and the next entrance reaches
102/153. RGB preservation is asserted by the frozen native method, but numerical
RGB samples are not emitted. Both windows record DESTROYED, terminated executor,
`cancel=0 / end=1`; cleanup completed without observing destruction-triggered
cancellation.

Presentation's summary says `header_clicks_expected=24`. The source asserts four
`performClick()` calls per window, but emits no individual click records; 24 is
not an actual logged click count. Its stdout has no nonce, geometry values or
capture-written events; identity relies on the owner's selected command/source/
APK binding. The destination probe owns the four images below.

## Original failure and correction

A separate preparation error expected three `133` occurrences where the owner
contained two; no build owner or Gradle process started. A corrected owner was
then prepared in a separate directory.

The first motion run failed 1/1 in 3.79 s on the test's strict destruction
expectation: `expected:<1> but was:<0>` cancel callbacks. Its 17 child commands
returned 0 with empty stderr; ADB exit 0 did not make that JUnit run pass.
Source binding and the failing stack support that scenario destruction, executor
termination, null scrim reference and stopped animator checks had already passed.
No DESTROYED event or event logging a cancellation count of 0 was emitted in
that failed run; the 0 is evidenced by the failing assertion. No suppressed
exception appears.

The correction changes the test/reader expectation to exactly one end callback
and zero-or-one cancel callbacks, allowing natural completion before destruction.
Production Share behavior is unchanged by that correction. The final test APK
was rebuilt and the three methods above ran on their recorded final binding;
the failed original remains retained. Final `cancel=0 / end=1` is not relabelled
as a measured destruction cancellation.

## Original native images

These 320 × 640 PNGs retain the memory-only marker and system bars. They show a
preset project, `example.invalid` link and blank note; Send was not activated.
Each public image is a byte-exact raw copy in both `assets/brand` and
`landing/media`. Root directly viewed all four raw stills. A separate visual
reviewer viewed four new and four earlier images and accepted the normal-font
static UI, including retained system bars. That reviewer authored the visual
proposal; this is a second pixel review, not an independent design audit.
The visual review report SHA-256 is
`7d9951b9c0df07a3f3f4b2fdffc90d22a8c6dc640ecc63891e57033f6019988b`.
It follows the native owner's earlier pending-visual status. Screenshot parsing
does not replace this direct review. Existing recordings retain their earlier
source binding.

| Image | SHA-256 |
| --- | --- |
| [English/light](../../assets/brand/source-share-confirmation-en-light-20261006.png) | `f2d161d5f4d6d81df5dc7520fad1b3597a6bf5f7190baba32509aad40c45619f` |
| [English/dark](../../assets/brand/source-share-confirmation-en-dark-20261006.png) | `66096d793408d2073bf79d1884b02952b8658eead1e5cb830094d91cb1b4466e` |
| [中文/浅色](../../assets/brand/source-share-confirmation-zh-light-20261006.png) | `a6c6725507d789da8a12616db9cd3b5a624361f7addc4a239b5e5ff954a4b60f` |
| [中文/深色](../../assets/brand/source-share-confirmation-zh-dark-20261006.png) | `9dc7b006d79b69a0d3f38e80276d6d5381b2b6fb09e6b79c0d1e0fc9491efbe7` |

Long-label checks place `ProjectPresentation.label` output into fixture memory;
they do not establish real catalog integration. The runs use guarded memory
preferences and no API/save/pair/submit/start/checkpoint-persistence actions.
They do not prove signed-package pixels, real keyboard entry, physical TalkBack,
frame/press performance, process recovery, instance isolation, genuine material
coverage or an installation-to-Codex-delivery loop. Stable acceptance gates remain
open.
