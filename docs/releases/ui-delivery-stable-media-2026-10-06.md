# Current delivery screenshots — October 6, 2026

README and website source now use four complete native initial viewports from
one capture set: English/Chinese, light/dark, normal Activity-local text size.
The result, fixed-snapshot explanation and delivery actions are visible. The
original memory-only/no-work-sent notice and system bars remain intact.

The dark originals show readable white status glyphs. Earlier images remain
under their original names; no pixels were repaired, cropped or resized. This
observation does not identify a platform cause or establish an appearance fix.

| Original | SHA-256 |
| --- | --- |
| [English light](../../assets/brand/source-delivery-stable-en-light-20261006.png) | `e9f123773f61d90315a7fe1f1126c503efe3bb0e831e67f157148ba46f404c5a` |
| [English dark](../../assets/brand/source-delivery-stable-en-dark-20261006.png) | `a6b2b61774a4c68bf987bb364ac7c2299b4252d443fd760b86015e7e2a6d8668` |
| [中文浅色](../../assets/brand/source-delivery-stable-zh-light-20261006.png) | `5b0a5133c498e7d19a42026b8bde5f707715ab3bd095ee9b9f4eb1c34f80ae75` |
| [中文深色](../../assets/brand/source-delivery-stable-zh-dark-20261006.png) | `5d39aafc25aa6bf71b8712e96f65718149a522d06ea3f1512e52487395e2c779` |

## Native capture

An explicitly enabled capture-only test opens the existing nonexported, guarded
memory fixture. It uses a synthetic version and `.invalid` preview URL. No click,
scroll, preview, task, file action, recreation or OS-setting change occurs.

Each window completes two pre-draws and a measured 2003–2008ms compositor wait,
then checks focus, layout, alpha, visible marker and no IME. The native test passes
1/1 in 12.527s; all four windows reach `DESTROYED` before transfer. Nine exact
PNG/JSON/run-result files match device-to-host hashes. Both owner and root viewed
all four originals individually. Requested bar appearance flags alone are not
treated as proof of the rendered pixels.

Only one capture test was added. The preceding 112 source files and four Gradle
files remain byte-identical. The development app is unchanged at
`a63be969d30710c624f13efed4be9dd25ac91cdcffdf1af4cddd1338b44cbca7`,
with 0.5.7-dev/code20 metadata. Assembly passes; no new JVM/lint result is claimed.

The first host attempt stopped after twelve successful read-only identity checks
because its AVD parser mishandled CRCRLF. It performed no install, native test or
transfer. A whitespace parser successor passed twenty offline cases but was not
executed; a further finite install-ack correction passed sixteen cases against
the prior actual successful stdout. Only the final helper ran a native capture.
Original diagnostics remain preserved. No denial or unknown lifetime was retried.

These are labelled development UI samples. Lower page actions may be outside the
initial viewport. They do not prove genuine delivery, complete-page usability,
physical accessibility/performance, a signed-package recording or stable release.
The independent [file-preview UI record](ui-delivery-file-verification-2026-10-06.md)
covers a different guarded fallback fixture.
