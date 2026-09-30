# Current Handoff

Updated 2026-09-29. Public self-hosted preview remains online; stable release and
marketing readiness remain unproven. Local candidate now includes recoverable
shares (commit45f71a2) and the verified keyboard-history anchor fix (c0a7969).
This pass fixed Windows distribution provenance/inventory and verified a new
installer, portable ZIP and source ZIP. Runtime payload files remain unchanged.

## Current authorization

Local UI validation only; retain real submission acceptance. No real Relay
submissions, grants, remote cleanup, Codex execution or social posts. Physical
Android unavailable; candidate work accepted. Marketing posters remain behind the
verified UX gate. Do not retry the previously rejected live share by another route.

The user replied “可以，我批准，你继续” to the emulator-start question. Normal
hidden startup then succeeded for the two existing test emulators. The startup
blocker is resolved; no source download was retried.

New local-history probe: automatic review rejected a command to check the debug
fixture origin, back up its preferences, insert240 synthetic cached tasks and open
MainActivity. The user explicitly answered “允许这项本地恢复验证”; the same command
was retried once with that authorization and rejected again, only `blocked by policy`.
Do not ask again or route the rejected operation through a wrapper/other tool. This
is the third goal turn with this unresolved tool block; no further retry occurred.
Independent Windows release-tool work made progress, so the goal remains active. No question
is pending. Do not manufacture more approval questions for the same rejection.

## Previously completed and verified

- Deterministic API35 reproduction: keyboard focus on the history header, no native
  selected row, two updates before layout; visible item170 became168. Home40
  similarly became38. A first fix using pending selected position failed again.
- Home/history now anchor by rendered record ID and offset in touch mode or when
  no laid-out native selected View exists. Existing keyboard selections use native
  stable-ID restoration. Restored Activity state remains higher priority.
- Three new native cases check header focus, Home Settings focus and task-card
  focus/Enter target after updates. API26 restores a selected View; API35 does not.
  Fixture setup handles input mode before requesting header focus. Exact viewport,
  focus and navigation assertions remain, without expected-result polling.
- Final full suites at200% font: API26 **87/87,99.456s**; API35 **87/87,143.41s**.
  Each includes59 LocalRecovery,15 attachment,11 draft and2 sync cases. Logs:
  `.local/keyboard-anchor-verified-full-api{26,35}.log`.
- Final focused cases pass3/3 per API. Native selection logs, deterministic failure,
  rejected first fix and earlier fixture failures are retained. Canonical evidence:
  [UX release record](../../docs/releases/0.5.1-ux.md), latest keyboard-anchor section;
  [UI matrix](../../docs/releases/0.5.1-ui-matrix.md).
- Offline debug/test/release build,12 JVM tests and lint passed42s. Lint0 errors,
  31 warnings. Final test-only build passed24s. Production source unchanged after
  signing; apksigner verifies stable cert. Node/Windows unchanged this pass.

## Local candidate and environment

- New verified distribution `.local/releases/android-distribution-sep29-final/`
  includes `DropRun-0.5.1-android.apk`, matching Android source ZIP, license/notice
  files, BUILD-MANIFEST.json and SHA256SUMS. APK matches the named candidate below.
  Export source commit9f75ae1bf3ea3397a4d784f2f31898d765424cc3; all8 inventory files
  and77 archived source files verified. The archive honors declared batch-file
  CRLF; other source bytes compare directly with Git blobs. No source mismatch.
  `.local/android-distribution-verification.json` records the local result.
- Scripts now read built version instead of hardcoded0.5.0, refuse existing output,
  require clean committed source and verify signatures. Global archive newline
  behavior is fixed independently of machine autocrlf. Real build passed11s;
  packaging integration tests3/3 in5.624s use fake build/sign tools. Real signing
  was independently checked. Earlier preliminary export is retained as evidence;
  use only the `-final` directory. No public upload or native test rerun this pass.
- `.local/releases/ux-polish-sep29/DropRun-0.5.1-android-candidate.apk`, SHA256
  `f7699308aac696a609e8bcc076a367eaacdd2a9a3876ed207b9bf7b4a07a0eae`.
  Version0.5.1/code14, package `app.droprun.mobile`. Stable certificate SHA256
  `d65e17ed1df9a1e0b8eabbc51675905673c8fdc1c44e7bb008237b6a0e82c5c4`.
- Previous candidate verified and backed up as `DropRun-0.5.1-before-keyboard-anchor.apk`
  in that directory, SHA256
  `d2c8084e7b76dbda8f173f9777498f90023c755f378e68b43e710ca6e65e3b51`.
  Older review files/backups remain. Private signing material is ignored.
- New Windows distribution `.local/releases/windows-distribution-sep29/`, built
  from43f3e00d628d547adc9bb8b1ba763a28761e51b9 with Inno7.1.0; Authenticode NotSigned.
  The manifest now covers2020 payload files, including the formerly unlisted ZIP
  helper, and binds source commit/archive hash. All2021 portable entries and272
  source files verified; all three artifact sidecars match. All previous payload
  files are unchanged; the added inventory entry already existed in the old ZIP.
  Exact artifacts/hashes are in the latest UX record and
  `.local/windows-distribution-verification.json`. Earlier Windows packages remain.
  Two packaging tests pass2/2 in8.172s; no runtime/native/full Node suite rerun.
  No upload, installation, deployment or public artifact replacement this pass.
- Both debug apps force-stopped. Outboxes/import journals empty; preexisting unknown
  6d908a45... attachment preserved. No live Relay task or Codex operation started.
- Emulator baselines restored and both stopped: API35 DropRunTest/5556 font2.0,
  keep-awake3, auto-rotation1/user0; API26 DropRunApi26/5558 font1.0, keep-awake0,
  auto-rotation1/user absent. No build/test remains running.
- Public package on API35 is paired and force-stopped: use `.debug` only. Never run
  instrumentation and another UI automation client on the same device concurrently.

## Prior pass: narrowed ANR evidence

- The original ANR at18:10:24.485 UTC was a20,002ms service-execution timeout;
  app PID8676 had27 Java threads. Main was in nativePollOnce; instrumentation was
  inside LocalRecoveryTest.seed, waiting for an Activity enter animation.
- Retained logcat begins at18:10:28.481,3.996s after the recorded ANR timestamp.
  Repeated bind-timeout/job-missing warnings occur later. They do not reveal what
  delayed the original service start. No pre-trigger timeline is available there.
- Commit604d556 subsequently stopped redundant periodic-job registration and
  removed fixture-only Home launches. Those are verified changes, not proof that
  this ANR was fixed. Current scheduler tests substitute a fake JobScheduler and
  validate registration policy; they do not exercise actual service binding.
- The official Android ANR guide confirms idle-main execute-service reports still
  require investigation. Evidence/official source are in the latest UX record.
  Do not change SyncJob speculatively or dismiss the event as an emulator artifact.

## Single recommended next action

Audit the public delivery boundary read-only: actual website download targets,
repository/release visibility and which published version users receive versus
these local candidates. Identify remaining launch gates from current evidence;
do not silently publish or relabel candidates as stable. The history process-death
probe remains authorized but tool-blocked; do not retry it through another route.
ANR diagnosis still needs a pre-start timeline. If no independent meaningful work
remains, the persistent block has reached the three-turn audit threshold; set the
goal blocked instead of repeating status-only turns. Progress this pass was real.

## Open release gates

Real receipt/execution/report/follow-up; physical Android/TalkBack/performance;
20 genuine sources,five full tasks over three projects; clean Windows/fresh
Cloudflare setup/upgrade/uninstall; real provider URI loss/storage-full/max-size.
Original API35 SyncJob ANR2026-09-29 18:10:25 root remains unproven. Earlier failures
remain recorded. Prior QA pairing/catalog remote cleanup remains unauthorized;
ignored `.local/ux-cloud-check.mjs` describes it. No task was sent.

## Historical public baseline (not refreshed this pass)

Website https://droprun.dengmaizi0802.chatgpt.site (`/zh/`), repo
https://github.com/MrMaii/droprun , prereleasev0.5.0-rc.1 source
`90718812d764c2d590e2d0db2d1381e2a2dfccaf`; CI run36290007838.
GPTSites projectappgprj_6ab726bb8cd48191acd3234da262627d version2; do not duplicate.
Git author Thomas Deng <150266369+MrMaii@users.noreply.github.com>.

## Environment and files this pass

Commit43f3e00 changes scripts/package-windows.mjs, adds windows-package.test.mjs,
and updates DEVELOPMENT/PRD/CONTRACTS. This handoff and the UX release record store
final artifact verification. Android candidate unchanged. No emulator was started.
No build/test/compiler remains running. Prior Android distribution source9f75ae1
and its8-file/77-source verification remain valid for that unchanged APK.
