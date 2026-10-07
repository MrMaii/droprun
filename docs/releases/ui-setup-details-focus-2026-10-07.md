# Setup: keep keyboard focus inside Details

Selected local record, 2026-10-07. Development after signed0.5.15-rc.1.

The Details summary now has a rounded inset focus outline. One CSS rule keeps
the keyboard indicator inside its target, so it no longer crosses the phase
heading. The action, status, disclosure and polling behavior stay unchanged.

A fresh finite browser run returns Node/Chrome0 with nine fixture assertions.
Two full-viewport originals cover English light at320px and Chinese dark with
CSS200% and reduced motion. Each shows the initiating action, active phase and
open, keyboard-focused Details. There is no horizontal overflow. Root and a
separate reviewer directly inspect both originals; automated pixel acceptance
remains false.

A separate file-only reader verifies ten source bindings, the returned process
records, both PNGs and the recorded geometry. Summary targets are52 CSS pixels.
The Range-to-outline gap is0 in both samples: this correction contains the ring;
it does not add spacing or measure painted glyphs.
[Selected byte-bound receipts](ui-setup-details-focus-2026-10-07.json).

Original attempts remain separate: V1 fails at fixture readiness without a
capture; V2 records three narrow English states, then times out preparing its
second configuration; V3 closes the remaining dark fixture with three originals.
Those images exposed the focus overlap. V4 covers only the corrected R2/open
focus state; it does not rerun the older long-error or D1 screenshots.

All status and deployment actions use a local synthetic server. No owner
Cloudflare change, real installation, physical Windows scaling, screen reader,
performance, signed-package or genuine delivery acceptance is claimed.
