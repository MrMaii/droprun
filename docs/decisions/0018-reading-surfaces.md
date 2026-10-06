# ADR 0018: Give primary content the page's reading width

Accepted 2026-10-06. Extends [ADR 0016](0016-project-first-light-ux.md).

## Context

Native Home, Share and delivery captures repeat filled containers for ordinary
reading. Project names lose width; received material competes with the note;
delivery adds a second inset around the result and its actions. The owner asked
for a calmer, clearer interface and continued improvement while real acceptance
remains unavailable.

## Decision

Keep native Java Views and the existing system font, theme and motion tools.
Home uses continuous clickable project rows, subtle separators and distinct
state emphasis. Share gives material/model controls trailing arrows and complete
row feedback, with the note as the input surface. Confirmed settings read as
context; unknown settings retain a warning. Delivery uses page-width content
over its opaque page background, with a quiet label and a readable summary.

Settings and project history follow this same edge. Reserve the distinct filled
Settings card for the connected computer; ordinary preferences and permission
rows use plain sections. Large text keeps the complete project name and moves
the permission action below it, with long names reachable through scrolling.
History uses one outer separator, preserving stable row identity and feedback.
Disclosure reversal continues the current opacity as well as its current height.
Delivery keeps the ready preview primary, files outlined and follow-up quiet,
using the existing button states and touch target.

Preserve counts, identity, permissions, callbacks, full evidence and confirmation
for consequential actions. Appearance does not change business authorization.
Do not replace genuine captures with invented product screens.

## Consequences and checks

Reduced containers need clear click boundaries: retain arrows, ripple, focus,
48dp targets and complete accessibility descriptions. Long content must remain
reachable through scrolling, without hiding the full report. Native checks cover
both languages/themes and selected 200% windows; physical accessibility,
performance and real handoff acceptance remain open.

Requirements live in [PRD](../product/PRD.md); actual evidence in
[Share verification](../releases/ui-share-reading-surfaces-2026-10-06.md) and
[Home/delivery verification](../releases/ui-home-delivery-reading-2026-10-06.md).
The follow-on [Settings/history verification](../releases/ui-settings-history-reading-2026-10-06.md)
records the actual layout and reversal checks.
