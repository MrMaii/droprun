# Cancellable permission feedback — October 5, 2026

This source refinement is newer than the unchanged
[0.5.3-rc.1 download](0.5.3-rc.1.md). It corrects an animation callback on the
share screen. It does not change the permission request or establish genuine
server authorization, sharing, physical-device performance or stable launch.

## User-visible behavior

After the project-permission request succeeds, its confirmation animation may
advance only while its original dialog, generation, selected project and project
step remain current and the page is alive. Closing that feedback, opening a
later dialog, changing the selection during dismissal or destroying the page
makes the old callback inactive. A replacement opened and closed during the old
dismissal also invalidates it, even though the dialog reference is null again.

The existing check animation, reading delay, dismiss timing, request, permission
scope, failure retry and normal successful advance are retained. Closing success
feedback does not revoke a permission already granted by the server. There is
no new confirmation or network operation.

## Reproduction and verification

An independent, non-exported debug fixture renders the actual ShareActivity,
CheckView and Glass over readonly memory. Its visible page/dialog marker says
“UI probe · simulated permission result · no request sent”. It uses the existing
sent-state recovery bypass so ShareImport is never opened. Permission changes,
API/credentials, preferences edits, files, pairing, task save, outbox, services,
Send and business navigation are guarded. It never presses Allow; it invokes
only the success presentation to simulate an already received response.

The original single native regression failed **1/1**, in 3.447s (host4.3788006s):
closing confirmation A and opening dialog B let A's delayed callback close B.
The assertion expected the same Glass and found null. ADB exit0 was not treated
as success. The English assertion failed before the Chinese loop; the final
guard assertion after that failed assertion did not execute. Guard checks up
through confirmation passed. The original source/APKs and exact log remain.

After the local generation/identity/selection checks, **5/5 methods passed** in
26.905s (host27.7611063s), each in English and Chinese, ten actual UI scenes:

1. The current confirmation advances exactly once with the same selection.
2. Closing confirmation A leaves a later permission dialog B attached/current.
3. Opening and closing B during A's actual dismiss prevents A's old advance.
4. Changing the selected project during dismiss prevents the old advance.
5. Destroying the page prevents its old animation callback from dismissing or
   navigating afterward.

Tests wait for actual CheckView completion, the existing delayed callback and
dismiss window, two native frames and main-thread idle. Their bounded3s waits
and assertions were not weakened. Back is invoked through the Activity callback;
this is not evidence of real system-Back input or IME behavior. All successful
paths assert no import and forbidden actions0. No screenshot or performance
measurement is claimed by this callback regression.

The corrected fixture red build passed36s; the green build passed42s. Both passed
JVM24/24 with no failures/errors/skips and lint0errors/30warnings. The initial
fixture build's API28 accessibility-pane call failed min26 lint before any native
run; its API guard is corrected and the original failed build is preserved.

The final ten source hashes matched before/after. Settings stayed exactly
320×640/160dpi/font2.0, animator null/window1/transition1 and the original default
IME. No setting, process reset, kill or reboot was used. Event logs retained the
earlier Task-fixture creation crash and showed no new ANR/crash in either run.

| Saved tested artifact | SHA-256 |
| --- | --- |
| Original ShareActivity.java | `768f2738728234c2db22c10fb087205917b0c9f65994e8899adf010f15fd57e1` |
| Corrected ShareActivity.java | `078ab1af1be57721fc02f826ebc3ed7ca3da19c10a52a48c935933f7b17c0a0f` |
| Green debug APK | `7ac99f0c6e5af8b6182e9a5911fa43bf1a93eaf7b7edbd4faf1bb495850e361b` |
| Green test APK | `fb63e9e9176f913d382562ddb2e35d507eec6bd26d825893c18279c75d9550c1` |

Private `.local/ux-oct5-permission-motion/` retains the original failure, corrected
red/green source copies, three APK pairs, verification records and66-file index.
Source hashes refer to saved tested bytes, before Git line-ending normalization.
The independently verified Task source and earlier evidence stayed unchanged.
Real authorization persistence, motion-off, physical accessibility/performance
and the complete share-to-delivery flow remain separate acceptance work.
