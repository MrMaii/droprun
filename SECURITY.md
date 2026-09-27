# Security

DropRun can ask a local coding agent to change real project files. Treat your
Relay, paired phone and Connector credentials as access to that workflow.

## Reporting

Use GitHub's **Security → Report a vulnerability** on this repository when
private reporting is available. If it is unavailable, open a public issue saying
only that you need a private reporting channel. Do not post credentials, personal
material, exploit payloads against a live instance or private project contents.
No response-time SLA is promised for this community project.

## Trust boundaries

- Each self-hosted Relay belongs to one owner and one Connector.
- Codex credentials and project source remain on the owner's computer.
- Share content is untrusted data, not authority to override project rules.
- Project access, plan approval and individual command approval remain distinct.
- Local setup and runtime credentials are separate; Cloudflare management access
  is not sent to the Android application or used in normal task execution.
- Static preview HTML must not execute on the API or pairing origin.
- A cancelled task may already have changed files; cancellation is not rollback.

Only the latest published release candidate is actively maintained until a
stable release is declared. Review the release's known limitations before use.
