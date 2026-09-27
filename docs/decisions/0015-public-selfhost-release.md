# ADR 0015: Public self-hosted distribution

Date: 2026-09-25. Status: accepted by the user; implementation evidence belongs in
the release record, not this decision.

DropRun's first public distribution supports Android and Windows x64. Each owner
deploys their own single-owner, single-Connector Cloudflare Relay. Multiple phones
may pair with that instance. No official registration, billing, shared tenant
database or hosted execution is added. Cloudflare and agent usage belongs to the
owner; open source is not a promise of unlimited free infrastructure.

The public website uses GPTSites, and source/installers use MrMaii/droprun on
GitHub. Source is Apache-2.0 with separate third-party notices. English is the
primary public language with full Chinese support. Existing brand assets remain.

Installation uses a Windows per-user installer or portable archive, bundled Node,
a local browser setup guide, resumable Cloudflare deployment, then Android QR
pairing. Setup credentials are separate from runtime credentials. Android public
distribution uses its own stable signing identity and package, preserving the
old private application instead of attempting an incompatible signature upgrade.

Screenshots, reports, files and supported static snapshots are base delivery.
Live preview is optional and needs the owner's managed tunnel/domain and an
online computer. Quick Tunnels are development-only. Missing preview capability
must be disclosed and never reported as a verified preview.

Release candidates may be public with explicit gaps. Stable status requires the
acceptance gates in the PRD and release record, including real-device validation.
Social posts are prepared; publishing them requires a separate instruction.
