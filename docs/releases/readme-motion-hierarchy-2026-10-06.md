# README motion and reading hierarchy

Date: 2026-10-06 UTC. English and Chinese source refinements, outside the unchanged
0.5.5 packages. Actual immutable GitHub rendering is verified separately below.

The existing native UI GIF previously followed three tall workflow screenshots
and the full installation explanation. It now follows the value explanation,
before the workflow gallery. The primary installation action and candidate notice
remain at the top. The earlier-tour disclosure retains manual video links; its
second looping GIF no longer interrupts reading when expanded.

The native tour uses `picture` sources for reduced motion: existing light or dark
share screenshots according to the browser's color preference. Its default image
remains the same GIF. Explicit links to both stills remain available. No JavaScript
or new imagery was added to GitHub Markdown.

All six referenced GIF/PNG hashes remain unchanged, matching the
[media provenance record](../../assets/brand/readme-media.md). Tours are October 5,
2026, 0.5.1 development UI with labelled Demo data; the third workflow image is an
October 6 memory-only native delivery probe. No genuine Codex execution or recording
of the signed download is claimed. The newer Home source is not represented by
the older Home screenshot or tour.

Source order, local references and unchanged media are checked separately from
GitHub's sanitizer and actual picture/media-query selection. Source markup alone does not certify the static fallback; the actual renderer
checks below supply that evidence.

## Actual GitHub rendering

Source7e405f9c7e5566483e1e0d7d3dd063b90631a237, fresh anonymous Edge, CSS
390×844 and unedited raw PNG390×844. Both English and Chinese retain the
picture sources and media queries. Normal motion selects the original480×960
GIF; reduced motion selects the original720×1440 light/dark PNG. Actual
currentSrc matches the selected source. All images finish loading, Demo and
candidate notices remain, tour heading precedes workflow, and client/scroll
width390 has no horizontal overflow. No account login or download action.

Six accepted captures come from two runs: English3 in the second attempt and
Chinese3 in the third. The first helper called a nonexistent convenience method;
the second used a fixed800ms wait and failed when the Chinese GIF was still
loading. The third used a bounded image-readiness check and captured only the
remaining Chinese cases. Original errors and partial captures are retained; this
is not a single six-scene run. Owned browser closure was confirmed before
temporary-profile removal. This does not certify every theme, keyboard flow or
website reduced-motion behavior.
