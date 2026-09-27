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
