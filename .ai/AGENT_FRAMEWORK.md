# Agent Framework

## Goal

A user should be able to request a feature, fix, or adjustment without restating the repository purpose, architecture, domain rules, current behavior, or documentation workflow.

Every compatible agent must discover `AGENTS.md` automatically or through its small tool-specific adapter. `AGENTS.md` then directs the agent to this canonical `.ai/` knowledge base and the living feature map.

## Canonical versus generated content

Canonical human/agent-maintained project truth:

- `PROJECT.md`
- `REQUIREMENTS.md`
- `DOMAIN_RULES.md`
- `ARCHITECTURE.md`
- `UI_BEHAVIOR.md`
- `CURRENT_STATE.md`
- `TESTING.md`
- `UPDATE_PROTOCOL.md`
- `CHANGELOG.md`
- `decisions/`
- `docs/agent/feature-map.md`

Generated discovery content:

- `docs/agent/repository-index.md`

Thin adapters must not become independent sources of project truth.

## Task-time update behavior

For each code-changing task, the agent must:

1. load the canonical context before editing,
2. identify the affected capability and contracts,
3. update implementation and verification,
4. update every affected living document in the same branch/commit set,
5. regenerate the repository index when paths change,
6. run validation and report the actual result.

There is no passive background documentation watcher. Manual edits made outside an agent can still drift; CI and local contract scripts provide guardrails.

## Requirement lifecycle

- The newest explicit user instruction supersedes older conflicting text.
- Accepted behavior must be written into the canonical requirements/current-state documents during the same change.
- Removed or replaced requirements must be edited or marked obsolete rather than left as contradictory active instructions.
- Architectural decisions belong in ADRs when the reasoning will matter to future agents.
- `CURRENT_STATE.md` describes what exists now, never merely what is planned.

## Adapter maintenance

All adapters must direct the tool to `AGENTS.md`. Shared rules are edited once in `AGENTS.md` or the appropriate `.ai` document. Agent Skill copies are kept byte-for-byte identical and validated by script.

## Enforcement

- `scripts/verify_agent_contract.py` checks required files, adapter links, identical skill copies, and optional change coverage.
- `scripts/refresh_agent_index.py` generates deterministic repository discovery.
- `scripts/agent_validate.*` combines context checks with Gradle verification.
- `.github/workflows/agent-context.yml` enforces the contract in pull requests and pushes.
- `REVIEW.md` and the pull-request template make omissions visible during review.

Heuristics warn when implementation/build changes have no test or living-document changes. Strict CI treats those warnings as failures. Narrow environment overrides exist only for genuine false positives and must not be used to hide missing work.
