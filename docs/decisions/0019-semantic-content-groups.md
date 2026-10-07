# ADR 0019: Use surfaces to make functional groups recognizable

Accepted 2026-10-06. Revises the Home and model-preference portions of
[ADR 0018](0018-reading-surfaces.md); its reading, motion and permission rules
continue to apply.

## Context

The owner found the current client too basic and asked for continued UI and
brand refinement while genuine acceptance is unavailable. The plain Settings
sections put model availability, a recovery action and two preferences on the
same background as unrelated project permissions. Their relationship is harder
to scan than the text hierarchy alone suggests. Home's bare text rows also give
projects little visual identity, despite each having its own history and state.

## Decision

Give the default model group one opaque, theme-aware surface: availability and
Refresh/Retry, model, effort and their explanation belong together. Use the
existing 26dp card radius with 16dp horizontal and 12dp vertical padding, without
a border or elevation. Keep the system font, current values, target sizes,
keyboard feedback and existing read controller. Other Settings sections retain
their current layout for this pass.

This adapts Apple's separation of stable content surfaces and navigation
materials to native Android; it does not copy an Apple widget or promise an
Apple-equivalent experience. [Apple material guidance](https://developer.apple.com/design/human-interface-guidelines/materials)
was checked on 2026-10-06. Blur is unnecessary for this content group. DropRun's
lime accent continues to identify actions and selected states rather than every
container.

Home gives each project one matte surface using the same radius and no border
or elevation. A decorative initial and a smaller18sp name distinguish the
identity block; counts follow the name, while state and recency share the lower
reading line. The card has16dp padding and12dp separation. At150% text and above,
hide the initial and its gap to preserve width for the complete name. Recycled
cards update that initial from the current displayed label. IDs, counts,
permissions, navigation, polling and feedback retain their existing meaning.

## Alternatives and consequences

A plain section preserves more width but leaves the new availability/recovery
controls visually detached from their dependent preferences. A negative margin
could preserve that width inside a filled group but would require changing
parent clipping. Normal bounds keep the change small and predictable.

At 320dp, model rows now have 248dp instead of 280dp. Complete values and controls
must remain readable and reachable at 200% text; content can grow vertically.
This is a functional grouping change, not an authorization or network change.
Genuine handoff, physical accessibility and performance gates stay open.

Home's normal-font name width changes264→184dp; the18sp heading lets the sampled
names fit. Large text uses232dp and full wrapping. Continuous rows offered more
width but weaker separation of project identity and task state. Do not trade
complete values, keyboard focus or reading position for the new surfaces.

## Verification

### Project-access extension — accepted 2026-10-07

The model group exposed a similar boundary problem below it: project permissions
remained on the bare page. Extend the same flat surface to the entire existing
access disclosure, with its section label outside. Individual rows stay plain.
Names and distinguishing ID prefixes wrap without a two-line cap. The existing large-text
stacked action, callbacks, confirmation and pending presentation remain.

At 320dp this changes access inner width280→248dp. Normal text shares that width
with a48dp-minimum permission control and10dp gap; at150% and above the name uses
the whole248dp and the control follows. Extra height is preferable to truncating
identity. A bare page preserved32dp but made unrelated settings harder to scan.
This extension revises only the previous pass's access-section layout; no glass,
business action or permission policy is added.

[Six fixed memory windows and four original partial captures](../releases/ui-settings-access-group-2026-10-07.md)
own its source/APK binding and actual limits. Both reviewers directly accept the
normal-font excerpts; English lower corners continue beyond the viewport. This
does not establish full-page visibility, permission focus restoration, TalkBack,
motion or genuine use.

[Actual native checks and unchanged captures](../releases/ui-settings-model-group-2026-10-06.md)
own the frozen source, both language/theme samples, large-text reading and
Refresh recovery results. New visuals use original native pixels, including
their memory-only markers; no invented product screens replace them.
[Home's actual reading and rebind checks](../releases/ui-home-project-cards-2026-10-06.md)
own its separate source/APK binding. Those tests are not genuine polling or
cross-instance acceptance.

### Delivery-artifact extension — accepted 2026-10-07

Report-present delivery pages give the existing preview explanation and file
inspection actions one opaque theme surface: radius26, padding16h/12v, no stroke
or elevation. Remove only the first preview heading's duplicate top inset. The
result summary and screenshot thumbnail retain the page's full reading width;
follow-up, approval, full report and deletion remain outside. Report-empty states
and all copy, eligibility, confirmation, callbacks and motion are unchanged.

This extends functional grouping without restoring an inset around the result.
At320dp, artifact controls change280→248dp; complete labels may grow vertically.
The lime primary still means ready preview, or files when none is ready. No blur,
new navigation, business authorization or decorative identity is introduced.

[Two font2 reading windows and four normal-font native originals](../releases/ui-task-artifact-group-2026-10-07.md)
pass the bounded local checks; root and an independent reviewer view all four
originals and accept this grouping. Follow-up sits near the normal viewport's
bottom and can require scrolling; its complete normal-font touch area is not
established by the stills. No genuine, physical, signed-package or motion claim.

### Follow-up extension — accepted 2026-10-07

The delivery's ghost-style follow-up looked like text and its complete target
extended below the short normal sample's safe area. Reuse the existing outlined
secondary action rather than introducing another primary action. Reduce only
report-present outer margins18top/10bottom to12top/0bottom; keep the8dp gap after
the artifact group,22sp result and52dp action. Long results and large text scroll.
The same-thread dialog, eligibility, draft, confirmation and locks remain.

[Fresh source-bound reading and four normal originals](../releases/ui-task-followup-fit-2026-10-07.md)
establish whole initial targets only for the fixed short EN/ZH sample, with15px/4px
bottom clearance. Root and an independent reviewer accept all four normal stills;
the original overflow baseline remains unchanged. This does not claim arbitrary
first-screen fit, actual press/dialog/IME, new motion, physical or signed-package
acceptance. The outlined action's existing interaction states are reused.
