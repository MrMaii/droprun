# Required setup and optional source tools — October 5, 2026

The Windows browser guide now separates required computer checks from optional
media extraction. Step01 previously put Check again and Install media tools on
the same row, with equally weighted buttons. The existing self-hosting guide
already defines those tools as optional; the interface now reflects that rule.

## Design

Keep the required check action with computer readiness. An always-visible
secondary section follows a divider, with a16px heading and a short explanation
that supported source tools can be installed later. Missing-media badges also
say optional in English and Chinese. The installer action and its pinned-checksum,
upstream-license and no-Python explanation remain visible together.

This uses the existing DropRun palette, spacing, system fonts,48px controls,
focus treatment and motion. No collapse, new dependency, font download or
decorative animation is added. Existing actions, DOM IDs, inline errors, mutation
locks, cost consent, deployment validation and pairing gates are unchanged.
Optional tools do not imply that every source is readable without them.

## Verification

Existing local-browser setup-ui and synthetic setup-actions tests passed7/7 in
5,308.0718ms, with zero failures, skips or cancellations. These fixtures model
local HTTP replies; they do not install software or call an owner's services.

A separate GET-only fixture checked three actual browser scenes: English and
Chinese at390×844 light, and Chinese at1280×1000 dark with CSS zoom2 and reduced
motion. Four optional-section targets per scene were complete and reachable;
the action retained48px CSS minimum height, with no document horizontal overflow.
The three raw PNGs retain a visible local-fixture/no-installation label. Root
inspected all three scenes, as did the independent reviewer.
Captures show the optional region, not the entire cloud form or installation.

| Tested source bytes | SHA-256 |
| --- | --- |
| installer/setup.html | `18d8ee16bc1128163fb91f3bb12afaa87f5dda08bbef757a45721e1098d4533f` |
| installer/setup.css | `76ef5eca00c955b070f01c0f6be03410fd58d57c802050f44cee095b14993f2e` |
| installer/setup-ui.js | `baa42b8dad83dacc949867cc6df19eb891e0cac5e353df045d03de30d467df89` |

Sources were identical before and after the capture. Private
`.local/ux-oct5-setup-audit/` retains originals, baseline and green browser logs,
raw captures, measurements and readonly evidence. Candidate-to-tested comparison
permits only Git's CRLF/LF conversion.

An additional actual-font inspection found Microsoft YaHei UI for Chinese setup
and locally installed Noto Sans SC for the Chinese landing page; English uses
Segoe UI. A screenshot-based suspicion of serif fallback was not confirmed.
No font change was applied. The landing probe's initial incorrect heading-size
assumption and corrected font inspection remain separate; no product styles or
timeouts were changed to obtain that result.

Clean Windows installation, owner Cloudflare provisioning, physical-phone
pairing and genuine first delivery remain outside this local validation. This
refinement does not establish stable-launch or marketing readiness. The existing
0.5.3-rc.1 downloads and website have not yet been replaced.
