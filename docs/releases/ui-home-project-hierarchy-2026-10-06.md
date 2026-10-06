# Home project hierarchy — local native verification

Date: 2026-10-06 UTC. This record covers one local Android refinement. It is not
a signed-candidate, genuine-handoff, physical-device, TalkBack or performance
acceptance record. Parent owns product contracts, Git and publication.

## Change

Only MainActivity production code changed: remove the 44dp project initial tile,
its 12dp spacer and the now-unused holder field/binding. Names and counts begin
at the same content edge as state. Existing card padding, typography, chevron,
counts, status, used-project filtering, navigation and recovery behavior remain.
Ui.projectTile/projectInitial stay in place for Share. No framework or theme
change. The accepted ADR 0016 identity/count/state hierarchy remains the basis.

The existing non-exported DemoHomeRecoveryActivity gained an opt-in three-record
memory scenario and per-Activity font Configuration. The test temporarily sets
hierarchyFontScale and restores it in finally. Default recovery fixture data and
light appearance are retained. No manifest or existing recovery test changed.
The new HomeProjectHierarchyTest never clicks a project, runs a sync, or reads
business files/preferences/cache/outbox. Existing fixture guards remain active.

## Red and green evidence

- Red build succeeded in 51s (host52.3733977s). All101 app/src file hashes match
  its frozen source snapshot. Native1 method failed as intended in3.181s
  (host5.8199238s): `Project identity begins at the card content edge
  expected:<40> but was:<96>`. The first actual row measured168dp name width,
  left96dp,179dp card height. The title `Studio mobile app` occupied two lines.
  Raw screenshot/metadata were captured before the failing assertion.
- The production deletion was applied only after that red result. Green build
  succeeded in44s (host44.480727s). JVM24 passed with0 failures/errors/skips;
  lintDebug0 errors,30 warnings. All101 source hashes match before/after build
  and final source verification.
- Green native2/2 methods passed in19.616s (host21.5571878s): new hierarchy method
  covers EN/ZH × light/dark × font1/2 and all3 laid-out rows; the affected existing
  largeTextShowsCompleteProjectNamesInBothLanguages method also passed. This is
  one bounded matrix; no unrelated/native full-app suite was repeated.
- Every hierarchy row's real name column measured224dp; its left edge equals
  card content/state/counts at40dp. Names, counts and state render all characters;
  card minimum48dp and its original complete clickable/focusable/enabled native
  accessibility description pass. Checks use actual attached native views.
- Normal EN first card179→150dp (29dp less); first two same-prefix project names
  are now one line. Green normal ZH first card164dp. Atfont2 first card is EN272dp
  and ZH273dp; full project-name suffixes remain. No before/after height claim is
  made for ZH orfont2, since the minimal red captured only EN/light/font1.
- Nine actual320×640 raw PNGs and matching JSON: one red and eight green. All18
  device/host hashes match. All were visually reviewed; probe markers readable.
  Normal screenshots show identity before counts and state, with aligned edges.
  Atfont2 the first card is fully visible. The entire list need not fit at once.
  Native text-layout checks cover all3 rows; screenshots show their actual
  viewport, without cropping/resizing or invented completed work.

## Frozen files and identity

- Red: build-red/source/, build-red/*.apk, native-red.log,
  screens-red/home-en-light-font1.{png,json}.
- Green: build-green/source/, build-green/*.apk, build-green/jvm/,
  build-green/lint-results-debug.xml, native-green.log.
- Green raw images: screens-green/home-{en|zh}-{light|dark}-font{1|2}.png;
  same basenames.json contain native geometry, marker and guard counts.
- Source provenance: source-final-verification.json and each build's
  source-before.json/source-after.json; all101 match final green sources.
- MainActivity.java SHA256:
  14c64aa9a54f76e88cf37ab4e3d145b6277ded0fa9b7433e3db2c4778371280f
- DemoHomeRecoveryActivity.java SHA256:
  b45e7062a45bc4994b87ae71341a70deb3c32c6264beb22ace3dd01a490f5ba7
- HomeProjectHierarchyTest.java SHA256:
  096fa58ecccb7f00d612ed91663e7423a6a4388adbaf81be97d586e20015a9cc
- Green debug app SHA256:
  dea46bc4e3b77c6ec840e5d3d71e18fbe8a4e4a6b23600397cf29a5dd3e8cfd5
- Test APK SHA256 (identical in red/green; only production code changed):
  9c6ccd759ac7133f2210662008bc302cdeef4d316fc223f0a4f1b1655ce1a473

## Boundaries and retained limitations

Only authorized `adb install -r -t` preserved existing debug data. Emulator5554
API35,320×640,density160 remains unchanged. Device font_scale2.0, IME and animation
settings compare byte-for-byte before/red/green. Font1/2 overrides belong only to
diagnostic Activity contexts. Ui.dark/L are in-process fixture state; no saved
appearance/language preference was edited. API/navigation/service/files/outbox
guard0 and sync_calls0 in all captures. No kill/reset/clear/IME/OS-setting action,
real Pair/Send/download, large dataset or rejected-path replay occurred.

Crash log SHA256 unchanged. Four new ActivityManager `Unable to freeze binder
... -11` lines remain in events-new-lines.log; no new ANR/FATAL entry was observed.
This is not a claim that historical environment/performance failures are fixed.
Dark images contain low-contrast system status-bar glyphs; this unchanged area
was not repaired or certified by this card-only change. No genuine TalkBack or
physical rendering/frame claim. The public0.5.5 candidate is unchanged.

All original failures, PNGs, build logs, source snapshots and APKs are retained.
No Git/commit/version/product-document changes were made by this subtask.

## Later system-bar observation

The original dark screenshots and their limitation above remain unchanged. A
separate [bounded screenshot-timing observation](ui-system-bars-observation-2026-10-06.md)
recorded readable white glyphs later, with the same app and appearance request.
No production correction was made, and this does not broaden the card-only
acceptance claim.
