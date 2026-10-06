# Share material inspection — October 6, 2026

Both pre-Send steps now offer one compact disclosure for received material.
Its 52dp header keeps the existing summary and adds an explicit expansion
affordance. The accessible description retains the complete summary and state.
Expanding shows the exact received text, complete URLs and each full filename,
along with an explanation that actual reading coverage belongs in the report.

Text and names are plain, selectable values. The editor does not fetch a URL,
open a file or add a new Copy action. Empty sections are omitted. The existing
outer scroll handles long material; expansion survives ordinary step changes.
Bundle restoration is implemented but was not exercised in this memory-only run.
The submission and existing paper-plane material chip remain unchanged.

## Verification

- Red: one expected failure in 4.699s; eight differing filename suffixes and the
  full source text had no inspection entry. The first green batch passed two of
  three methods in 43.109s, with a test synchronization failure in the new method.
  Only its bounded wait was corrected. Original diagnostics were preserved.
- The subsequent same batch passed **3/3 in 142.717s**: material inspection,
  compact height and model disclosure. Each used EN/ZH, light/dark and per-Activity
  font1/2. The app APK was identical across these green builds; test APKs differed.
- A later focused material run passed **1/1 in 46.593s**, separately from that
  batch. It corrected the long-text tail target to the last received character,
  verifying first/middle/last-line reachability in the original eight configurations.
  Eight additional native tail pictures show the concluding phrase, including
  its second wrapped line at font2. The earlier limited frames remain unchanged.
- Full values, unellipsized filenames, absence of URLSpan, header state/target,
  project/model/effort/note retention and both pre-Send steps passed. Text-only,
  files-only and single-URL cases additionally ran in EN/light/font1. Closed
  normal forms retained complete Send and draft feedback; font2 requires scrolling.
- JVM **24/24**, no failures/errors/skips; debug lint **0 errors / 30 warnings**.
  These came from the first green build and were not repeated for test-only fixes.
- All 105 final sources match the tail build; the other 102 baseline sources,
  four Gradle configurations and protected submission/flight/header/inset sections
  stayed unchanged. The 670-file archive retains failures, build sources and raw
  captures. All 72 transferred PNG/JSON files match device hashes. Direct visual
  inspection covered 16 earlier final images plus all eight later tail images.
- API, pairing, task saving, Send, navigation and actual checkpoint-persistence
  attempts were zero. Materials and preferences existed only in memory. System
  settings and crash buffer were unchanged. Binder-event originals remain recorded
  without a performance or causality conclusion.

Tested ShareActivity SHA-256:
`9774bd0c4c8de1f0d892a71cb03c0cb86e4ce4404ceff81e16a392eb04c47bb4`.
Green/final/tail debug APK:
`921a4fea1e620fa307af8abd05d6ac9a994f1c07f9c9a32cd93451f796ceffdc`.
The final three-method test and focused-tail test are distinct artifacts.

This verifies local native presentation, not real source receipt or understanding,
IME typing, TalkBack, disabled-motion runtime, Activity/process restoration or
physical performance. The [signed 0.5.7 candidate](0.5.7-rc.1.md) predates this
source-only change. Genuine workflow acceptance and stable marketing remain open.
