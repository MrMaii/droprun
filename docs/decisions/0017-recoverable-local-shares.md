# ADR 0017: Recoverable local shares

Status: Accepted — 2026-09-29

## Context

Retained attachment bytes alone do not recover an unfinished share. Android can
end its original page or process before the user submits it; an Activity Bundle
cannot provide a durable, discoverable note and target outside that original task.

## Decision

Keep the editor snapshot with the existing instance-scoped atomic import journal.
Expose unfinished shares through Home and pre-pairing setup, separate from project
history and submitted outbox entries. Recovery uses a non-exported Activity and
the existing share editor. It never submits automatically.

One live editor owns each import; recovery returns to an existing editor or claims
an unowned import. Writes run serially off the UI thread and cannot resurrect an
explicitly discarded or transferred import. Ending a page is not a discard choice.
Pre-pairing material needs target confirmation; another paired instance's material
cannot be adopted. Exact behavior is owned by [CONTRACTS](../technical/CONTRACTS.md).

## Rationale and consequences

One journal ties choices to the same material identity without a second database
or cross-store transaction. Explicit saving/error feedback distinguishes a durable
snapshot from pending edits. Only completed writes survive process loss. A missing
original URI can still prevent recovery of an incomplete attachment; verified
complete copies remain usable. Unfinished drafts consume phone storage until the
user submits or discards them; there is no silent sweep of older unknown material.

## Alternatives

- Activity state alone: cannot discover a share whose task no longer returns.
- File-only recovery: loses the user's intent and selected destination.
- Outbox entries created on receipt: would conflate editing with consent to submit.
- A new draft database: unnecessary ownership and migration complexity here.

## Verification

Native local tests cover owner conflicts, instance isolation, interrupted copying,
persisted edits, explicit discard, failed writes, outbox ownership and first-pairing
confirmation. A separate emulator process-loss probe removes the source fixture
before recovering the saved copy. Exact evidence and remaining real-provider/device
gates are maintained in the [UX release record](../releases/0.5.1-ux.md).
