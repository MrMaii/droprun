# Share project focus survives row replacement

Accepted local development, 2026-10-07; outside immutable signed candidate16.
Keyboard users keep the same surviving project after `renderProjects` replaces
its rows. Each row has a tag containing the complete project ID. Restoration
only runs when the previous row had non-touch focus, and its rectangle is
revealed only while the replacement still has focus. Search and Refresh retain
their own focus. A filtered or removed project has no new fallback behavior.
Selection, order, fields, read requests, permissions and submission are unchanged.

## Closed local regression

1. Baseline: the second memory project takes keyboard focus before rebuilding.
   Both EN/light and ZH/dark lose that same-ID focus after replacement: actual
   observed preservation is0/2. The sole diagnostic JUnit succeeds in13.217s;
   this records the defect, rather than asserting a repair.
2. Accepted: the same method requires surviving full-ID focus and a wholly
   visible replacement. Both windows pass,2/2, in13.172s. Search and Refresh
   separately keep the same focused control through their own rebuilds. All12
   material, selection, catalog, input and step fields stay exact.
3. Both takes close two known DESTROYED windows and drain their executors.
   Each retains23 ordered events,16 zero guard snapshots, restored font/language/
   theme/palette,146 frozen source/config files,56 returned-zero direct children
   (build1/install26/native29),112 raw streams and six source receipts. Separate
   file-only readers rehash and reparse each closed take. XML reports show27
   JVM tests without failure/error/skip and lint0errors/29retained warnings.

Root and a separate reviewer directly viewed both before and both after original
PNGs. Both after originals have complete memory markers, project labels, Search
and Refresh. The second row's gray focus fill is distinct from the first row's
green selected state and check. Original launcher pixels and system bars remain;
the launcher background is excluded from acceptance.

- [English/light original](../../assets/brand/source-share-focus-en-light-20261007.png)
- [中文/深色原图](../../assets/brand/source-share-focus-zh-dark-20261007.png)
- [Selected source, APK and receipt hashes](ui-share-project-focus-2026-10-07.json)

## Limits

These are two fixed font1 memory windows and local row rebuilds. No asynchronous
catalog request, removal/fallback, business click, input, Send, Pair or real Relay
is exercised. Touch-mode restoration means the framework setter returned; it is
not a fresh observation of global mode. Only ten saved device read values are
compared. Continuous motion,200% pixels, physical keyboard/device, performance,
signed-App and full UX acceptance remain separate. Owner/parser pixel flags stay
false; direct visual review is separately scoped above. The public APK is unchanged.
