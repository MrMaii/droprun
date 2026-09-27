# Contributing to DropRun

Start with [development](docs/technical/DEVELOPMENT.md) and the
[component contracts](docs/technical/CONTRACTS.md). Small, focused changes are
easier to review. Describe the user-visible problem, implementation and evidence.

1. Open an issue for a substantial behavior or architecture change.
2. Create a branch and preserve unrelated changes.
3. Add a regression test when behavior changes; run the relevant checks.
4. Update contracts and acceptance criteria when crossing component boundaries.
5. Submit a pull request with screenshots for visual changes and actual commands
   and results for validation. Synthetic fixtures are not real-source evidence.

Never include tokens, pairing invitations, private project paths, source material
or personal recordings. Use `.local/` for machine-specific work. Report security
issues through [SECURITY.md](SECURITY.md), not a public issue containing secrets.

Contributions are provided under Apache-2.0. Preserve dependency notices.
