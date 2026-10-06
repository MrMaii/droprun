# Task report reading position — 2026-10-06

Development UI fix: an expanded long report no longer returns to the page top in the checked Activity recreation scenario. The existing Task `ScrollView` now has a stable resource ID so Android can restore its hierarchy state. Existing section expansion state is restored before the report first renders. The change adds no navigation, business action or new persistent task data.

A guarded, nonexported debug fixture supplies one fixed memory report. The same native test APK was used before and after the two production edits:

| Phase | Native result | Scroll Y | Paragraph top | Full report |
| --- | --- | --- | --- | --- |
| Before | 1 test, 1 intended failure; 4.513 s | 1062 → 0 | 595 → 1657 | Expanded |
| After | 1 test passed; 5.096 s | 1062 → 1062 | 595 → 595 | Expanded |

Each phase confirmed two destroyed Activity instances and zero forbidden actions. The test did not scroll or reveal the paragraph after recreation. All recorded child commands returned; before/after device settings matched. The fixture used local font 1.0 while leaving the device's font setting unchanged.

Accepted development source contains 129 Android source files and four unchanged build configuration files. Latest accepted JVM results: 27 passed; lint: zero errors, 30 warnings. The red JUnit result remains a failure even though the owner correctly recognized its intended assertion.

Accepted app APK SHA-256: `f13b90de395e009e070f69bfdb53ba1fb981c1e1f68c1005888d497b2d45b3ab`.
Shared test APK SHA-256: `49dfd7bbc25710cd98df5f2ad5c4915013eecf0de4b7891027523b68bb5c4ed4`.

These are debug development results for `0.5.10-dev` / code 23, not a new installed signed release claim. Validation covers one fixed report and one real in-process Activity instance recreation at the current app language/theme. It does not establish process-death recovery, polling updates, keyboard focus, physical rotation, real-task behavior or performance. No screenshot or animation was produced for this check.
