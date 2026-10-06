# Navigation icons and current Home media — local verification

Date: 2026-10-06 UTC. Native source refinements, not a signed-package recording
or genuine handoff. The current signed 0.5.7-rc.1 package remains unchanged.

The settings gear retains its existing path, now with transparent fill and a
1.8-unit round stroke. A matching circular information vector replaces the old
History information glyph. Both use a 24×24 viewport and 24dp intrinsic size.
Existing 48dp controls, theme tint, focus, press behavior, descriptions and
callbacks remain. This pass changes two production resources and one resource
reference; it does not replay press or business actions.

Compile and lint pass: 0 errors, 30 warnings. One capture-only native method
passes in 23.763s: Home/History × EN/ZH × light/dark, Activity font1, eight windows
and zero clicks. Readiness includes focus, attachment, layout, alpha1, two
pre-draws and two seconds of settling. All eight raw 320×640 images were directly
inspected for readable markers/system bars and complete icons. Initial gear/Back
keyboard-focus rings remain visible. Lower list content continues below the frame.

All 16 PNG/JSON device-host hashes match. The 107-file baseline becomes 109:
105 original files unchanged, gear and History reference changed, information
vector and capture harness added. Ui, fixtures, manifests, existing tests and
Gradle configs are byte-identical. JVM, History and motion suites were not rerun.
Device settings and crash log match before/after; system event differences remain
diagnostics, without a causal or performance claim. Earlier History778 and
Material670 archives remain intact; this pass seals 272 files read-only.

The initial preparation console serialization error and offline verifier's wrong
tint expectation are preserved. The corrected expectation uses actual Ui.TEXT,
not the vector's base stroke. Neither correction changed the accepted source,
APK, capture or device. Debug app SHA-256:
`4cc4ebe3e95a338d84651e3ee445e9e8f62d09f05c470160a1ada57840d1763a`.

The four Home images are copied byte-for-byte to new dated README filenames.
Both language galleries now show the current aligned card hierarchy and outline
gear. All 34 earlier native PNG/GIF files and the brand mark remain unchanged.
The first copy guard counted 34 but found 35 including the mark; it stopped before
copying. Its original helper/diagnostic remain; the corrected guard then succeeded.

Two localized installation links also follow the README tour. Source verification
checks their labels, positions and existing targets; the six local first-minute
renders that motivated this edit were pre-edit Markdown approximations. No fresh
GitHub rendering probe or stopped-platform replay is claimed.

No genuine task, API, sync, sending, import, pairing, business persistence, IME,
process reset or OS change occurred. TalkBack, physical performance and genuine
end-to-end acceptance remain in [the candidate record](0.5.7-rc.1.md).
