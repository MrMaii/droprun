# Native Share project and effort tour · 2026-10-07

These English and Chinese recordings show a labelled, memory-only Android development sample: select the first project, return to the list, select a different second project, expand model options, select another sample model, change its effort from Thorough (high) to Balanced (medium), and collapse the options. The note stays empty; no task is sent. The source launcher, system bars and memory marker remain in the full 320 × 640 viewport.

The clips come from debug 0.5.15-dev / code28, captured app SHA-256 `0872cbfc5263c10692a25470c12acd6df5552750b7168eb266f05c038ecf302a`, and test APK `c433802af528df523429b83c4c1bd113aa7f0255d688ac2a3c6a57cb833c6f35`. They show that frozen build, not later UI changes, the signed download, or a genuine Codex handoff. Each language used one closed native window; both recordings completed with restored in-process UI settings and zero guarded business calls. The model header received one local DOWN/UP pair; other choices used programmatic clicks.

The README GIFs sample the whole original timeline at 25fps, with 4-centisecond frame delays and standard palette/difference encoding. No crop, resize, interpolation, action trim, overlay or extra ending was added. Original MP4s preserve all source frames. The last sampled state matches the original ending; normal GIF time-grid rounding is listed below. Captions follow inspected encoded-frame PTS, not the instrumentation clock. Existing reduced-motion stills retain their separate provenance.

Visual review covered chronological original-frame sheets and ten composited GIF keyframes: the different destination, high-to-medium change, final retained choice, marker and system bars are visible. This is phase review, not continuous human playback, physical-device performance, all-button testing or a complete release-animation curve. The first English offline reader's container-stream assumption failed and its output was retained; its successful probe was reused for the later successful decode, without re-recording.

| Language | Original MP4 | Sampled GIF | Rounding | Native result |
| --- | --- | --- | --- | --- |
| English | 100 frames · 9.535800 s | 238 frames · 9.52 s · 1,282,748 B | −15.800 ms | 1 test · 22.223 s |
| 中文 | 104 frames · 10.339411 s | 258 frames · 10.32 s · 1,374,769 B | −19.411 ms | 1 test · 22.004 s |

English: [GIF](../../assets/brand/native-share-touch-en-20261007.gif) · [Original](../../landing/media/native-share-touch-en-20261007.mp4) · [Captions](../../landing/media/native-share-touch-en-20261007.vtt) · [Original first frame](../../landing/media/native-share-touch-en-20261007.png).

中文：[GIF](../../assets/brand/native-share-touch-zh-20261007.gif) · [原片](../../landing/media/native-share-touch-zh-20261007.mp4) · [字幕](../../landing/media/native-share-touch-zh-20261007.vtt) · [原始首帧](../../landing/media/native-share-touch-zh-20261007.png)。
