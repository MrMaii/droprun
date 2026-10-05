# Android dependency notices

The release APK uses `com.google.zxing:core:3.5.3` for QR decoding.
The adjacent LICENSE and NOTICE are unmodified copies from the
[ZXing 3.5.3 source tag](https://github.com/zxing/zxing/tree/zxing-3.5.3),
retrieved 2026-09-29. The upstream files also describe other ZXing modules;
their inclusion does not mean those modules are bundled with DropRun.

AndroidX Test is only in the debug test APK, not the public release APK.
When changing the runtime dependency version, refresh these upstream files.

`ic_computer.xml` adapts Google's Material Design `computer` icon from
[the upstream SVG](https://github.com/google/material-design-icons/blob/master/src/hardware/computer/materialicons/24px.svg)
to Android VectorDrawable. `Material-Icons-LICENSE.txt` is the unmodified upstream
Apache-2.0 license retrieved 2026-10-05. The source path and adaptation notice are
retained in the drawable; no Apple fonts or SF Symbols are included.
