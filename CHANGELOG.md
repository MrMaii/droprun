# Changelog

## Unreleased

Preparing runtime 0.5.6/code19; current download remains 0.5.5 until new packages
are built and independently verified.

- Simplify share steps to one primary question, and unify default model/effort
  with existing settings rows and localized effort names. Preserve original IDs,
  selection, cancel and focus. [Native verification](docs/releases/ui-share-settings-hierarchy-2026-10-06.md).
- Track the actual owned browser through Windows compatibility relaunches; bind
  DevTools to its dedicated profile and await confirmed closure. Keep update
  barriers while a media browser remains unconfirmed. [Verification](docs/releases/browser-lifecycle-2026-10-06.md).
- Put the dated native tour earlier in both READMEs, with actual GitHub static
  light/dark fallbacks for reduced motion. [Renderer verification](docs/releases/readme-motion-hierarchy-2026-10-06.md).

- Give project names and counts the full card content width by removing the
  redundant initial tile. Keep states, statistics, access and navigation intact.
  [Local native verification](docs/releases/ui-home-project-hierarchy-2026-10-06.md)
  is separate from the unchanged 0.5.5 download. A separate
  [system-bar observation](docs/releases/ui-system-bars-observation-2026-10-06.md)
  recorded readable glyphs after settlement; no appearance code changed.

## 0.5.5-rc.1 — clearer results and installation

[Verified preview packages and open acceptance](docs/releases/0.5.5-rc.1.md).

- Distinguish execution-setting progress, confirmed success and warnings with
  existing accessible theme colors. Include visible explanations in native option
  names, retaining selected and disabled states.
- Give narrow delivery images a minimum 48dp touch target without cropping.
  Keep image navigation disabled during a decision, including stale callbacks.
- Explain which manual pairing input needs correction. Keep entered values and
  clarify that the link must be blank to use the three connection values.
- Keep snapshot explanations brief. Put the complete, selectable version in a
  separate disclosure that retains its expanded state across page recreation.
- Keep English and Chinese README workflow pictures readable on narrow screens
  with independent step headings, captions and full-width-safe pictures.
- Give README installation its own primary entry and keep utility links compact.
  Show the current result hierarchy in original, labelled native probe captures.
- Put the Windows installer and self-hosting guide beside the website prerequisites,
  so the installation landing point immediately offers a next step.

## 0.5.4-rc.1 — clearer delivery and decisions

- Keep execution-setting progress and its result through page recreation without
  replaying the operation. Show it beside the choices, ignore stale feedback and
  announce success once.
- Separate required computer checks from optional media tools in the bilingual
  Windows setup guide, with a clear later-install option and unchanged actions.
- Keep home sync failures compact, with the original cause in historical details.
  Manual checks show immediate accessible progress and reuse an in-flight home
  check. Explicit checks also work while the live receiver is running.
- Let enlarged project names wrap fully so shared prefixes do not hide their
  distinguishing suffixes.
- Explain local Codex work in both README workflows before project statistics.
- Put platform downloads in both README headers and installation directly after
  the workflow. Keep the native GIF visible and distinguish Unreleased source
  from the current downloadable candidate.
- Make inspecting a delivery the primary result-page action: open a ready
  preview, otherwise view screenshots/files. Follow-up is secondary. Missing
  and unavailable previews explain the next step without implying expiration
  or pointing to an absent files entry.
- Group the result, existing screenshot, preview state and inspection actions
  on one content surface. Keep follow-up and approvals separate, with readable
  layouts in both themes and enlarged text.
- Keep dismissed project-permission feedback from closing a later dialog or
  advancing another selection. The permission request and confirmation remain
  unchanged.

## 0.5.3-rc.1 — native feedback refinement

- Show the saved or unconfirmed execution preference before sharing, with the
  receipt-time boundary and localized effort summaries. Simplify routine saved
  feedback; align Chinese project permission copy with the execution setting.
- Replace README architecture embeds with bilingual theme-aware SVGs and text
  descriptions, keeping self-hosted boundaries readable without a rich renderer.

- Structural keyboard focus preserves page colors; button focus rings and Enter
  activation remain visible and available.
- Background settings updates retain the focused appearance or language control.
- Invalid manual pairing shows a complete form error, retains entries and sends
  no request. Both input methods still require source confirmation.
- README places the existing demonstration label before the tour and aligns the
  English home description with locally saved shares.
- Offline and disabled-notification share feedback stays readable for at least
  four seconds, with immediate Close and the existing accessibility timeout.
  Flight completion refreshes the explanation; ordinary saves still return promptly.
- The Codex label grows with text size instead of wrapping inside a fixed circle.
  Pending execution-setting choices expose their disabled state to keyboard and
  accessibility users, then become available when the request settles.
- README leads with the three-step screenshots, then a smaller native tour;
  project counts and the portable startup entry are explicit in both languages.

Runtime0.5.3/Android code16 is publicly available with the same Android signing
certificate. All eight release assets were downloaded anonymously and verified.
[Package/source evidence and open gates](docs/releases/0.5.3-rc.1.md).
[Local verification](docs/releases/ui-followthrough-2026-10-05.md).

## 0.5.2-rc.1 — native UI and product presentation

Warm neutral surfaces, clearer project identity/count/state hierarchy, contextual
share steps and result-first delivery refine the native Java Views client.
Settings leads with the connected computer; pairing brings scan/manual actions
before setup help. Press, selection, focus and interrupted disclosure feedback
share the same controls and respect disabled animations.

Plan review describes execution configuration rather than a pending decision.
Unknown modes do not imply isolated execution. Discarding an unsent share names
the saved copies and note that will be removed; the saving animation's accessible
label describes phone persistence rather than receipt by Codex.

English/Chinese website and README use actual labelled native screens, with
theme-aware images, a clearer handoff story and explicit download prerequisites.
The published0.5.2-rc.1 packages include this visual pass. Captures remain dated
0.5.1 development UI; labelled Demo data does not establish genuine execution.

Windows setup and phone pairing use the same neutral surfaces, system appearance
and 48px controls. Invalid deployment fields stay local with focused errors;
saved configuration is distinguished from the last verified deployment. Expired
pairing pages hide invitations, and failed clipboard writes offer manual copying.
The existing local confirmation/token and Relay pairing boundaries remain intact.

Source CI and complete anonymous downloads of all eight files passed. Stable
device, installation and genuine-handoff acceptance remain open. Current evidence:
[0.5.2-rc.1](docs/releases/0.5.2-rc.1.md).

## 0.5.1-rc.2 — Windows startup and preview shutdown

Bundled Windows scripts start with a process-only policy. Preview shutdown waits
for owned process closure; failed or unknown termination blocks project edits and
retains handles for retry. Concurrent requests notify once; exited PIDs are not
terminated again. Temporary preview-fixture cleanup uses bounded retries.

Node218/218 and released-source CI passed. Source-bound packages retain Android's
stable signing certificate and disclose the unsigned Windows installer. Real
device, clean installation and genuine-handoff gates remain open. Current evidence:
[0.5.1-rc.2](docs/releases/0.5.1-rc.2.md).

## 0.5.1-rc.1 — public self-hosted preview

- Confirmed deletion clears paged history and blocks stale responses or saved
  UUID retries from recreating a task. Uncertain deletion retains the cached
  report; an inaccessible record offers separately confirmed phone-cache clearing.
- Windows update/uninstall wait for verified worker exit, refuse in-flight
  queue work and legacy shutdown protocols, and keep the supervisor from restarting
  an intentionally stopped Connector. Upgrades use the new bundled verifier.
- Update the pinned Cloudflare tools and affected transitive dependencies to
  their smallest compatible security fixes; validation remains candidate-only.
- File saving shows inline progress/results, prevents duplicate picker/write
  requests, and survives page recreation without repeating a write. Interrupted
  state explains uncertainty; failed saves retain a retryable preview. Leaving
  an active save requires confirmation and defers source cleanup until completion.
- Reopening home preserves an unchanged periodic sync job instead of repeatedly
  replacing it. Missing or changed schedules are registered again.
- Restored delivery previews verify cached size and SHA-256 again before showing
  contents or Save. Export also rechecks bytes; changed/expired caches offer
  recovery instead of retaining a verified-preview claim.
- Page focus scrolling keeps buttons inside system-bar insets; delivery errors
  use a short title that remains readable with large text.
- Setup actions immediately show progress and prevent duplicate clicks; errors
  survive status polling. Lost acknowledgements require a status check, without
  automatically repeating an action. Release checks provide bilingual waiting,
  retry and result feedback.
- Windows setup shows a read-only project inventory with IDs, local folders and
  availability. Complete pagination, bilingual recovery and keyboard access help
  match same-name phone projects; paths stay in the protected local setup page.

- Share catalogs prioritize all recent projects, including local pending work.
  Same-name projects show distinct short identifiers across selection and history;
  unavailable projects explain how to reconnect instead of entering the editor.
- Share search and expanded catalogs survive Activity recreation; search ignores
  surrounding whitespace. Permission dialogs scroll at large font sizes, keep
  keyboard focus inside, and reveal failed authorization messages for retry.
- Refined project cards, grouped settings and result-first reports.
- Shared press feedback, internal page transitions and animated disclosures;
  system-disabled animation remains supported.
- Readable project/computer names at large font sizes, saved-item inspection and
  retry, and follow-up draft/identity restoration on Activity recreation.
- Earlier local-save feedback and a shorter share success animation.
- Confirm before discarding an unsent note. Local instrumented recovery checks
  cover drafts, interrupted uploads, lost acknowledgements and instance isolation.
- Rounded confirmation surfaces and readable sentence-case actions in both themes.
- Scrollable empty-home guidance at large font sizes; keyboard focus and Enter
  activation for project and task cards.
- Share keyboard navigation skips the passive sheet surface and duplicate model icon.
- Cached command and preview actions expire correctly; failed decisions stay visible
  after background refresh. Reading status describes attempted material access.
- Settings show the last synced, instance-scoped Relay retention policy, with an
  explicit unknown state before first retrieval and offline-cache guidance.
- Scrolling pages clip content to system-bar insets, avoiding text over the clock
  and navigation controls.
- Reject foreign saved tasks without adopting their instance identity on failure.
- Fixed-length string bodies in the local Miniflare test transport avoid Windows
  resets on early rejection; production Worker authentication is unchanged.

Local validation and remaining gates: [UX record](docs/releases/0.5.1-ux.md).
Published candidate evidence: [0.5.1-rc.1](docs/releases/0.5.1-rc.1.md).
Stable launch and real handoff acceptance remain open.

## 0.5.0-rc.1 — public self-hosted preview

- Android project-grouped history, retry-safe task/dispatch counts and paginated history.
- Consolidated settings, light/dark/system appearance, English and Chinese.
- Instance-bound pairing with Android Keystore and Windows DPAPI credentials.
- Resumable Cloudflare provisioning, per-user Windows installer and portable ZIP.
- Separate-origin, versioned static snapshots with expiring/revocable access.
- Public source, bilingual website, real app screenshots and captioned UI tours.

This is a release candidate. Physical-device, genuine-source and independent
clean-environment gates remain in [release evidence](docs/releases/0.5.0.md).
It does not upgrade the old privately signed Android package in place.
