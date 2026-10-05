# ADR 0016: Project-first, translucent mobile experience

Date: 2026-09-25. Status: accepted by the user. Supersedes ADR 0014's visual
direction and flat home history, preserving its share and approval semantics.

The home screen contains Recent handoffs and a small settings control. Remove the
logo header. Show only projects with retained handoffs or locally pending shares.
Opening a project shows its history; opening a task shows its result, action
required and evidence before the detailed report.

Project summaries use stable project identity and count retained root tasks and
all accepted dispatches (shares plus follow-ups). Network retries do not count.
Pending shares are separate until acknowledged by their existing UUID. Complete
server aggregation and cursor pagination replace counting the first 200 rows.

Light warm-white surfaces, lime actions and readable text replace the pure black
default. Dark/system options remain. Translucency belongs to navigation and
sheets; report text has a stable background. Keep native Java Views and platform
fonts. Respect disabled animations, 48dp hit targets, large text and TalkBack.

Settings contains real connection, execution, model, permission, appearance,
language, notification and data controls. There is no account login in the
self-hosted edition. A share's temporary model selection does not change defaults.

Actual local persistence, server acknowledgement and verified completion are
separate states. Motion communicates state and never fabricates progress.

Design references checked 2026-09-25:
[Apple materials](https://developer.apple.com/design/human-interface-guidelines/materials),
[motion](https://developer.apple.com/design/human-interface-guidelines/motion),
[Android blur support](https://source.android.com/docs/core/display/window-blurs).

## Implementation refinement, 2026-10-05

Keep the original DropRun mark at public entry points and onboarding. Brand
recognition inside the working client comes from consistent platform typography,
warm neutral surfaces, dark text, lime actions and restrained tactile feedback.
It does not require restoring a large logo to the project home.

Project identity, task/dispatch counts and state form separate reading levels.
Connection identity leads settings; result content leads delivery. Ordinary
selection remains immediate. Destructive actions and expanded permissions retain
their explicit confirmation, rather than adding a dialog to every choice.

References rechecked 2026-10-05: the materials and motion references above, and
[Apple typography](https://developer.apple.com/design/human-interface-guidelines/typography).
These inform the native Android design, not an Apple API or certification claim.
Actual captures and the dated scope of verification belong to
[the UX validation record](../releases/0.5.1-ux.md).
