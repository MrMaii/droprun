# Current Handoff

Updated 2026-10-05 UTC. **v0.5.1-rc.2 is publicly available.**
Stable launch and marketing handover are not achieved. Preserve the complete
Android+Windows self-hosted goal and the user's local-only validation boundary.

## Latest session: candidate startup and preview shutdown repair

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
- This resumed run made substantial independent progress. Do not carry the earlier
  run's blocked-turn count over. After three consecutive resumed goal turns with
  the same external blocker and no useful independent work left, mark blocked.
- The original SyncJob ANR at 2026-09-29 18:10:24.485 UTC remains unexplained; retained
  logcat begins 3.996s later. The scheduler change is not proof of resolution.
- Mandatory open gates: genuine 3-project/5-consecutive-handoff/20-source reading
  evidence and recovery; physical Android accessibility/frame/press measurements;
  clean Windows/fresh owner Cloudflare/physical-phone install-deploy-pair-deliver-
  upgrade-uninstall; provider-permission loss/storage/size limits; blocked local
  process-loss probe; genuine completed-task demo and final posters after UX closure.
  See [UX evidence](../../docs/releases/0.5.1-ux.md) for the dated UI verification.
- Existing uncommitted promo-film/launch-kit changes remain separate and unpublished.
  No final poster generation or marketing handover claim. No local build/emulator
  remains running.

## Single recommended next action

Resume genuine acceptance when the device, isolated owner environment and explicit
real-submission scope become available. Public candidate delivery is complete;
stable launch and marketing handover are not. Do not replace the gates with more
synthetic passes or another preview release.

## Files this pass

Windows installer/launch/service/setup/media tools and focused tests; preview
lifecycle and native cleanup tests; contracts, PRD, self-hosting guides, changelog,
readmes, roadmap/context, current rc.2 release record, anonymous audit and handoff.
Git author Thomas Deng <150266369+MrMaii@users.noreply.github.com>.
