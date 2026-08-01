# ADR 0003: Recalculate a Deterministic Rotation Graph

## Status

Accepted

## Context

Incremental mutable propagation can retain stale rates after detaching, reconnecting, changing radius, changing power, or changing source selection. The master clock also needs deterministic past/future tick jumps.

## Decision

Resolve every circle's effective drive mode and angular rate from the current authoritative graph. Memoization is scoped to one solve. Detect cycles safely.

Displayed angle is derived from starting angle, resolved rate, and master tick.

## Consequences

- No authoritative cached inherited rate is persisted.
- Results do not depend on list order.
- Graph changes take effect immediately.
- Source-free cycles stop safely.
- Rate/source changes at nonzero tick may require rebasing the starting angle to preserve visual continuity.
