# Share and default-model hierarchy — local native verification

Date: 2026-10-06 UTC. Unreleased Android source; public candidate0.5.5 bytes/version untouched.

## Changes

- ShareActivity removes only two duplicate Ui.label calls in project/intent steps. Main questions, dots, material, cached execution notice, Send, back, import and draft logic are unchanged.
- SettingsActivity.modelSection uses existing Ui.setting rows and one divider. Stable settings_model/settings_effort IDs and callbacks remain. Known effort IDs display effortWord; unknown IDs display verbatim. Choice callbacks still save the original available ID.
- Debug opt-in fixtures use guarded memory only. Existing fixture defaults stay intact. Share's local navigation bypasses hideKeyboard in this opt-in path only; no original IME path was attempted. Settings saveDefaults is a memory map/count spy. Bundle carries the memory values through Activity recreation.
- Existing LocalRecoveryTest focus lookups use stable IDs for changed control types; this old disk-backed scenario was not executed. The new bounded native test covers equivalent focus/cancel/recreation behavior in the guarded fixture.

## Evidence

- `original/` and `original-inventory.json`: 101 baseline source files. `build-red/` and `build-green/`: both exact102 source sets, logs, APKs and before/after hashes.
- Red native: 2 methods, 2 expected failures, 9.685s. Settings remained Button; Share question child index1. Three raw screenshots captured before assertions; no setup failure. Both Share eyebrows measured34dp in EN/light/font1.
- Green native: 2/2 in the same run, 92.494s. Each method covers EN/ZH × light/dark × font1/2. Activity-level font configurations only. No unrelated native suite rerun.
- Share: project selection and back-to-project preserve selected ID and programmatic note; main questions and material fully laid out; execution title/detail and Send are individually reachable by scrolling. No Send click. `incoming` stays null, forbidden/API/pair/save/start/submit counters0. Keyboard calls are counted bypasses, not IME verification.
- Settings: both rows at least48dp; complete title/value text and accessibility descriptions, enabled/clickable/focusable nodes. Select model and localized effort, cancel each, retain raw IDs/focus through render and recreation, then select unknown `future-effort` verbatim. Exactly3 memory saves per configuration; no preference edit, mode operation or network call.
- JVM24 passed, failure/error/skip0. lintDebug errors0/warnings30 (existing warning total retained). Build sources equal before/after.
- `screens-red/`:3 PNG+3 JSON. `screens-green/`:24 PNG+24 JSON. All54 artifacts match device/host SHA-256. Captures wait for focused/layout-ready windows and another2s once; originals keep320×640, full aspect and memory markers. No crop/repaint/repeated screenshot retry.
- API35 emulator320×640/density160. System font2.0, IME and animation settings equal before/after. Crash buffer hash equal. Four new ActivityManager `Unable to freeze binder … -11` lines;207 historical event lines rolled out of the ring buffer. This is not a clean physical-performance result. Both raw event files retained.

## Measured layout

- Normal EN Share project column221→187dp; note column467→433dp. Both remove34dp. Project question screenY remains409 because the bottom sheet itself gets shorter; note question moves210→176. No claim that Send appears initially in every configuration.
- Settings normal EN rows54/54dp (card224dp; old button card227dp); normal ZH60/60dp. Font2 EN model82/effort119dp; ZH96/96dp. Long English reasoning label wraps with complete value. The model card is a preference hierarchy, not a promised universal height reduction.
- Settings images capture the relevant scrolled region, not the entire Settings page. Share font2 images show the upper content; action reachability is separately asserted after scrolling.

## Exact frozen source

- `android/app/src/main/java/app/droprun/ShareActivity.java`: `190efd916af6c3ae81b4967f1475efaf4f4fd473be8fe1b449752240ebd46954`
- `android/app/src/main/java/app/droprun/SettingsActivity.java`: `ba61d44e634bee86ac7e64b42efe9594d58cfc6f9151bbeb5924e4e20751b2fe`
- `android/app/src/androidTest/java/app/droprun/LocalRecoveryTest.java`: `55660052826770e62b8e5745ef13dff3d55b9535d3b764e71fb9df22d810a5a2`
- `android/app/src/debug/java/app/droprun/DemoShareEditorActivity.java`: `d654b4cc998e02658180ddcf963bab9688c1d2a5d113e66394ba57b5f77bc109`
- `android/app/src/debug/java/app/droprun/DemoSettingsRecreationActivity.java`: `579b61b56a9362c5e10f0db21b321479dabb7336f628efe80a03006c7131f22a`
- `android/app/src/androidTest/java/app/droprun/ShareSettingsHierarchyTest.java`: `6f9319ad348792ee97f54babb94138a33ec5959dbd927d3ad564225b9cb1e404`

Green debug app SHA-256: `76539c00ca43ffc1eda6c1ffe750e506476074b202ac1e885fc6aef2d91e8657`.
Green test APK SHA-256: `b1a2b3268545497eb0c7e0a7bb6ed0afdc73098fb1f63edeaadc676f6ba5ad5b`.

Ui, StyledActivity, Home3 files, manifests and other production source remain byte-identical to baseline. Release configuration was not edited. All102 current source hashes equal the green build snapshot. Instrumentation-only code lives in debug/non-exported fixture/test paths; no manifest export change.

## Limits and handoff

No real Relay/task/project material, business preference/cache/outbox, pairing, download, Send, external navigation, clipboard or process-loss path. No killing/resetting/clearing, OS font/IME/animation change, true TalkBack, physical touch/IME/performance or stable-release claim. Programmatic note entry does not verify soft-keyboard typing. Memory save/recreation does not verify disk persistence or process death.

Independent source, inventory and original-image review passes. This public record keeps the private frozen evidence facts; original archives remain unchanged.
