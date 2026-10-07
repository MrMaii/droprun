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
or elevation. Refined2026-10-07: the complete20sp name leads without a decorative
initial;13sp counts follow, while state and recency share the lower reading line.
The card keeps16dp padding and12dp separation. Complete names wrap at every text
size. IDs, counts, permissions, navigation, polling and feedback retain their
existing meaning.

## Alternatives and consequences

A plain section preserves more width but leaves the new availability/recovery
controls visually detached from their dependent preferences. A negative margin
could preserve that width inside a filled group but would require changing
parent clipping. Normal bounds keep the change small and predictable.

At 320dp, model rows now have 248dp instead of 280dp. Complete values and controls
must remain readable and reachable at 200% text; content can grow vertically.
This is a functional grouping change, not an authorization or network change.
Genuine handoff, physical accessibility and performance gates stay open.

The original2026-10-06 initial reduced Home's normal name width264→184dp; large
text hid it and used232dp. The2026-10-07 revision removes its48dp allocation at
every size, giving normal names232dp too. Two English names shared S and two
Chinese names shared 工: the repeated decoration did not distinguish those
projects. The20sp name restores emphasis while the matte surface still separates
identity and state. This loses the repeated initial motif between Home and Share;
Share's destination-confirmation tile remains. Larger headings may change wrapping
and height. Do not trade complete values, keyboard focus or reading position for
the new surfaces. [Fresh reading and rebind checks](../releases/ui-home-typography-2026-10-07.md)
cover six fixed local windows; both reviewers accept the four normal-font originals.
Prior initial-based captures do not cover this revised layout. Long-name and200%
reading are assertion checks, without new pixel, physical or genuine-use acceptance.

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

### Project-identity extension — accepted 2026-10-07

Inside the access surface, the distinguishing ID previously read as another
part of the long project name. Keep the existing TextView and complete15sp
medium name; put only the helper-appended ID on a separate12sp muted line.
Unique names get no hint. Use the exact known name length, preserving names
that contain a middle-dot and prefixes that must extend beyond eight characters.
The original full accessibility label, permission description, confirmation,
callbacks,48dp target, large-text stacking and focus rules remain.

A new chip or nested card would add another visual boundary without a new
function. Two text levels clarify identity within the existing surface.
[Four fixed memory windows and two normal originals](../releases/ui-settings-project-identity-2026-10-07.md)
own the actual local scope; this adds no real permission or physical acceptance.

### Share project-read extension — accepted 2026-10-07

Six cached destinations put the existing read state and Retry below the initial
viewport. Loading and failure therefore looked identical, despite useful cached
choices. Move these same controls before the project rows, within their existing
surface. At normal type, status takes the remaining width beside its48dp action;
at150% and above they stack so complete text can wrap. Keep the surface, values,
polite live region, disabled state, callbacks and row identity.

This exchanges one initially visible project row for immediately discoverable
recovery. A separate dialog would interrupt a usable cached catalog; a floating
action would separate Retry from its explanation. The small layout change keeps
the result and next action together without extra navigation or decoration.

[Two fresh fixed-six-project takes](../releases/ui-share-read-feedback-2026-10-07.md)
retain four unchanged baseline and four successor originals. Both reviewers
accept the successor's EN/light and ZH/dark font1 loading/failure feedback.
Geometry separately requires the complete status and48dp action in each fixed
initial viewport. Large-type stacking is source behavior, not new200% pixel or
physical acceptance. No click, Send, genuine cache or signed-package claim.
