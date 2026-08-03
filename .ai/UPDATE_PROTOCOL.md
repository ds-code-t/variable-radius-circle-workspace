# Documentation Update Protocol

Every change must be classified and documented in the same branch/commit set as the implementation.

## Behavior or requirement change

Update:

- `.ai/REQUIREMENTS.md`
- `.ai/DOMAIN_RULES.md` when mathematics or invariants change
- `.ai/UI_BEHAVIOR.md` when interaction changes
- `.ai/TESTING.md`
- `.ai/CURRENT_STATE.md`
- `.ai/CHANGELOG.md`
- `docs/agent/feature-map.md` when capability anchors or coverage change
- relevant tests

## Architecture change

Update:

- `.ai/ARCHITECTURE.md`
- an existing ADR or a new ADR under `.ai/decisions/`
- `.ai/CURRENT_STATE.md`
- `.ai/CHANGELOG.md`
- `docs/agent/feature-map.md`
- architecture-focused tests/checks where practical

## Bug fix without intended behavior change

Update:

- regression tests
- `.ai/TESTING.md` if the scenario was missing
- `.ai/CURRENT_STATE.md`
- `.ai/CHANGELOG.md`

Update requirements/domain rules if the bug exposed ambiguity.

## Build, dependency, or runtime change

Update:

- `.ai/PROJECT.md`
- `.ai/ARCHITECTURE.md` when responsibility or tooling changes
- `.ai/TESTING.md` commands
- `.ai/CURRENT_STATE.md`
- `.ai/CHANGELOG.md`
- an ADR for significant platform changes

## Persistence-format change

Update:

- `.ai/REQUIREMENTS.md`
- `.ai/DOMAIN_RULES.md`
- `.ai/ARCHITECTURE.md`
- `.ai/TESTING.md` with migration/compatibility tests
- `.ai/CURRENT_STATE.md`
- `.ai/CHANGELOG.md`
- `docs/agent/feature-map.md`
- an ADR when compatibility strategy changes

## Agent-framework or repository-layout change

Update:

- `AGENTS.md` or the appropriate canonical `.ai` document
- `.ai/AGENT_FRAMEWORK.md`
- `docs/agent/feature-map.md` when discovery/ownership changes
- `.ai/CHANGELOG.md`
- adapter files only when their tool-specific entry behavior changes
- `docs/agent/repository-index.md` by running `python scripts/refresh_agent_index.py`

Keep tool-specific adapters short and pointed at `AGENTS.md`.

## Documentation-only correction

Update the incorrect canonical file and add a changelog entry when the correction materially affects agent understanding. No code change is required.

## Generated repository index

After adding, moving, renaming, or deleting a relevant file:

```bash
python scripts/refresh_agent_index.py
```

Do not hand-edit `docs/agent/repository-index.md`.

## Current-state rules

`CURRENT_STATE.md` must describe what actually exists, not an aspirational design.

After verification, record:

- commands actually run
- results
- date
- JDK and OS when known
- remaining known issues or unverified areas

## Completion rule

A change is complete only when:

1. implementation,
2. automated/manual verification,
3. canonical documentation,
4. capability mapping and generated discovery,

all describe the same behavior and repository layout.
