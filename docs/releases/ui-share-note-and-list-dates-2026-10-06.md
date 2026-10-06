# Share note label and readable list dates — development UI

Recorded October 6, 2026 on `0.5.13-dev`, Android code 26. This is a local
debug binding; the downloadable signed candidate12 retains its own evidence.

The optional note now keeps a 13 sp medium label after text is entered, linked
to the editor by `labelFor`. A shorter hint preserves room for the existing
Send and draft feedback. Material, model, permissions and sending are unchanged.
Home and Project History retain relative dates for less than seven days; older
records show an explicit English/Chinese calendar date in the system timezone.
Missing dates say “Date unavailable”/“日期未知”; future dates retain existing
clamping. Elapsed and processing-time formatting are unchanged.

The offline build ran assembleDebug, assembleDebugAndroidTest, testDebugUnitTest
and lintDebug: child exit 0 in 32.156 s, actual 27 JVM tests pass, lint 0 errors/28 warnings.
Its 266 B compiler-deprecation stderr is retained. The 12 date-boundary checks run
inside the existing MobileContractTest wrapper; they are not 12 additional JUnit
or native tests. Unknown/future/recent dates, the seven-day boundary, language,
FORMAT locale and timezone/year changes are covered; global defaults are restored.

The exact SharePresentationTest method passes 1/1 in 27.373 s (child 28.187 s):
four English/Chinese light/dark normal-font windows and two 200% reading windows.
All six complete and reach DESTROYED; 32 lifetime and 12 zero-business-counter
streams are retained, including the font/language restoration event. The source
asserts four material/model header clicks per window and reports 24 in its summary;
there are no individual click logs. Empty/populated note labels, unchanged focus,
restored note/draft, initial normal-font Send/draft visibility and 200% reachability
are asserted within that same method, without activating Send or real keyboard entry.

The 134-source/four-Gradle closure matches before/after build, install and native
reading. Both installed APK byte hashes match the actual build; ten read-only
device observations remain unchanged. [Public bindings and image hashes](ui-share-note-and-list-dates-2026-10-06.json)
record that scope without private paths.

Root directly reviewed all four original 320×640 PNGs: system bars, memory-only
markers, persistent note labels, Send/draft visibility and light/dark contrast.
Each image is copied byte-for-byte into both public asset directories; old images
remain. Raw metadata retains `capture_accepted:false`; the owner's pending visual status
predates this direct review.

- [en/light](../../assets/brand/source-share-note-en-light-20261006.png)
- [en/dark](../../assets/brand/source-share-note-en-dark-20261006.png)
- [zh/light](../../assets/brand/source-share-note-zh-light-20261006.png)
- [zh/dark](../../assets/brand/source-share-note-zh-dark-20261006.png)

These are preset memory-only screens. No new Home/History geometry or native
date-image check was added. This does not establish signed-package pixels,
real IME entry, motion quality, physical TalkBack/performance, genuine material
coverage, installation-to-delivery acceptance or stable marketing readiness.
