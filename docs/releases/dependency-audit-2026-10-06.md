# Scoped image-library update — October 6, 2026

CI run 37493512308, attempt 1, for source
`28e5f45baed4ab2cb4748b115da50f6476224454` failed its dependency audit.
Its actual core 210/210 and browser 18/18 tests passed; Android's build job
passed. Seven later steps were skipped. The complete 313,427-byte log is
SHA-256 `2ec64cc6680acc73df6bcec2bd42258a8f1a6a3a524c3516eeef3a9fca25ce21`.
This remains a failed CI run.

The [sharp maintainer advisory](https://github.com/lovell/sharp/security/advisories/GHSA-wq5f-xc86-pv6w)
identifies sharp versions below 0.35.5 and a prebuilt fix carrying librsvg
2.63.2. Its possible code-execution impact depends on glibc Linux and runtime
conditions; this review does not establish a DropRun or Windows exploit.
Official sources were checked on 2026-10-06.

The [published Miniflare manifest](https://registry.npmjs.org/miniflare/5.20260926.1-alpha)
pins sharp to exactly 0.35.4. The fix uses npm's
[version-scoped child override](https://docs.npmjs.com/cli/v11/configuring-npm/package-json/#overrides):

```json
"overrides": {
  "miniflare@5.20260926.1-alpha": {
    "sharp": "0.35.5"
  }
}
```

Wrangler 4.144.0, Miniflare 5.20260926.1-alpha and workerd 1.20260926.1 remain
unchanged. Only sharp and its 26 platform/image packages change in the resolved
lockfile. No major upgrade, forced audit fix or Miniflare downgrade is used.

## Actual local checks

After generating the lockfile, a normal install still left the old installed
sharp files. An explicit clean `npm ci --ignore-scripts --no-audit` installed
74 packages; inspection confirmed the actual sharp 0.35.5 package and unchanged
three Cloudflare versions. Resolver status alone was not treated as success.

A native SVG → PNG → raw-pixel smoke check produced an 8×8 image and the exact
lime RGBA pixel `[184, 239, 115, 255]`; loaded versions were sharp 0.35.5 and
librsvg 2.63.2. Actual `npm audit --audit-level=high --json` returned zero
vulnerabilities in all severity categories. The existing 34 core test files
passed 210/210, with zero failures, cancellations, skips or todos in 35.99s.
No browser test was repeated locally in this dependency check.

| Original output | Bytes | SHA-256 |
| --- | ---: | --- |
| Native smoke | 596 | `b1c3087101ad1e5dd6771cc804141f8e255143ad0017f3ef6c7154e5aa64ca6c` |
| Audit JSON | 364 | `becc885dd8c41346454d39dcdee133246749a2990b3fc47d5183fdbbf68a7afc` |
| Core log | 24,368 | `81c1e8dcfed8f0c960d6313b8f107eb34c30ebebffb0c5893f3c272ff938806d` |

All three children returned exit 0 with empty stderr. The first smoke helper
used an incorrect private module path and failed before image processing; its
original output remains. The corrected smoke uses the public `sharp` export.

These are local dependency and core-compatibility checks. They do not turn the
old CI run green, update a signed release, establish genuine Relay acceptance,
or prove that a future public installer contains the new files. A new candidate
must be built from the updated lockfile and checked separately.
