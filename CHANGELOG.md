# Changelog

## 0.5.1 — unreleased UX candidate

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
This candidate has not replaced the public release or completed real handoff acceptance.

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
