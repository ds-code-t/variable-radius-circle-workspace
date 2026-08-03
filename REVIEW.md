# Review Contract

Review every functionality change against `AGENTS.md`, `.ai/UPDATE_PROTOCOL.md`, and the applicable ADRs.

## Required review questions

1. Does the implementation match the newest accepted requirement?
2. Does it preserve unrelated behavior and persistence compatibility?
3. Are geometry, contact, and rotation-transmission concepts kept distinct?
4. Is `slaveContactId` still the single authoritative inherited-rotation input?
5. Are derived rotation values deterministic, cycle-safe, and recalculated rather than persisted as competing state?
6. Are internal and external tangency handled correctly?
7. Is UI state only a view/command surface rather than a second domain model?
8. Is there a focused feature or regression test?
9. For UI-only work, is a concrete manual verification recorded?
10. Are all affected `.ai` files, `docs/agent/feature-map.md`, and `.ai/CHANGELOG.md` updated?
11. Is `docs/agent/repository-index.md` current?
12. Were validation commands actually run and reported accurately?

## Block the change when

- behavior changes without applicable tests or a justified manual verification,
- requirements/current-state documentation contradicts the implementation,
- persistence compatibility changes without a migration/compatibility strategy,
- a contact is treated as a rotation source without the explicit transmission rule,
- adapter files duplicate and drift from canonical context,
- validation was skipped without explanation,
- unrelated refactoring obscures the requested change.

Review comments should identify the violated contract and the smallest correction needed.
