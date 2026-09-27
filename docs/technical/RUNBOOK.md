# Runbook

Public installation and recovery instructions now live in
[SELF_HOSTING.md](SELF_HOSTING.md) ([中文](SELF_HOSTING.zh-CN.md)).

Use [DEVELOPMENT.md](DEVELOPMENT.md) for local builds and tests, and
[the release record](../releases/0.5.0.md) for supported artifacts, verification and
known gaps. Never use an old private instance URL or token from a historical
handoff as a public default.

Runtime data belongs in the selected DropRun data directory; `.local/` is reserved
for repository development. Do not delete configuration, signing identities or
paired-device state to repair an unrelated build or UI failure.
