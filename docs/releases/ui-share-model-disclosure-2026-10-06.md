# Share model disclosure — native UI refinement

October 6, 2026 UTC. Development source after the 0.5.6 package freeze; captures
precede the subsequent 0.5.7/code20 version bump. This record owns the local
checks. [Current candidate](0.5.6-rc.1.md) owns shipped package evidence.

## Result

The optional model control now has a visible title, current summary and state
chevron. It retains the original row, gauge callback, focus and accessible name.
Effort choices use complete localized option rows instead of narrow raw-ID
segments and a separate translation key. Unknown IDs remain readable verbatim.
Model/effort IDs, selection tags, project, note, checkpoint and submit paths stay
unchanged. A minimum of 48dp applies only to these Share options; Ui/Main/Settings
are unchanged.

The chevron moves180↔270 degrees over 180ms; existing panel expansion remains 280ms.
Disabled motion cancels the animator and sets the final angle directly. This
branch was source-reviewed only; it is not a native Remove animations pass.

## Evidence

- Baseline102→green103 source files: production Share, one opt-in memory fixture,
  one new instrumentation test;100 other source files and the old402-file archive
  are byte-identical. Independent review found no mismatches or blocking issue.
- Red1 expected failure in9.948s: non-default model target42dp, no visible
  disclosure title/chevron, effort cells52–53dp wide. Normal EN medium split
  across lines. Three raw captures precede the aggregate assertion.
- One green batch2/2 in93.465s: new disclosure and existing safe Share return/note
  method, each EN/ZH × light/dark × font1/2. Not16 separate native tests.
- Models42→48dp normal /91dp at font2; effort rows280dp wide, normal48dp,
  font2EN54dp and ZH61dp for Chinese meanings. Full text, exact IDs, selected state,
  keyboard focus tags and individually reachable controls verified. Programmatic
  note and project survive the safe local return path.
- JVM24 passed, failure/error/skip0; lint0errors/30warnings. Red/green test source
  differs by explicit keyboard-focus mode and an unused-import removal; test
  APKs differ. Original red evidence remains, not represented as identical tests.
-24green PNG+24JSON and3red PNG+3JSON:54 device/host hashes match. Raw320×640
  captures retain memory markers, without crop/repaint. Component-scrolled
  captures do not claim the whole form fits at once.
- Memory fixture has no import object or persistence; checkpoint calls are counted
  no-ops, not zero method calls. Persistence/network/save/pair/submit/start and
  forbidden counters0. IME/font/animation settings unchanged. Crash buffer
  identical;4 new binder-freeze warnings and4 old event lines rolling out retained.

ShareActivity SHA-256: `2455f8c14db90a8b413687ba0397c2338da9b2440d9b443c1f9ab399a94702d4`.
Debug app: `05f7af44367b039b14ee3139817e57409a84570372f9f4e9fd5dbae431f63292`.
Test APK: `197dd4b6f7fc2ee97618c2a246b773cba844fd3d88540be99ebbe0bdd6b93b64`.

## Limits

Memory/sample UI is not a genuine handoff or signed-package recording. Real IME,
TalkBack, physical press/frame timing, motion-off runtime, rotation/recreation,
disk persistence/process death remain unmeasured here. Source review of Bundle
paths does not establish those runtime results. No denied path was replayed.
README copies retain the original markers and dated [media provenance](../../assets/brand/readme-media.md).
