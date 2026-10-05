# Current Handoff

Updated 2026-10-05 UTC. **v0.5.3-rc.1 is publicly available.** Stable launch and
marketing handover remain open. Independent UI/UX work continues under the
user's local-only validation boundary.

## Current source and public delivery

- Candidate source/tag: 615c3f0958e4c7ffdbfb52c9a86f9b8902ce3492. All eight public
  assets were downloaded anonymously and verified; matching CI37354680526 passed
  all three jobs (core210/browser14, no skips/cancellations). Stable Android
  signing is retained; Windows NotSigned is disclosed. Canonical package/source
  evidence and open gates: [0.5.3-rc.1](../../docs/releases/0.5.3-rc.1.md).
- GPTSites version6 is live at https://droprun.dengmaizi0802.chatgpt.site/ and /zh/.
  Anonymous pages/CTA/resources and desktop installation/language controls were
  checked. The site uses dated, visibly labelled0.5.1 development Demo media;
  it does not show genuine Codex execution or the current package. Old Netlify
  and older release assets remain. Current audit:
  [public delivery](../../docs/releases/public-delivery-2026-10-05-rc053.json).
- Home/README source ebb0f74797183114c1dac95aeec8c6719d43cf6a is public. Matching
  CI37373157705 passed Android/core/browser, core210/browser14, no skips or
  cancellations, public-source347, site and Windows packaging. Anonymous README
  EN/ZH/recovery record match immutable blobs. Fresh public GitHub desktop
  screenshots show complete three-image workflows and captions in both languages.
  The media remain older Demo. Canonical scope:
  [Home recovery](../../docs/releases/ui-recovery-2026-10-05.md).

## Latest completed local UX work

- Home uses a compact sync notice and exact-cause details; manual checks show
  accessible busy state and reuse a real in-flight check. Enlarged project names
  wrap fully. The initial viewport fixture failure and title clipping regression
  are retained. The five-method EN/ZH final matrix passed but took2,680.552s;
  no new ANR/crash was found and the duration remains unexplained. Its older Main
  hash is not the later cache-only/manual fix: that separate readonly regression
  passed1/1 in1.542s, guard0, exact scroll/project retention and receiver restoration.
- Task results now emphasize a ready preview or terminal screenshots/files;
  follow-up is secondary. Missing/unavailable/expired states have accurate copy;
  guidance points to files only if that entry exists. Reopen/approval/permission
  rules are preserved. Final memory-only native4/4 passed15.038s/host16.0454176s:
  78 semantic configurations, six local-font1 layouts and four system-font2
  configurations with12 individual reachability checks. Actual primary/secondary
  styling and busy disabled states pass. JVM24/24; lint0errors/30warnings. Guard0,
  unchanged device settings/hashes, no new ANR/crash. All six raw320x640 PNGs
  inspected; actual scrolling excludes some/all topbar in several captures.
  [Delivery record](../../docs/releases/ui-delivery-2026-10-05.md) owns scope.
- Task's first2/2 finder failures and third fixture-creation crash are retained.
  Third attempt completed no methods/PNG; local font override was applied too
  late. Its independent attachBaseContext correction then passed fourth3/3;
  that four-PNG baseline predates final hierarchy. Sealed originals and final
  60-file inventory remain in .local/ux-oct5-task-preview/. These Task source
  changes are public at eb2dce8d33b5e54bee698ff8f4e2469eb790114a. Matching
  CI37378369508 passed all3jobs/core210/browser14/no skips or cancellations;
  anonymous5documents match immutable blobs. Eleven tested/public source files
  match exactly or by CRLF/LF alone. Not in downloadable0.5.3 or dated media.
- Share permission-success feedback now checks the original dialog, generation,
  selection, step and page at animation/delay/dismiss boundaries. Original native
  red1/1 reproduced closing a later Glass; final5/5 ENZH10scenes passed26.905s,
  guard0, unchanged settings/sources and no new ANR/crash. Build42s/JVM24/24/
  lint0errors30warnings. Initial debugAPI28/min26 lint failure is retained;
  the guard correction precedes the first native red. Permission requests and
  successful/failed server flow are unchanged. Direct onBackPressed is only a
  handler check, not real Back/IME acceptance. Canonical scope:
  [Permission motion](../../docs/releases/ui-permission-motion-2026-10-05.md).
  Private66-file inventory and threeAPKpairs are frozen in
  .local/ux-oct5-permission-motion/. No actual permission was requested.
  Public source26bc86fa9fc94982070b247af1c65a3cb1096b27 has matching
  CI37380000020 attempt2 green: core210/browser14/no failures/skips/cancellations,
  original successful Android job, public-source354/site/Windows packaging.
  Only failed core/browser jobs were rerun once, without source/assertion/timeout
  changes. Attempt1's local ECONNRESET and first-browser readiness timeout remain;
  no root cause is established. Anonymous5documents and10sealed sources match
  immutable blobs (8exact/2CRLF-only). Publication note is added this pass.
- Task result/preview/files now reuse one outcome card; follow-up, approvals and
  full report stay outside. No text, font, action eligibility or callback change.
  Native5/5 passed27.65s/host28.6324866s, JVM24/24, lint0errors/30warnings.
  Original4methods/frame/geometry helpers are unchanged. New24theme/language/
  text/state combinations check24summaries,12normal complete action regions and
  40font2 detail/action targets. All12raw320x640PNGs inspected; only delivery
  regions, not full pages or actual tasks. Four byte-identical ready PNGs are
  curated public evidence with visible memory/no-work markers, not README tours.
  [Single surface](../../docs/releases/ui-delivery-surface-2026-10-05.md) owns scope.
  Private262-file readonly inventory,94source equality, twoAPK hashes, unchanged
  130earlier host files/10devicePNGs/106red files/settings/crash buffer remain in
  .local/ux-oct5-delivery-surface/. The red1/1 structure failure and host-record
  mtime assumption failure are preserved. No device clock anomaly is inferred.
- README headers now surface candidate platform downloads and prerequisites.
  Setup remains the main CTA; installation follows the workflow. The short GIF
  stays directly visible with dated Demo attribution. Unreleased/source and
  unchanged0.5.3-rc.1 downloads are explicit. No README media changed; static
  EN/ZH candidate/media22hash checks pass. Fresh public rendering is pending.
- Earlier native color/focus, share feedback/editor, settings busy controls,
  manual-pair errors and README diagrams remain separately documented in
  [UI follow-through](../../docs/releases/ui-followthrough-2026-10-05.md).
- Real IME checks failed before the keyboard appeared; isolation reported
  Process crashed. Original Gboard/debug startup ANRs, CPU pressure and20-file
  hashes remain in .local/ux-oct5-ime/. No IME/Chinese composition/Back acceptance
  or cause is inferred. No kill/reboot/default-IME change occurred.
- Candidate build worktree is archived. Final artifacts/proofs remain outside
  it. Temporary browser tabs are closed; the user's website tab remains.

## Work prepared next

Settings source review finds mode busy/notice are Activity-local and not retained
when appearance/language calls recreate or configuration changes. Current tests
cover one instance/render only. A readonly red test is being prepared; no native
reproduction or production fix yet. Store.SYNC_LOCK serializes settings requests:
do not infer actual concurrent RPCs or a permanently wrong server preference.
The bounded next scope is the pending-mode feedback/request observation through
recreation, with no real save, preferences write or permission expansion.

## Authorization and blocking conditions

- User permits local UI validation only; genuine Relay/Codex tasks, owner cloud
  changes and social posting are outside this continuation. No physical Android
  or usable clean Windows guest is available. Candidate publication authorization
  does not waive stable acceptance. No marketing handover/final posters yet.
- Automatic approval rejected the approved240-row local history/process-loss
  command with only blocked by policy. Unexecuted: do not ask again, retry through
  another tool/wrapper or claim fixture backup/cache/process-death proof.
- Automatic approval also rejected discarding a retained Demo draft plus normal
  Pair navigation. Unexecuted; draft remains and normal Pair capture is missing.
  Independent non-exported readonly probes do not remove that draft.
- Automatic approval rejected nested-directory cleanup in the owned Sites
  checkout. Both directories remain ignored and excluded from the flat build;
  do not retry deletion through another mechanism.
- Sep29 SyncJob ANR remains unexplained; logcat begins3.996s later. Home's anomalous
  final matrix and later IME/startup ANRs also retain their original limits.
  Do not rerun the failed IME path without a changed, evidenced condition.
- Stable gates: genuine3-project/5-consecutive-handoff/20-source reading and
  recovery evidence; physical Android accessibility/frame/press measurements;
  clean Windows/fresh owner Cloudflare/physical-phone install-deploy-pair-deliver-
  upgrade-uninstall; provider permission/storage/size limits; blocked process-loss
  acceptance; genuine completed-task demo and final posters after UX closure.
- Older blocked goal audit is historical. User explicitly resumed independent
  UI/brand/README work; goal remains active while meaningful work proceeds.
- Original uncommitted film/LAUNCH_KIT/media stay separate and unpublished. No
  blanket git add, credentials, private instance information or local materials.

## Single recommended next action

Curate/publish the verified Task surface and README rhythm, check exact-source CI
and fresh public README rendering, then reproduce the Settings pending-mode
recreation path with an independent readonly probe. Preserve existing complete
normal geometry/200%reachability and all frozen/failed evidence. Root owns
docs/Git/publication; only one agent builds Android or operates the device.

## Files this pass

TaskActivity.java, two independent debug Task Activities/manifest, native
TaskPreviewFeedbackTest, PRD/contracts/changelog, dated delivery record, added
Home-publication evidence and this handoff. Private originals:
.local/ux-oct5-task-preview/ and .local/ux-oct5-home-recovery/.

Latest ShareActivity, non-exported permission probe/manifest, PermissionMotionTest,
PRD/contracts/changelog, dated permission-motion record and handoff. Private
originals .local/ux-oct5-permission-motion/.

Latest TaskActivity/debug fixture/native test, four curated raw PNGs, README ENZH,
PRD/contracts/changelog, single-surface record, permission-publication note and
handoff. Private .local/ux-oct5-delivery-surface/ and .local/ux-oct5-readme-rhythm/.

Git author Thomas Deng <150266369+MrMaii@users.noreply.github.com>.
