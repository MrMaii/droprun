# Share project reads: explain the state and keep the intent

Accepted local development check, October 7, 2026 UTC. A failed project request
previously looked like waiting for Connector, with no recovery action. Share
now distinguishes loading, confirmed empty and failure, and offers explicit
Retry/Refresh. Cached projects can still be chosen while reading.

The change keeps the existing Java Views and 48dp secondary control, with disabled
feedback, localized text and a polite accessibility status. Refresh does not
restart Share or reset its material, note, destination, model, effort or step.
Activity-summary failure is separate from catalog failure. A valid `projects`
array is required before replacing the catalog cache. Requests keep the Store
they started with; callbacks cannot update another Store or a destroyed page.
These last identity/malformed-response branches were source-reviewed, not all
exercised at runtime.

Only ShareActivity behavior and one nonexported debug fixture/test registration
changed. Existing Share fixtures, Store, Ui, retained imports, Send and pairing
guards were unchanged. [Selected evidence](ui-share-project-load-2026-10-07.json).

One fresh debug build, install and native run pass, with 1/26/25 direct
children returning 0. The freeze of 139 source files and four configurations is unchanged before
and after each action. The saved, current and installed app/test APK bytes match.
Actual JVM results are 27 tests with zero failures/errors/skips; lint has zero
errors and 29 warnings. The new warning is the fixed caption in the debug fixture
literal (`SetTextI18n`); it is not production copy or a localization result.

The sole opted-in native method passes in 7.839s:

- EN/light/font1: held loading, deduplication, catalog failure, real Retry,
  successful empty catalog with failed activity read, then populated recovery.
- ZH/dark/font1: choose a cached project through its actual row into the note
  step; catalog failure preserves values and editor/control identities. Real
  Back reveals the failure. A second read is released only after actual
  destruction; admitted memory work drains without any new UI calls.

There are 14 ordered events, two completed/destroyed windows, five project and
five activity memory GETs, seven exact cache applies, zero forbidden counters,
and restored language/theme/palette. Executors terminate and a main-thread
barrier completes before proceeding. Held destroyed View reads check identity
and text only. Ten read-only device identity/settings values match afterward.

The caption follows the project rows so status changes do not move those rows.
With six cached projects it may be below the initial viewport; this take checks
two cached projects and does not accept large-directory first-screen visibility.
No nonempty search, 200% type, TalkBack, screenshot, recording, physical touch,
performance, store swap, network, import, persistent draft, Send or signed-App
result is claimed. This is local memory recovery, not genuine connection or
process-loss acceptance. Candidate 15 remains unchanged until a new candidate is
separately built and published.
