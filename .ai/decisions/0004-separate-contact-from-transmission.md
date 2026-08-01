# ADR 0004: Separate Contact from Rotation Transmission

## Status

Accepted

## Context

Two circles may be mathematically tangent without either circle accepting rotation from the other. Powered circles can touch. A non-powered circle with multiple contacts may choose only one incoming source.

## Decision

A `ContactState` represents explicit geometry only. Rotation transmission occurs only when a non-powered circle selects that contact through its `slaveContactId` and the upstream graph resolves to a powered source.

## Consequences

- Passive and transmitting contacts require different visual indicators.
- Contact creation does not automatically change drive selection.
- Breaking a contact clears any circle selection that references it.
- Recreating a geometrically similar contact does not revive stale selection automatically.
