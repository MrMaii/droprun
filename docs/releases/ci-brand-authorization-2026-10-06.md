# Brand authorization test — October 6, 2026

CI run [37488186095, attempt 1](https://github.com/MrMaii/droprun/actions/runs/37488186095)
for source `f04330d4b831b4b46d53085e2fc97974b058c736` failed. Core passed
209/210 tests; browser passed 18/18 and Android succeeded. Eight downstream
steps in core were skipped. This run is not recorded as successful.

The sole failure was `TypeError: terminated`, caused by `read ECONNRESET`, while
reading the response to the phone's rejected brand upload in local Miniflare.
It was not a logo-hash or permission assertion mismatch. Worker authentication
returns before reading a rejected request body. The test sent the full 1,432,103B
logo for that permission check. The test, runtime helper, Worker, logo,
dependencies and workflow were unchanged from the parent commit.

The three authorization probes now send a fixed 32B body. They still require
anonymous 401 and phone/Connector 403. Administrator invalid-content 400, full
selected-logo upload 200, public PNG type/byte hash and private-object denial
remain unchanged. No production Worker or retry behavior changes.

After this adjustment, the local brand and existing Miniflare transport tests
passed 2/2, with no failure/cancellation/skip/todo. The original brand test also
passed once locally before the adjustment. These observations neither reproduce
the CI reset nor prove its trigger or a CI fix. Rejected large-logo transfer
reliability is no longer exercised by this authorization test. New-source CI
must establish the current result; the original failed run and full log remain.

Full original CI log SHA-256:
`2d2410991100c228299cbf060dbd2fff6f1e62998db23f4c82ddb2068eecf595`.
Local two-test log SHA-256:
`e256e5e8cc81ab5d821e75bf6f76ff300eaa9d0b29d846ef06c69642648fa825`.
