# Android dependency notices

The release APK uses `com.google.zxing:core:3.5.3` for QR decoding.
The adjacent LICENSE and NOTICE are unmodified copies from the
[ZXing 3.5.3 source tag](https://github.com/zxing/zxing/tree/zxing-3.5.3),
retrieved 2026-09-29. The upstream files also describe other ZXing modules;
their inclusion does not mean those modules are bundled with DropRun.

AndroidX Test is only in the debug test APK, not the public release APK.
When changing the runtime dependency version, refresh these upstream files.
