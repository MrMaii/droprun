# README architecture — October 6, 2026

The English and Chinese README now use a compact, vertical architecture diagram
in light and dark themes. Android and the owner's Cloudflare Relay are separate
from the Windows boundary, which contains Connector and local Codex. The diagram
identifies who deploys each part and how reports and files return to the phone.

The previous 1000×320 composition reduced supporting text to 6.24px at a 390px
image width. The new 400×840 composition has 20–26px source text: supporting text
is 19.5px at 390px and 16px at 320px. The README limits desktop width to 400px,
centers the figure and retains its descriptive alt text and plaintext data flow.
Its introduction also makes deployment to one's own Cloudflare account explicit;
the share caption now describes the note step shown in the native image.

## Verification

- Four actual SVGs passed native text-bound checks with no overlap or overflow.
- Twelve local Windows Edge screenshots covered both languages and themes at
  400, 390 and 320px widths. Each complete diagram fitted the 844px viewport;
  there was no horizontal scrolling. Card text retained at least 19px left,
  27.06px right, 8px top and 12px bottom padding in source coordinates.
- All 44 observed requests were loopback GETs. The renderer used a fresh owned
  browser profile; page, browser, process, temporary profile and server closure
  were confirmed. Original SVGs, native PNGs and diagnostic records are retained.
- No script, external image or web font is embedded in the SVGs. Existing App
  captures and GIFs were preserved. Root additionally inspected the English light
  and Chinese dark 320px originals.

This is an authored engineering illustration, not an App screenshot. Verification
is limited to local Windows Edge; it does not establish GitHub/Sites rendering or
font rendering on Android, macOS or iOS. Previously stopped public-browser probes
were not replayed. Native workflow, signed-package and real-device acceptance
remain separate in the [candidate record](0.5.7-rc.1.md).
