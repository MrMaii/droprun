# Self-host DropRun

[简体中文](SELF_HOSTING.zh-CN.md) · [Release notes](../releases/0.5.0.md)

This is a public preview. Read the release's validation gaps before using it on
important projects. One Relay belongs to one owner and one Windows Connector;
several phones may pair with it. There is no official DropRun account.

## Before you start

- Windows x64, Git and a signed-in local Codex installation.
- Chrome or Edge for browser validation; Android 8+ for the phone.
- Your own Cloudflare account with Workers, D1 and R2 available. R2 can require
  enabling its subscription in the official dashboard. Provider usage may cost
  money; DropRun does not activate a paid plan on your behalf.
- Optional source extraction uses independently downloaded media tools.

## Install the computer package

Download the installer or portable ZIP from
[GitHub Releases](https://github.com/MrMaii/droprun/releases). Compare the file's
SHA-256 with the release manifest. Windows may identify the unsigned package as
an unknown publisher; the project does not claim a trusted Windows signature.

The installer works per user. For the portable ZIP, extract to a permanent folder
and open `DropRun.cmd`. The package contains Node; no global Node installation is
needed. Configuration and runtime data live in `%LOCALAPPDATA%\DropRun`.

The local browser guide checks prerequisites, opens Cloudflare login, selects
your account and creates a private instance. It records progress so interrupted
provisioning can resume. Do not change the instance name to recover a failed step:
retry the original setup first. Cloudflare management credentials belong to setup,
not the Android app or normal Connector task execution.

In the 0.5.1 candidate, expand **Projects on this computer** after the check to
match a phone's project ID to its local folders. This read-only list includes all
project pages and marks missing folders. Open the folder in Codex and check again
to recover an unavailable project. Folder paths stay in the protected local setup
page; phone access is still granted separately in the Android app.

Start the Connector from the guide. It runs after Windows sign-in; the computer
must remain awake and online to execute tasks. An offline computer does not lose
handoffs already accepted by its Relay.

## Pair Android

Install the release APK, then scan the code shown by your computer. Confirm the
computer and HTTPS Relay address before pairing. The invitation is single-use and
expires. Manual pairing accepts the complete invitation or server/code fields.
Never publish a screenshot of a real pairing code.

The public package is separate from the historical private/debug app. Keep the
old app until you have confirmed the new connection; signatures cannot be
silently exchanged. Authorize only projects you want the phone to access.

## First handoff

Share a reference from an Android app. Pick a project and optionally describe the
change. “Saved” means stored in the phone outbox, not finished by Codex. Follow the
task to see receipt, waiting for the computer, work, approval or a final report.

Every independent share creates a new task. Follow-ups reuse its conversation.
Project totals distinguish independent tasks from dispatches including follow-ups;
network retries do not count. A report must distinguish acquired material from
missing content, and actual changes from a proposed plan.

## Preview and data

Screenshots and files do not require a live computer once uploaded. Supported
static exports use a separate preview Worker and expiring access links. Arbitrary
web applications that depend on a backend or hardcoded runtime asset paths are
not automatically static exports. Live previews require a managed Cloudflare
Tunnel, a domain you control and an online Connector. Quick Tunnels are for local
development, not a production reliability promise.

See [data and privacy](PRIVACY.md) for storage and deletion. Instance resource
limits and retention are controlled by its owner. Preview links grant access;
keep them private unless you intentionally want to share the result.

## Source installation

```sh
npm ci
node scripts/setup.mjs doctor
node scripts/setup.mjs open
```

For an explicit command-line deployment after official Cloudflare login:

```sh
node scripts/setup.mjs login
node scripts/setup.mjs deploy --account YOUR_ACCOUNT_ID --name droprun-personal --accept-cloud-costs
```

`--accept-cloud-costs` acknowledges the provider's usage terms; it does not buy an
unlimited plan. Never put Cloudflare or Connector tokens in a command or issue.
The first deployment uses the fresh baseline. Existing private instances require
the documented upgrade migration, not reapplying the baseline over live data.

## Updates and recovery

Read the release notes, finish or stop active work, and back up your instance and
local data before upgrading. Keep the same data directory and pairing identity.
Do not delete the old package until health and a handoff pass. Relay/schema
updates are explicit owner actions, not silent background changes. Current update
automation and any manual steps are stated in the release notes.

The Windows installer checks a configured Relay for protocol 2/schema 11 and
refuses replacement during active work or failed compatibility checks. Before
replacing an existing installation it copies the old application and local data
to `%LOCALAPPDATA%/DropRun-backups/<id>/`. Only a backup with `backup.json` containing
`complete: true` is ready for recovery. A backup failure stops replacement.
These private backups are not uploaded. Keep a previous installer as well.

Recovery is manual: stop Connector and close setup, retain the failed installation,
reinstall the previous version, then restore the backup's `data` contents to the
same data directory under the same Windows account (DPAPI is account-bound).
Do not restore local data over a running Connector. Cloud data is separate:
export D1 and retain needed R2 objects before any cloud migration. The installer
does not migrate or roll back cloud resources. Portable users keep their old
directory and make the same data backup before switching versions.

For a historical database already at migration `0010`, the compatibility step
is `relay/migrations/0011-selfhost.sql`, applied **once after backup**, followed by
the matching Workers/configuration. Check existing columns first; if
`root_task_id` already exists, do not reapply its ALTER statements. Older or
partially migrated databases require owner review; the fresh baseline is not
an upgrade script. Check `/health` for the expected instance, protocol 2, schema
11 and `ready: true` before pairing. Keep the previous cloud deployment and
database export for recovery; this candidate has no automatic cloud rollback.

If setup fails, retry the saved step and share only redacted diagnostics. If a
source cannot be read, try a directly shared file you have permission to use;
do not treat metadata-only fallback as full video understanding.
