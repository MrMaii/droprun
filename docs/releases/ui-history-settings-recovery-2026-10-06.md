# History, search and settings recovery — October 6, 2026

Three refinements in the native Android development client:

- History distinguishes loading, a failed read and confirmed empty results.
  Cached rows stay visible; null or blank error details have useful Refresh
  guidance. A failed append cannot become a successfully loaded page.
- Share search matches the label people actually see, including disambiguating
  ID prefixes, or a complete project ID. Selection, permissions and ordering stay
  intact.
- Settings offers Refresh/Retry beside model availability. It reuses the existing
  three read endpoints once at a time, shows partial failure, keeps cached models
  and preserves the current reading position or surviving keyboard selection.
  It does not submit an execution-mode change.

## Actual local checks

The first frozen build contains 133 native source files and four build configs
and returned successfully in 54.782s. JVM tests passed 27/27; lint reports zero
errors and 30 existing warnings. Version is `0.5.10-dev`, code 23. Source closure
SHA-256: `65f2d1cb84e3ed298bd4f89d5ac9d236e9acc219ce202334360751286c15c92c`.

| Fixed-memory native check | Actual result | Scope |
| --- | --- | --- |
| History | 1/1 passed, 8.88s | Three known windows; six actual reads; loading, failure, Refresh to confirmed zero, failed append and cached-list preservation |
| Share search | 1/1 passed, 6.717s | Two known windows; displayed prefix, displayed full label, full ID, name, no match and cleared query |

History has 21 actual zero-guard observations; Share has 18. All five windows
reached `DESTROYED` and their owned executors terminated. English/light/normal
text and Chinese/dark/200% text are represented. History checks keep its adapter,
cache and visible anchor; Share checks keep the existing search view, selection
and material while exercising the actual text-change renderer.

The owner initially expected raw instrumentation output but used its human
format. History's original JUnit result passed; the owner then stopped at parsing.
Its failed owner record remains. Original output was parsed separately, without
rerunning History. A continuation used raw output for Share and Settings.

### Settings failure and fix

Settings first failed its actual 1/1 native test in 7.915s. In the cached-failure
scene, the selected model row was 54px tall but only 48px visible. Three known
windows closed; the fourth was not entered. This failed output remains.

The unchanged-data completion branch now also uses the existing preference
reveal-on-layout behavior. No test or fixture changed. A new source closure
`4d577923860894a6b8119998c260eede8f6329bed01f26739c9fd04de82db733`
differs only in Settings. Its build returned successfully in 42.125s. The app
APK is `3a2778735ffa2775531cb13995b4425bce15dc5806eeddc748e547610d62f40b`;
the unchanged test APK is
`7cce8d9756dc3b9d23053172450a208e90724068c6bf742dc8249907b52712e1`.

The same Settings test then passed 1/1 in 10.729s: four known windows closed,
25 actual zero-guard observations, nine completed read batches/controller starts,
eight UI callbacks and 25 memory-cache applications. Retry and offline recovery
work; the keyboard-selected row is fully visible, and the touch reading position
remains 2771→2771 after changed data. The final known pending read drains after
closing without a later UI update. Language, dark mode and palette are restored.
The existing installed test APK was verified, not reinstalled. All 26 commands
returned; ten before/after device properties and the frozen source match.

| Original native output | Bytes | SHA-256 |
| --- | ---: | --- |
| History pass | 12,376 | `8957140b388a8b2648b9ebce13699b10d5007b2864df380400d8a19d93a61904` |
| Share pass | 10,234 | `4bcab63bbf98fc16800fa79392b48b3d422289ec3b242615b921450bf1b44f76` |
| Settings failure | 22,258 | `7b0fa412df308685be9509934704afcaae06e6482d62c442e695ac207ba2f812` |
| Settings pass | 27,414 | `7a9789ba30eea2c52382a6479ac216465de1e84ca7a04fbcfaf913cabbf6b7b1` |

## Limits

These are fixed-memory UI checks on the dedicated emulator, not genuine Relay
or Codex work. The three-project search uses programmatic queries; it does not
establish human search entry when that small catalog initially hides the input.
No 240-row/process-loss path, disk outbox, real pagination, OS/IME changes,
captures, frame measurements or spoken TalkBack is claimed. Settings mode
operations and announcement counts were not observed by this test.

History and Share retain their original source/APK binding; their results are
not relabelled as a new run of the later Settings-only build. These changes are
ahead of the signed [0.5.10 candidate](0.5.10-rc.1.md). They do not establish
stable launch or readiness for marketing.
