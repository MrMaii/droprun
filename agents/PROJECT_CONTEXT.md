# DropRun Project Context

Updated 2026-09-25. Current work and evidence: [CURRENT](handoffs/CURRENT.md).

DropRun lets a user share phone material to an existing local Codex project and
receive inspectable results. The first public edition is Android + Windows,
self-hosted by each owner on Cloudflare. No official accounts, shared hosted
backend, billing or cloud execution. Source is Apache-2.0; public language is
English with complete Chinese support. Decisions:
[ADR 0015](../docs/decisions/0015-public-selfhost-release.md),
[ADR 0016](../docs/decisions/0016-project-first-light-ux.md).

## Stable rules

- Independent share = new task/conversation; follow-up = same conversation.
- A user's note is the main intent; without a note the agent may judge useful
  applications or decide not to modify anything.
- Codex runs locally in the original project directory. Keep existing changes.
- Project permission, plan approval and high-risk individual actions are distinct.
  New paired devices default to direct execution; the user can choose plan review.
  A preference change cannot approve an already waiting plan.
- External material is untrusted. Report actual text/audio/frame coverage, never
  turn a received URL into a claim that the video was fully understood.
- Completion requires verified output. Static snapshots are tied to versions;
  live preview is optional, uses an owner's managed tunnel and needs an online PC.
- Offline accepted tasks queue. Android background limits remain explicit.
- Home shows only used/pending projects; the full project catalog belongs in
  share selection and settings. Counts exclude transport retries.
- Public preview does not imply stable release. Real-device and real-source
  acceptance gates remain visible when unavailable.

## Ownership

[PRD](../docs/product/PRD.md) owns requirements;
[CONTRACTS](../docs/technical/CONTRACTS.md) owns protocols;
[release record](../docs/releases/0.5.6-rc.1.md) owns current candidate evidence;
[self-hosting guide](../docs/technical/SELF_HOSTING.md) owns installation.
Do not publish local materials, credentials, signing keys or historical chats.
Social campaign preparation does not authorize posting.
