# Repository Agent Context

This directory supports repository-native coding agents. It is not a runtime dependency of the JavaFX application.

## Canonical files

- `/AGENTS.md` — universal repository contract, workflow, invariants, validation, and definition of done.
- `/.ai/` — project-specific purpose, requirements, domain rules, architecture, UI behavior, current state, tests, decisions, and update protocol.
- `/docs/agent/feature-map.md` — living capability-to-code/test/documentation map.
- `/docs/agent/change-checklist.md` — completion checklist.
- `/docs/agent/repository-index.md` — generated relevant-file inventory.
- `/REVIEW.md` — review-time checks.
- Agent Skill copies — reusable functionality-change workflow for compatible agents.

Tool-specific instruction files are deliberately short adapters to `AGENTS.md`.

## Expected behavior

A prompt such as:

> Make rotation slaving resynchronize whenever a touched circle changes.

should cause a write-capable agent to:

1. load `AGENTS.md` and the canonical `.ai` context,
2. find rotation-link/resync anchors in the feature map,
3. inspect source, tests, requirements, domain rules, UI implications, and ADRs,
4. implement a focused change,
5. add regression tests,
6. update affected living documents and changelog,
7. refresh generated discovery when paths change,
8. run validation and report results.

This is task-time automation, not a passive background watcher. Manual code changes do not update documentation by themselves.

## Setup and maintenance

Windows:

```powershell
.\setup-agent.ps1 -StageGit
```

macOS/Linux:

```bash
./setup-agent.sh --stage-git
```

Refresh discovery:

```bash
python scripts/refresh_agent_index.py
```

Validate context:

```bash
python scripts/verify_agent_contract.py
python scripts/refresh_agent_index.py --check
```

Full project validation:

```bash
./scripts/agent_validate.sh
```

Windows:

```powershell
.\scripts\agent_validate.ps1
```

Change-coverage enforcement can compare with a base ref:

```bash
python scripts/verify_agent_contract.py --base-ref origin/main --strict
```

Temporary narrowly scoped false-positive overrides:

- `AGENT_CONTRACT_ALLOW_NO_DOCS=true`
- `AGENT_CONTRACT_ALLOW_NO_TESTS=true`
- `AGENT_CONTRACT_ALLOW_NO_CHANGELOG=true`

Do not use overrides to bypass genuinely missing work.
