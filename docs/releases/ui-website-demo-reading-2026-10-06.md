# Website demo reading — October 6, 2026

The Share recording is now large enough to inspect its controls. Its maximum
display width increases from 220px to its native 320px; narrow viewports constrain
it to the available column. At 780px and below, copy and recording stack into
one column. The separate 190px and 218px overrides are removed.

This trades a taller demo section for readable product detail. The original
recording, poster, captions, download links and development-UI provenance remain.
The video is still user initiated; this change adds no autoplay.

The hero, Share workflow and delivery examples now select the eight matte
English/Chinese, light/dark [raw native stills](ui-primary-surface-2026-10-06.md).
Their website copies are byte-exact. Generation changed only eight static image
attributes per page; links, visible copy, Home images and the complete dated
video section remain identical. The recording and its poster retain their own
earlier source. These substitutions are not a new thirteen-scene render.

## Actual local verification

Generated English and Chinese pages were inspected at 320, 375, 768, 781, 820
and 1280px in light appearance with reduced motion. An additional Chinese 768px
dark scene makes 13 actual scenes. All had the requested viewport, no horizontal
overflow, a 1:2 video rectangle and contained caption. Computed layout was one
column through 780px and two columns above it. All 13 owned browser endpoints
and pages closed; no external request occurred.

| Viewport | Actual video width | Columns |
| --- | --- | --- |
| 320px | 228px | 1 |
| 375px | 283px | 1 |
| 768px | 320px | 1 |
| 781px | 260px | 2 |
| 820px | 277.34px | 2 |
| 1280px | 320px | 2 |

The existing workflow link accepted keyboard focus with its 3px visible outline.
The native video element's focus was observed without activating controls.
Four raw demo-region captures were directly reviewed. The Chinese 768px capture
includes a sticky header and skip-link overlay obscuring part of the heading;
it is retained as evidence, not used as an unobstructed brand image. Some other
captures also retain page chrome at their upper edge.

Exact CSS SHA-256:
`f58fe457cf6b2390c774b2793af8471a77f321ba1304910bd616986f80987331`.
Actual report SHA-256:
`92f204e7cf4ad9d732796987ef681aa8d7028444f67b3bd6ab813d0f75f890e3`.
Read-only review SHA-256:
`4016669d40987e970867d128f85668409233d533a8c43769fc9d20432a095fec`.

## Scope and limits

This was an offline local layout check. Exact CSS was inlined and bound PNGs
embedded; page JavaScript was removed. The one known MP4 source and VTT track
were omitted locally while the original poster, controls and caption remained.
It does not establish hosted JavaScript, playback, subtitles, contrast ratios,
full-motion behavior or a full dark-mode matrix. It does not establish complete
page aesthetic acceptance or genuine task delivery.

The first private preview exited before creating output or launching a browser:
its resource guard incorrectly matched the `src` suffix in `data-local-src`.
The corrected guard validates complete attribute names and exact bound PNG
values. The failed original remains; it is not counted as 13 failed scenes.

## Website publication

The reviewed changes are now public on [GPTSites](https://droprun.dengmaizi0802.chatgpt.site/)
and its [Chinese page](https://droprun.dengmaizi0802.chatgpt.site/zh/).
Native version 16 deployed successfully at 2026-10-06T16:20:39.712014+00:00.
Product source is `28e5f45baed4ab2cb4748b115da50f6476224454`; pushed hosting
source is `ce5f0e94835aa004a89d037fb0a07fa2b8ebd0ab`. Public audience stayed
unchanged. The source copy has 87 public files: eight added PNGs and eleven
changed files. Existing media and unknown checkout paths were preserved.

The actual archive contains those 87 byte-checked files plus its hosting
manifest, with no private path or link member. Local gzip is 12,100,464 bytes,
SHA-256 `6b44eaef4e674e570ebb546647a5ce85bcd927a2df0e8602134a03474f7681c3`.
Native storage converted it to an 88-file, 13,137,920-byte tar, separately hashed
`dc0e63e22b9b70802c49ea3d4d11b61c81cf256867afb7deab528a00aa2018c2`.
The first workflow failed after the source push because PATH resolved the WSL
bash shim. The unchanged workflow subsequently packaged successfully with
process-local Git Bash PATH and TAR_OPTIONS. An initial read-only validator
expected the wrong root layout; corrected validation checks the actual `dist/`
layout and unchanged manifest. Originals remain; neither failure is hidden.

Native publication and archive checks do not establish hosted JavaScript,
playback or new screenshot rendering. Public candidate downloads remain
[0.5.10-rc.1](0.5.10-rc.1.md); the signed package was not rebuilt in this pass.
