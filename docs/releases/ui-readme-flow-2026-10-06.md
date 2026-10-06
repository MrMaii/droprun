# Readable README workflow — October 6, 2026

The public README now presents each step with its own heading, explanation and
centered picture. This changes only the English and Chinese presentation; the
image bytes, source labels, theme alternatives and download destinations remain.
It trades a compact desktop row for readable pictures on a phone. Installation
stays in the header and before the GIF.

## Original problem and scoped change

At source `270b002b694164fbab2ebe0e8de14d4960b9cb82`, actual GitHub rendering at
390px compressed the three-column table's images to 65.44–91.55px. The document
did not overflow, but screenshot text and the memory-only marker were too small
to read. Desktop delivery pictures were 220px wide. This failed usability check
and its raw captures are retained.

Source `6f577f6bdbd006d3413b51133043a2d2742195c4` replaces that table with ordinary
Markdown step headings and independent `<picture>` blocks at width 280. It does
not depend on custom CSS or JavaScript in GitHub Markdown.

## Actual public rendering

| Scene | Viewport | Document client / scroll width | Three workflow pictures |
| --- | --- | --- | --- |
| English desktop | 1280 × 720 | 1265 / 1265 | Each 280 × 560 |
| English narrow | 390 × 844 | 375 / 375 | Each 280 × 560 |
| Chinese desktop | 1280 × 720 | 1265 / 1265 | Each 280 × 560 |
| Chinese narrow | 390 × 844 | 375 / 375 | Each 280 × 560 |

All twelve images loaded. Each heading and original explanation precedes its
picture; both narrow delivery markers were visibly readable. No document
horizontal overflow was measured. The install CTA and 0.5.4 candidate notice
remain, with installation before the loaded main GIF in all four scenes.

The third step selects the light native delivery probe, naturally 320 × 640.
The first two pictures and GIF remain dated development demos. Their unchanged
[capture notes](../../assets/brand/readme-media.md) explain the source and scope.
These are not signed-package recordings or genuine completed work.

Actual browser coverage is an existing signed-in Edge profile, GitHub auto mode
with light preference. No sign-in or theme action was taken. Anonymous and dark
UI rendering were not tested. Two separate new anonymous raw-file GETs matched
the exact Git blobs; this establishes public bytes, not anonymous visual layout.

Fifteen original JPEGs were preserved. One Chinese desktop capture showed a
blank lower bitmap area despite complete DOM dimensions; one bounded additional
capture showed the complete frame. The original was not replaced. Sticky GitHub
navigation can cover the desktop heading while its picture is visible; DOM/AX
and narrow captures establish the association. The temporary tab was closed and
viewport restored. Existing user tabs and the previous nine evidence files were
unchanged. Private originals are under `.local/ux-readme-flow-oct6/`.

## Source verification

[CI 37391622412](https://github.com/MrMaii/droprun/actions/runs/37391622412),
attempt 1, passed all three jobs and 35 steps. Actual core tests 210 and browser
tests 14 passed, with no failures, skips, cancellations or todos. Android
build/JVM/lint, audit, generated landing pages, public-source checks and Windows
packaging passed. No CI rerun was used.

This closes the specific narrow README compression issue. It does not establish
physical Android, real handoff, clean installation, performance or stable-launch
acceptance. Public 0.5.4 release artifacts and the website remain unchanged.
