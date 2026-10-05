# Current Handoff

Updated 2026-10-05 UTC. **v0.5.2-rc.1 is publicly available.**
Stable launch and marketing handover are not achieved. Preserve the complete
Android+Windows self-hosted goal and the user's local-only validation boundary.

## Latest session: focus, pairing and README follow-through

- Diagnosed the earlier large-text green tint: Android's default focus highlight
  covered the whole focused ScrollView/share root, not a font-specific palette.
  Ui.page/frame now suppress that container highlight while keeping keyboard
  focus, button rings and Enter activation. Original PNGs remain untouched.
- Background Settings render now restores its existing appearance/language IDs
  alongside model/effort. Manual pairing errors appear after the fields, scroll
  fully into view, retain entries and never submit invalid input. Strict parsing
  and source confirmation remain unchanged.
- Independent debug probes do not seed fixtures, write settings/outbox, pair or
  call a Relay. Native API35 font200/320dp/motion-off passed8/8. A separate4/4
  capture run at720×1440/density360 produced six raw PNGs; Settings and EN/ZH
  error views were inspected. Surface samples are #F7F7F2 and #FFFFFF, without
  whole-page lime. JVM24/24; release lint0errors/28warnings; builds passed.
  Reproduced failures and initial probe corrections remain in
  `.local/ux-oct5-color-audit/`. Source/APK hashes and scope:
  [UI follow-through](../../docs/releases/ui-followthrough-2026-10-05.md).
- README's existing demo label precedes its tall tour; the English home description
  now includes saved shares. PRD/contracts/changelog track the exact behavior.
- These fixes are newer than downloadable0.5.2-rc.1; no release package/tag/site
  media was replaced. Curated source publication and matching CI are checked
  separately. No rejected command, draft removal or genuine submission occurred.
- Original emulator size/density/font/animation values restored/read back:
  320×640,160dpi,2.0/null/1/1. Only internal PNGs were added. Original uncommitted
  film/LAUNCH_KIT and their handoff section remain separate and unpublished.

## Previous session: native UI, brand and README refinement

- The user explicitly rejected stopping independent product work on unavailable
  real-environment acceptance. Continue actual UI/UX, brand, website and README
  improvements; real-device/submission gates still apply to stable launch.
- Native Java Views now share warm neutral/lime/dark surfaces, quieter project
  cards, contextual share steps, result-first delivery and grouped settings.
  Pair actions precede setup help. Press, focus, selection and disclosure feedback
  respect animation-off. Discard confirmation accurately names phone copies/note;
  saving does not imply server receipt. Plan review no longer implies isolation.
- Sixteen fresh native EN/ZH/light/dark screens and unretimed 9.084s/12.192s UI
  recordings feed the bilingual README and website. They visibly use Demo data;
  no agent ran. Capture provenance remains 0.5.1 development UI despite the later
  source bump to 0.5.2/code15. Media manifest: assets/brand/readme-media.md.
- Node224/224, JVM24/24, release lint0errors/28warnings, ten focused native
  API35 checks at 200% font/animation-off, and one later long-status typography
  native check passed. Its first failure was fixture task-list precedence, fixed
  before the passing run. Seven focused setup frontend cases passed.
- Windows setup and pairing now share the visual language. Deployment-field
  validation stays local, saved/last-verified/live connection states are distinct,
  and pairing requires a live Relay response. Four pure pairing tests cover
  confirmation/token, escape handling, clipboard failure and expiry priority.
  A separate read-only fixture server was visually inspected; no owner commands.
- Sites version5 succeeded at 2026-10-05T06:52:13Z from hosting source
  f781483921365cf74aef309d2be04c4e44ba747f. EN/ZH now point to 0.5.2-rc.1 and
  current setup guides. Installation precedes boundaries in the specified page
  order. Public light/dark desktop views and visible download controls were
  inspected; local 320px evidence remains separately scoped. Anonymous v5 audit
  passed at 06:55:48Z: two pages, ten CTA targets, seven documents and four small
  resources. Original markup matches the hosting build; added platform script
  bytes are recorded separately. Unchanged media reuses explicit dated v4 proof.
  [Public audit](../../docs/releases/public-delivery-2026-10-05-rc052.json) retains
  the original eight complete package downloads and hashes.
- Curated source07eb357361e0086c02e848f4f66a08932e49c03f is public. Source CI
  [37272891984](https://github.com/MrMaii/droprun/actions/runs/37272891984) passed
  both jobs, including Node224/224/no skips and Windows package construction.
  The documentation/pointer commit912017c also passed both CI jobs in
  [37274120930](https://github.com/MrMaii/droprun/actions/runs/37274120930).
  Final publication commit374faf0 CI37275265534 passed Android but Node223/224:
  one setup-language test asserted before its asynchronous status read completed.
  Original log retained as `.local/ux-rc052-final-ci-failed.log`. Only the test was
  corrected to await refresh before the language click, preserving all assertions.
  Focused browser case1/1 and subsequent full local Node224/224, no skips,31.800s
  passed; log `.local/ux-rc052-test-sync-node-final.log`. No product files or
  released-source/package provenance changed. The failed CI stays historical;
  the subsequent matching-main run is recorded under `.local/ux-rc052-test-sync-ci*`.
  Final test-fix commitce404486b6ff2987add4658b80713d4c40d719cb passed both jobs in
  [37276025057](https://github.com/MrMaii/droprun/actions/runs/37276025057).
  A clean managed checkout installed74 packages/0vulnerabilities, then built
  signed Android0.5.2/code15 and Windows0.5.2 without product installation.
- Independent complete inventories, ZIPs and source blobs match07eb. Android
  stable certificate unchanged; public APK has no Demo entries/debuggable flag.
  Windows installer NotSigned remains disclosed. All eight0.5.2-rc.1 assets were
  anonymously downloaded and matched exact bytes/API digests at
  2026-10-05T06:41:44.766Z. [Current record](../../docs/releases/0.5.2-rc.1.md)
  owns package hashes and the remaining acceptance gates.
- The dedicated emulator remains running. Original size/density/font/animation
  values were restored/read back (320×640,160dpi,2.0/null/1/1). No true pairing,
  Relay submission, Codex work, owner cloud change or process-loss probe occurred.
- The owned managed build checkout is archived; final artifacts and verification
  logs remain outside it under `.local`. The two read-only preview servers were
  stopped after checking their process identities; ports 4178/4181 no longer listen.
  Temporary browser tabs are closed; the user's original website tab remains.

## Previous candidate snapshot: startup and preview shutdown repair

- Windows launch/update/uninstall, portable startup, scheduled Connector and media
  extraction now pass a process-only execution policy to owned script processes.
  Nine source sites/eight runtime sites verified. Persistent policies are unchanged.
- Owned preview shutdown waits for process closure before project work proceeds;
  termination failure/timeout retains handles and blocks the operation. Concurrent
  stops share one result. Exited/signalled PIDs are not killed again. Four added
  lifecycle cases and native PID/port assertions cover this behavior.
- Runtime/tag/package source: a16c178a6f43d336e2695e03de95bea7c3a0c272.
  Android source/UI is unchanged from rc.1; signed artifacts were rebuilt. Exact
  source and all payload inventories were verified. Android certificate retained;
  Windows installer NotSigned disclosed. Complete provenance and limitations:
  [current candidate record](../../docs/releases/0.5.1-rc.2.md).
- Local Node 218/218, no skips. [Released-source CI](https://github.com/MrMaii/droprun/actions/runs/37264031106)
  passed both jobs. First temporary cleanup EBUSY and unrelated Miniflare ECONNRESET
  causes remain unknown; original logs retained. Eight local probes confirmed
  exit/close and immediate cleanup; only three additionally checked OS PID/port.
- Published a non-draft prerelease with eight assets. At 2026-10-05T04:45:48.202Z,
  all eight complete anonymous downloads matched local bytes and API digests;
  annotated tag resolved to the package commit. Both site languages, five current
  documents and ten site resources were public. [Audit](../../docs/releases/public-delivery-2026-10-05-rc2.json)
  owns this snapshot. Earlier release/audit records remain historical.
- Website remains https://droprun.dengmaizi0802.chatgpt.site with /zh/ and Sites
  version3. Its existing Releases CTA exposes the new candidate. No site source
  change/redeployment. Screens and tours are labelled demonstration material.
- Clean Windows capability inspection found no usable isolated guest; no product
  installation, owner deployment, real Relay/Codex task or social post occurred.
  Build worktree archived; needed final artifacts/logs retained outside it under
  .local/releases/rc2-oct5-final-{android,windows} and public-0.5.1-rc2-final.

## Authorization and blocking conditions

- User permits local UI validation only and retained real submission acceptance.
  No physical Android is available. The candidate instruction does not waive
  stable gates or authorize genuine tasks, owner cloud changes or social posting.
- User explicitly approved the 240-row local synthetic-history/process-loss probe.
  Automatic approval review still rejected the command with only `blocked by policy`.
  It remains unexecuted. Do not ask again, retry via another tool/wrapper, or claim
  that a fixture backup/cache or process-death result exists.
- The previous goal turn was progress: runtime fixes, rc.2 publication, complete
  anonymous downloads and final main CI37265035932 (both jobs passed,218 tests).
- A previous resumed run was marked blocked after three repeated stable-gate
  turns. That audit is historical. The user's new request resumes independent
  design work and supersedes its instruction to wait for external conditions.
  The goal is active while meaningful UI/UX work proceeds. Do not equate local
  screenshots or design improvements with genuine submission acceptance.
- The original SyncJob ANR at 2026-09-29 18:10:24.485 UTC remains unexplained; retained
  logcat begins 3.996s later. The scheduler change is not proof of resolution.
- Automatic approval review also rejected a compound command to discard one
  local Demo draft and navigate normal Pair, with only `blocked by policy`.
  It was not executed or retried. The draft remains and final normal Pair capture
  is missing; final large-text Pair layout captures exist.
- Mandatory open gates: genuine 3-project/5-consecutive-handoff/20-source reading
  evidence and recovery; physical Android accessibility/frame/press measurements;
  clean Windows/fresh owner Cloudflare/physical-phone install-deploy-pair-deliver-
  upgrade-uninstall; provider-permission loss/storage/size limits; blocked local
  process-loss probe; genuine completed-task demo and final posters after UX closure.
  See [UX evidence](../../docs/releases/0.5.1-ux.md) for the dated UI verification.
- Existing uncommitted promo-film/launch-kit changes remain separate and unpublished.
  No final poster generation or marketing handover claim.

## Single recommended next action

Review share-success feedback readability. ShareActivity.landed() currently
reveals the detailed outcome for100ms with motion off, or280ms including a220ms
fade when enabled. Determine whether offline/notification instructions can be
read, then apply the smallest verified improvement while retaining immediate
Close and prompt return for routine saves. Validate only an independent local
UI path; do not send tasks, clear the retained draft, navigate normal Pair or
repeat the rejected process-loss probe. Genuine acceptance remains open; current
download evidence belongs to0.5.2-rc.1 and the original film remains separate.

## Files this pass

Current refinement: Ui.java, SettingsActivity.java, PairActivity.java, three
debug-only independent probes and three native test classes, debug manifest,
bilingual README, PRD/contracts/changelog, UI follow-through record and handoff.
Local evidence: `.local/ux-oct5-color-audit/` (original red logs, green logs,
source/build hashes, pixel analysis, raw screenshots and restored settings).

Current work: native visual hierarchy and onboarding/history, shared feedback,
long-status typography, landing build source/CSS/JS, bilingual README/native media,
Windows setup/pair rendering and tests, Material icon notice, runtime0.5.2 metadata.
Local evidence: .local/design-oct5/review.md; .local/ux-oct5-ui-verification.json;
.local/ux-oct5-node-version-final.log. Dated scope: docs/releases/0.5.1-ux.md.

Windows installer/launch/service/setup/media tools and focused tests; preview
lifecycle and native cleanup tests; contracts, PRD, self-hosting guides, changelog,
readmes, roadmap/context, current rc.2 release record, anonymous audit and handoff.
Git author Thomas Deng <150266369+MrMaii@users.noreply.github.com>.
