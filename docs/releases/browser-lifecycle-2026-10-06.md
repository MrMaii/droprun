# Windows browser lifetime and ownership

Date: 2026-10-06 UTC. Unreleased Connector source, separate from the unchanged
0.5.5 packages. Real media sign-in and management-stop acceptance remain open.

## Problem and change

During the hosted-page check, Edge's initial compatibility launcher exited with
code 0 while its successor browser and DevTools endpoint remained alive. The
old startup loop reported failure, and killing that exited child could not close
the successor. A media-login loop also used the initial child's lifetime.

Startup now waits within its existing 20-second deadline and binds ownership to
the newly written `DevToolsActivePort` in DropRun's dedicated profile. The browser
and OS select the port; its browser UUID must exactly match the loopback version
endpoint before connecting. An unknown endpoint receives no shutdown command.
The retained browser socket defines the browser's lifetime.

Owning callers await close. The command uses that original socket, then checks
endpoint refusal within three seconds. A failed close preserves the profile;
repeating it can recheck the same endpoint, without connecting to a replacement
browser. Cookie export through a borrowed port leaves ownership with its caller.
Media-login cleanup retains its busy barrier until closure is confirmed, with
an error in the local log and instructions to close DropRun's window manually.

## Verification

- Initial Chrome regression: 3 methods, 1 pass / 2 expected failures for missing
  browser-lifetime behavior. Its first corrected run passed 3/3; the then-affected
  group passed 17/17. These are earlier source snapshots.
- A later reused-port regression failed once because a rejected close promise
  was permanently cached. After correction, the final focused run passed 4/4.
- Final related run: 29/29 methods in 26.3208234s, no failure, skip, cancellation
  or todo. It covers browser, Relay snapshot browser, setup actions, setup UI,
  pairing-page and synthetic Connector-stop tests. Seven source syntax checks pass.
- Four fresh Edge profiles used a local synthetic EN/ZH fixture at desktop/narrow
  sizes. All initial launchers exited 0 while their bound browser sockets stayed
  alive. All four identity checks and raw screenshots passed; close took
  127–168ms, endpoint refusal was confirmed, and each profile's process count
  went from 14 to 0 before removal. Only four local GET requests occurred.
- The reused-port case closes its original browser, starts its own HTTP server
  on the old port, and confirms cleanup cannot shut down that replacement. It
  reports failure while the replacement responds, then succeeds after it stops.

The 29-method run used Chrome 154.0.8037.98; the four separate Windows fixture
scenes used Edge 154.0.4258.53. No Linux/macOS or visible account-login claim.
The `main` login/stop barrier received source review, not a real sign-in test.

## Provenance and retained limits

Seven final source copies matched working bytes before the routine 0.5.6
health-version bump. Only that version field changed later in `main`; browser
functions and the other six source files remain unchanged. The frozen private
inventory has 66 payloads plus its manifest; an earlier failure manifest is also
retained. Manifest SHA256:
`a2a90c2e5e127b46aad186e9c1293eabb246289d741b5fd270a16f643ef40e3c`.
Final `connector/browser.mjs` SHA256:
`2576adea3d7c371464d64bf29d43f3739c61e1e60fe1eac8c1ea72a7da68503d`.

The initial hosted reduced-motion failures, earlier source/test runs, first
helper's incorrect Chrome/Edge process filter and raw captures remain separate.
One diagnostic failure profile is retained. Those failures were not relabelled
as final passes. This source change alone does not certify the hosted site's
reduced-motion behavior, a genuine preview, media login, upgrade or uninstall.

Official protocol checked 2026-10-06:
[Chrome DevTools browser endpoint and DevToolsActivePort](https://chromedevtools.github.io/devtools-protocol/index.html).
The precise runtime boundary belongs to [CONTRACTS](../technical/CONTRACTS.md).
