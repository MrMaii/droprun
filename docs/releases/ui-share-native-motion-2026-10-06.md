# Native Share motion — October 6, 2026

The current share sheet can be inspected in a short, continuous native recording:
open received material, close it, open model options, choose another sample model,
scroll to its default effort, and return to the compact editor.

These are **memory-only development UI samples**. The project is preset and the
optional note is empty.
No effort option is clicked, no handoff is sent, and no real Codex work occurs.
The visible sample marker, system bars and emulator underlay remain. Both recordings
use English/Chinese, light appearance, normal Activity-local text size, Android
API35 and a320×640 native canvas. They are not signed-release recordings.

## Original videos

| Language | Actual encoded duration | Original frames | SHA-256 |
| --- | --- | --- | --- |
| [English](../../landing/media/native-share-en-20261006.mp4) | 8.528933s | 127 | `ed08cb13f48452d62fa3d430532afd6c5360a21f24ce15d1c69b4c6860c9c313` |
| [简体中文](../../landing/media/native-share-zh-20261006.mp4) | 9.104911s | 118 | `c07f4b0b7841b6b624874623084455b5ab5635570dc22a7916855249954954e2` |

MP4s are copied byte-for-byte, without cuts, reordered actions, resizing,
interpolation, intentional speed changes or added tail holds. Each has one H.264
video stream, two Android metadata streams and no audio. First-frame posters are
unmodified native decoded PNGs. Captions describe only the visible sample flow.

## Native verification

The nonexported Share fixture requires explicit opt-ins. Its guarded memory Store
rejects network, save, pairing, submission, service and checkpoint persistence.
Only material/model disclosure clicks and the sample model choice are allowed.
The high/Thorough effort is that model's default, rather than a separate click.

Each language has one accepted native test and one recorder: English1/1 in19.275s,
Chinese1/1 in19.335s. Native and recorder processes exited0, the recorder's DONE
handshake completed, Activity destruction was confirmed, and fixture context was
restored. All seven forbidden-action counters remain0. All four device-to-host
transfers per take match. Device settings and crash-log bytes are unchanged.

Capture source consists of110 files; the preceding109 files and four Gradle
configurations stayed byte-identical. Aggregate source SHA-256:
`189f3d72e321ae12392a74ee473f4836f0058af8017983f2d1d71711c469a12b`.
The unchanged development APK is
`4cc4ebe3e95a338d84651e3ee445e9e8f62d09f05c470160a1ada57840d1763a`;
capture-only test APK is
`407f4fbc40b6d35a51a6c4c2c48073ed11ee06cee11d6be297d3eea8598e3255`.

An earlier English host attempt failed while parsing ADB's exact missing-file
message; it never started a recorder. Its native timeout and diagnostic are
retained. The four-line parser correction passed13 offline cases before a fresh
UUID take. The failed attempt is not relabelled as a success.

## Media review and limits

Source video timestamps are strictly increasing in both languages. Complete
decoding passes. English27 and Chinese20 independently extracted keyframes were
directly inspected, including real expansion, fading, selection and scroll
intermediates. Complete sample URLs, selected model/default effort, visible
markers and system bars are readable. No keyboard appears in inspected frames.

The Chinese owner's PNG-output decode logged equal/non-monotonic output DTS;
those original warnings remain. Its original source PTS/DTS are strictly increasing,
and the independent decode preserving the demux timebase exits0 with no error.
Output-image timing is not used as the video's timing or evidence of corruption.

The14-second recorder limit did not encode14-second movies or the planned long
tail hold. Nominal60fps metadata does not establish stable60fps. Variable source
frame gaps do not establish a measured UI freeze. Continuous playback, all-frame
visual review, real touch/press feedback, TalkBack, IME, physical-device performance,
genuine extraction/submission and release acceptance remain unverified here.

Current public download and its separate acceptance gates:
[0.5.7-rc.1](0.5.7-rc.1.md). Media provenance is maintained in
[native README media](../../assets/brand/readme-media.md).
