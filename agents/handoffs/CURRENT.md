# Current Handoff

Updated 2026-09-29. Public self-hosted preview remains online; stable release and
marketing readiness remain unproven. Local candidate now includes recoverable
shares (commit45f71a2) and the verified keyboard-history anchor fix (c0a7969).
This pass investigated retained ANR evidence; no runtime or artifact changed.

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
is the first goal turn with this specific unresolved tool block. The goal remains
active because independent evidence analysis made progress. No question is pending.

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

- `.local/releases/ux-polish-sep29/DropRun-0.5.1-android-candidate.apk`, SHA256
  `f7699308aac696a609e8bcc076a367eaacdd2a9a3876ed207b9bf7b4a07a0eae`.
  Version0.5.1/code14, package `app.droprun.mobile`. Stable certificate SHA256
  `d65e17ed1df9a1e0b8eabbc51675905673c8fdc1c44e7bb008237b6a0e82c5c4`.
- Previous candidate verified and backed up as `DropRun-0.5.1-before-keyboard-anchor.apk`
  in that directory, SHA256
  `d2c8084e7b76dbda8f173f9777498f90023c755f378e68b43e710ca6e65e3b51`.
  Older review files/backups remain. Private signing material is ignored.
- Windows unchanged `.local/releases/ux-actions-sep29-windows-final/`, unsigned by a
  trusted publisher. No upload/deploy/public artifact replacement this pass.
- Both debug apps force-stopped. Outboxes/import journals empty; preexisting unknown
  6d908a45... attachment preserved. No live Relay task or Codex operation started.
- Emulator baselines restored and both stopped: API35 DropRunTest/5556 font2.0,
  keep-awake3, auto-rotation1/user0; API26 DropRunApi26/5558 font1.0, keep-awake0,
  auto-rotation1/user absent. No build/test remains running.
- Public package on API35 is paired and force-stopped: use `.debug` only. Never run
  instrumentation and another UI automation client on the same device concurrently.

## This pass: narrowed ANR evidence

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

The history process-death probe is authorized but tool-blocked; resume only after
that execution restriction is resolved, preserving task/offset and all240 cached
records. No fixture was written and no process-death result exists. Independently,
the next useful ANR evidence is a trace beginning before ordinary local startup,
including service bind/callback timing; post-ANR logcat and fake-scheduler passes
cannot establish the cause. Do not rerun existing UI suites without new changes.

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

Only UX release record and this handoff changed. DropRunTest was started normally
after correcting a nonexistent AVD-directory override, then stopped. No debug App
process started, no settings changed and no fixture backup/seed file was created.
Candidate SHA256 was rechecked unchanged. No build, test or emulator remains running.
