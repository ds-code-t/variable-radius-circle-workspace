# ADR 0002: Use One Source of Truth for Rotation Input

## Status

Accepted

## Context

A former design stored both a circle-selected contact ID and separate contact drive flags. These values could disagree after a contact was removed and recreated, causing a visually selected slave relationship to produce no rotation.

## Decision

`CircleState.slaveContactId` is the only authoritative incoming rotation selection for a non-powered circle.

Contacts contain geometry and identity, not a second authorization flag for the same relationship.

## Consequences

- Recreated contacts receive new IDs and must be selected explicitly.
- The solver validates that the selected contact exists and involves the circle.
- UI toggles edit `slaveContactId` directly.
- Legacy contact flags, if encountered during migration, must not block propagation.
