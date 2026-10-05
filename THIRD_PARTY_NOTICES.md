# Third-party software

The Apache-2.0 license applies to DropRun's original source, not to every
executable distributed alongside it. Dependency licenses must accompany releases.

| Component | Purpose | Upstream license/source |
| --- | --- | --- |
| Node.js | Windows runtime | [MIT and bundled notices](https://github.com/nodejs/node/blob/main/LICENSE) |
| node-qrcode | Pairing QR codes | [MIT](https://github.com/soldair/node-qrcode/blob/master/license) |
| ZXing core | Android QR decoding | [Apache-2.0](https://github.com/zxing/zxing/blob/master/LICENSE) |
| Google Material Design icons | Android computer icon, adapted to VectorDrawable | [Apache-2.0](https://github.com/google/material-design-icons/blob/master/LICENSE) |
| AndroidX Test | Development-only device test runner and ActivityScenario | [Apache-2.0](https://github.com/android/android-test/blob/main/LICENSE) |
| resvg-js | Development-only SVG campaign rendering | [MPL-2.0](https://github.com/yisibl/resvg-js/blob/main/LICENSE) |
| Wrangler | User-controlled Cloudflare deployment | [Apache-2.0 / MIT](https://github.com/cloudflare/workers-sdk) |
| cloudflared | Optional live preview tunnel | [Apache-2.0](https://github.com/cloudflare/cloudflared/blob/master/LICENSE) |
| yt-dlp | Optional source extraction | [Upstream distribution licenses](https://github.com/yt-dlp/yt-dlp#license) |
| FFmpeg / ffprobe | Optional media extraction | [Build-dependent LGPL/GPL terms](https://ffmpeg.org/legal.html) |

Media tools are independently invoked programs. An installer or optional-tool
download must preserve the license, upstream source/version and corresponding
source availability for its exact build. Do not label a third-party binary
Apache-2.0 merely because the DropRun source uses that license.

The Android distribution includes versioned ZXing license/notice copies from
[`licenses/android`](licenses/android/README.md), alongside DropRun's LICENSE and
NOTICE, plus the Material icon license. Preserve them when redistributing the APK.

DropRun's existing raster brand asset is included unchanged. System fonts are
used; Apple fonts and SF Symbols are not redistributed. Demo source and images
must be owned by the project or explicitly licensed for redistribution.
