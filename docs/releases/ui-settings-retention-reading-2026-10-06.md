# Settings retention reading — development UI

Recorded October 6, 2026. This extends the earlier
[retention typography record](ui-history-retention-reading-2026-10-06.md) with a
local native reading check. The run uses `0.5.11-dev`, Android code 24 debug
packages; it does not verify the [signed candidate](0.5.11-rc.1.md).

## Actual native scope

Nonce `71f69d02-9d1f-400e-ba00-0eef59e9e2dc` selected only
`SettingsReadingPresentationTest#readingSectionsKeepChoicesAndProjectIdentity`
with the accepted memory fixture and capture disabled. JUnit passed 1/1 in
11.524 s; the known native child returned 0 with empty stderr. The raw output
contains 56 ordered lifetime events, six entered/completed/DESTROYED windows,
24 guard rows with `forbidden=0 work_factories=0 model_saves=0`, and zero captures.

The six windows cover English and Chinese, light and dark at 100% text, plus
English/light and Chinese/dark at 200%. In each window, the frozen method checks
three unknown-policy paragraphs, then five paragraph instances for a fixed
in-memory policy of 14 upload days and 60 screenshot/file days. These include the
recovery instruction, cached-policy heading, actual interpolated day values and
both explanation paragraphs. It checks the unknown/cached heading transition,
complete text layout without ellipsis, and first/last line reachability.

The source performs 16 head/tail checks per window. There are no per-paragraph
geometry or reading-value logs; the complete-method JUnit result and source
binding support those assertions, not a separately recorded line count. The
original retention key was absent in all six fixtures and was removed again on
restoration. The method asserts original key presence and value-reference
restoration, but the existing-key restoration branch was not exercised.

## Exact bindings

| Binding | SHA-256 |
| --- | --- |
| Frozen 134 Android sources/four Gradle configurations | `79816d021acc6290b7539574f47955dee9b2cdfb940acb93b127d9b00b391d7b` |
| Settings production | `0a1882603d4f42459425183d9ee55ae4c301321e57a070e62f504420315e7f12` |
| Reading test source | `f003268bf16e0eb3435be4b7b09aabf6d48941f945a34e2c77af21d391969509` |
| Debug app APK, 2,183,717 bytes | `7ac4046a80066bb7079e3be216263b1cf0e359d93121a42c3b3274477f22110d` |
| Debug test APK, 2,028,565 bytes | `667e4144210584a9c9e8b969a97964547fb5228ed8489f4cf63e8d8b5bd6bb50` |
| Original native stdout, 8,989 bytes | `faa6dc02b8f6248cf25a1225dc5bbbcbe483c648332045c01106b64cd7602e48` |
| Separate independent output verification | `28e62c2159938a0a6fd6d3b72d9add98d8d8d9967a8e6e2ebb98407c7d723290` |

## Preserved owner failure

The original owner returned 1 after the native command and after-state reads.
Its parser replaced the Gradle `configs` variable with window identities, so the
final source-closure check and its `finally` path attempted to read an identity
as a file. The original final verification file was not produced. This was an
owner post-processing failure; the native child and JUnit results remain distinct.

A separate verifier reread the existing outputs, reconstructed the frozen
134-source/four-config closure, matched built and installed APK bytes, and found
all ten recorded device observations byte-identical before/after. Its output
verification passed without rerunning the owner or device test. The original
owner failure is retained and is not relabelled PASS.

## Limits

The fixture uses guarded memory preferences, no-op refresh and blocked API/request
paths. The added retention fragment changes only its retention Map; no business
action or persistent setting is invoked. Font scale belongs to the Activity
configuration, not an OS font write. DESTROYED is recorded; executor termination
is not measured.

This does not establish every intermediate line's visibility, real policy sync
or cleanup, real-account/business behavior, physical accessibility, keyboard
entry, motion or frame/press performance. No retention screenshot was produced;
existing Settings images retain their older bindings. Signed-package, genuine
delivery and stable launch acceptance remain open.
