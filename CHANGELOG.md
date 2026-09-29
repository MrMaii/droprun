# Changelog

## 0.5.1 — unreleased Android UX candidate

- Refined project cards, grouped settings and result-first reports.
- Shared press feedback, internal page transitions and animated disclosures;
  system-disabled animation remains supported.
- Readable project/computer names at large font sizes, saved-item inspection and
  retry, and follow-up draft/identity restoration on Activity recreation.
- Earlier local-save feedback and a shorter share success animation.
- Confirm before discarding an unsent note. Local instrumented recovery checks
  cover drafts, interrupted uploads, lost acknowledgements and instance isolation.
- Rounded confirmation surfaces and readable sentence-case actions in both themes.
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
