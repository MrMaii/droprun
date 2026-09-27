# Development

Requirements: Node.js 24, Git; JDK 17+ and Android SDK 35 for Android. Media tests
need FFmpeg/ffprobe; browser tests need a supported local Chrome/Edge installation.

```sh
npm ci
npm test
npm run site:build
```

Build Android with the checked-in wrapper from `android/`:

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

On Windows use `gradlew.bat`. Never use a debug signature for a public release.
Release signing uses `DROPRUN_KEYSTORE`, `DROPRUN_KEYSTORE_PASSWORD`,
`DROPRUN_KEY_ALIAS`, and `DROPRUN_KEY_PASSWORD`; keep them outside Git.

The Android application, local Connector and private Relay are independent
components. Read [CONTRACTS.md](CONTRACTS.md) before changing their interfaces.
Tests using Miniflare have synthetic state; they do not prove source-platform
access or real-phone behavior. Do not run personal-instance scripts in CI.

Generate both static website languages with `npm run site:build`. Set
`DROPRUN_SITE_URL` for canonical metadata. Real Android screenshots belong in
`landing/media/`; `--require-media` rejects a release-site build without them.

Public releases require the checklist in the release record, an index privacy
check (`npm run check:public`), dependency review and actual anonymous downloads.
Use `ANDROID_SERIAL` to select a test device explicitly. Never erase a user's
application data to make a test pass.
