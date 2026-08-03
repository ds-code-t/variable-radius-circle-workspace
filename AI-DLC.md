# Repository-Native AI Development Context

This repository includes a project-specific AI-DLC-style context and enforcement layer. Its purpose is to let the user request normal changes without repeatedly explaining the project or naming instruction files.

## What is canonical

- `AGENTS.md`: universal repository contract and change workflow.
- `.ai/`: project purpose, accepted requirements, domain rules, architecture, UI behavior, current state, testing, decisions, and documentation update protocol.
- `docs/agent/feature-map.md`: living map from capabilities to implementation, tests, and documentation.
- `docs/agent/repository-index.md`: generated file inventory.
- `REVIEW.md`: review-time omission checks.

Agent-specific files are thin adapters. They point to `AGENTS.md` rather than maintaining separate copies of project truth.

## What “automatic” means

Compatible coding agents are instructed to load the context at the start of each task and to update affected requirements, current-state notes, tests, feature mappings, decisions, and changelog entries in the same change as code.

This is task-time automation. There is no passive background process that can detect unrelated manual edits after the fact. The validation scripts and CI catch many common omissions.

## One-command setup

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup-agent.ps1 -StageGit
```

macOS/Linux:

```bash
./setup-agent.sh --stage-git
```

The setup scripts do not commit or push.

## Routine validation

Windows:

```powershell
.\scripts\agent_validate.ps1
```

macOS/Linux:

```bash
./scripts/agent_validate.sh
```

Agent-context-only:

```bash
python scripts/verify_agent_contract.py
python scripts/refresh_agent_index.py --check
```

## Supported repository adapters

The repository includes adapters or skills for universal `AGENTS.md` readers, OpenAI/Codex-style Agent Skills, Claude Code, Gemini CLI, GitHub Copilot/custom agents, Cursor, Continue, Cline, Windsurf, Junie, JetBrains AI Assistant, and Amazon Q.

An external agent browsing GitHub can read the root `AGENTS.md` and all canonical context without access to an IDE plugin.
