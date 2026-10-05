# DropRun roadmap

Current scope is the Android + Windows self-hosted open-source edition described
in [ADR 0015](../decisions/0015-public-selfhost-release.md). No hosted subscription,
iOS application or app-store launch is included. Evidence and current pass/fail
results live in [release validation](../releases/0.5.1-rc.2.md).

## 1. Reproducible baseline

Curated public source, deterministic dependencies, Node and Android checks,
Apache-2.0 and third-party notices. Exit: clean checkout builds and checks pass.

## 2. Product integration

Project-first history, retry-safe counts beyond 200 records, pending merge,
share recovery, consolidated settings, honest delivery and accessible motion.
Exit: grouping/permission/offline tests and actual UI walkthroughs pass.

## 3. Self-host installation

Per-user Windows installer and portable archive, resumable Cloudflare setup,
instance-bound pairing, independent Android signing, separate preview origin.
Exit: install, deploy, pair, deliver, upgrade and uninstall are demonstrated.

## 4. Release validation

- Three real projects and five consecutive complete tasks, with no duplicate run.
- Twenty real source samples documenting actual text/audio/frame access/failures.
- Offline phone/PC, interrupted upload, killed process, retries, revoked access,
  rejected approval, expired preview, failed upgrade and two-Relay isolation.
- Midrange physical Android: slow frames below 5%, no freeze over 700ms,
  press-feedback p95 below 100ms; TalkBack, 200% text and reduced motion.
- Clean Windows and fresh Cloudflare onboarding. A synthetic test or emulator
  does not satisfy a physical-device or genuine-source gate.

Unavailable field gates are documented for the explicitly accepted candidate;
they remain required before claiming stable-release readiness.

## 5. Public candidate and launch

Publish curated source and version-matched signed APK/Windows artifacts to GitHub.
Publish the bilingual GPTSites website with genuine app captures, labelled demo
data, working downloads and docs. Deliver README GIF, captioned demo videos and
bilingual campaign copy. Social posting requires a subsequent explicit request.

Exit: anonymous downloads work, documented setup is reproducible, release evidence
is visible and campaign files match the shipped features. Candidate publication
must preserve its acceptance gaps; publication alone does not prove stability.
