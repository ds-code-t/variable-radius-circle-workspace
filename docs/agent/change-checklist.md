# Functionality Change Checklist

## Before editing

- [ ] Read `AGENTS.md`.
- [ ] Read all canonical `.ai` files named in its boot sequence.
- [ ] Locate the capability in `docs/agent/feature-map.md`.
- [ ] Inspect relevant implementation, tests, resources, and ADRs.
- [ ] Convert the prompt into concrete acceptance conditions.
- [ ] Identify geometry, rotation, UI, persistence, build, and compatibility impact.

## During implementation

- [ ] Make the smallest coherent change.
- [ ] Preserve unrelated behavior.
- [ ] Keep contact separate from rotation transmission.
- [ ] Keep authoritative state separate from derived solver results.
- [ ] Keep model/solver logic free from JavaFX dependencies where practical.
- [ ] Add focused feature/regression tests.
- [ ] Record a concrete manual UI check when automation is not practical.

## Living context

- [ ] Update `.ai/REQUIREMENTS.md` for accepted behavior.
- [ ] Update domain/UI/architecture/testing documents where affected.
- [ ] Update `.ai/CURRENT_STATE.md` to match reality.
- [ ] Update `.ai/CHANGELOG.md`.
- [ ] Add/update an ADR for a durable architectural/domain decision.
- [ ] Update `docs/agent/feature-map.md` when anchors or coverage changed.
- [ ] Regenerate the repository index after path changes.

## Validation and handoff

- [ ] Run the strongest applicable Gradle checks.
- [ ] Run agent-contract and index checks.
- [ ] Run/manual-check the JavaFX UI when applicable.
- [ ] Report only commands actually run.
- [ ] Summarize behavior, code, tests, documentation, and remaining risks.
