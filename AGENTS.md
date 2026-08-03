# Repository Agent Contract

This file is the canonical entry point for every coding agent working in this repository. It applies to the entire repository unless a deeper `AGENTS.md` explicitly narrows a rule.

The repository is designed so the user can request a change without repeating the project background or naming context files. Load the repository context automatically before proposing or making changes.

## Mandatory boot sequence

Before changing code, tests, build files, persistence, UI behavior, or project documentation:

1. Read this file completely.
2. Read these canonical project files:
   - `.ai/PROJECT.md`
   - `.ai/REQUIREMENTS.md`
   - `.ai/DOMAIN_RULES.md`
   - `.ai/ARCHITECTURE.md`
   - `.ai/UI_BEHAVIOR.md`
   - `.ai/CURRENT_STATE.md`
   - `.ai/TESTING.md`
   - `.ai/UPDATE_PROTOCOL.md`
   - `.ai/AGENT_FRAMEWORK.md`
3. Read `docs/agent/feature-map.md`.
4. Use `docs/agent/repository-index.md` to find relevant source, tests, resources, and documentation.
5. Inspect the implementation and tests for the affected capability. Never rely on documentation alone when source is available.
6. Check `.ai/decisions/` before changing an architectural or domain decision.

Do not ask the user to restate information already recorded in these files. Resolve normal implementation details from the repository. Ask only when a material product decision truly cannot be inferred.

## Project purpose

Variable Radius Circle Workspace is a Java 25 / JavaFX 25 desktop workspace for creating, moving, snapping, measuring, and rotating circles of different radii. It models geometric contact separately from rotation transmission and recalculates inherited rotation through a deterministic directed graph.

The current implementation is intentionally small and partially monolithic. Improve it surgically. Do not perform a large rewrite or package migration unless the requested change requires one.

## Sources of truth

Use this precedence when sources appear inconsistent:

1. The user's newest explicit instruction.
2. Accepted requirements and invariants in `.ai/`.
3. Accepted ADRs in `.ai/decisions/`.
4. Tests that intentionally encode accepted behavior.
5. Current source behavior.
6. General README or generated inventory text.

When a newer instruction changes an earlier requirement, implement the new behavior and update the canonical documents so they no longer contradict it. Do not preserve stale requirements merely because they were written first.

`docs/agent/repository-index.md` is generated discovery help, not a behavioral authority.

## Non-negotiable domain and architecture rules

Unless the user explicitly changes them and the corresponding canonical documents/ADRs are updated:

- Preserve Java 25 and JavaFX 25.
- Keep authoritative model/domain logic independent of JavaFX where practical.
- `CircleState.slaveContactId` is the single authoritative incoming selection for inherited rotation.
- Effective rotation mode, source, depth, direction, and rate are derived solver results. Do not persist them as competing authoritative state.
- Rotation graph resolution must be deterministic and cycle-safe.
- Physical contact/tangency and rotation transmission are different concepts. A touching circle is not automatically a valid rotation source.
- Geometry code must distinguish internal from external tangency.
- Contact edits reposition only the selected/moving circle unless the user explicitly requests another interaction.
- Preserve current visual angle when changing a source/rate when the existing requirement calls for rebasing.
- Persistence field names, enum values, and meanings are compatibility contracts. Treat changes as migrations, not casual refactors.
- UI controls collect/commit input and render coherent snapshots; they must not become a second rotation solver.
- Derived values must be recalculated after relevant edits and must not become stale cached truth.
- Avoid unrelated formatting, renaming, dependency, or architecture churn.

If an implementation detail seems to conflict with these rules, inspect the canonical domain documents and tests before deciding whether the code or documentation is stale.

## Default functionality-change workflow

For every requested feature, bug fix, behavior adjustment, or refactor:

1. Translate the prompt into a concrete acceptance condition.
2. Locate the capability in `docs/agent/feature-map.md`.
3. Inspect all relevant implementation, tests, UI behavior, persistence, requirements, and ADRs.
4. Identify affected contracts:
   - geometry or mathematical invariants,
   - rotation/slaving behavior,
   - UI interactions and display,
   - persistence compatibility,
   - build/runtime requirements,
   - tests and documentation.
5. Make the smallest coherent change that satisfies the request.
6. Add or update focused automated tests. For UI-only behavior that cannot be sensibly automated yet, record an explicit manual verification procedure.
7. Update the living project documents in the same change.
8. Refresh `docs/agent/repository-index.md` after adding, moving, or deleting relevant files.
9. Run the strongest applicable validation.
10. Report changed behavior, changed files, validation actually run, and any remaining unverified area.

Do not stop after changing code. A functionality change is incomplete until implementation, tests/verification, and canonical documentation agree.

## Documentation update contract

Classify every change using `.ai/UPDATE_PROTOCOL.md`. At minimum:

- New/changed requirement: update `.ai/REQUIREMENTS.md`, `.ai/CURRENT_STATE.md`, `.ai/CHANGELOG.md`, tests, and any affected domain/UI/testing documents.
- Domain mathematics or invariant: update `.ai/DOMAIN_RULES.md`, tests, and usually an ADR.
- Interaction or visual behavior: update `.ai/UI_BEHAVIOR.md` and relevant tests/manual checks.
- Architecture/responsibility/dependency change: update `.ai/ARCHITECTURE.md`, `.ai/CURRENT_STATE.md`, `.ai/CHANGELOG.md`, and an ADR when significant.
- Persistence change: update requirements, domain rules, architecture, testing, current state, changelog, compatibility tests, and an ADR when strategy changes.
- Build/runtime/dependency change: update project, architecture/testing as applicable, current state, changelog, and significant ADRs.
- Bug fix: add a regression test and update current state/changelog; clarify requirements when the bug exposed ambiguity.
- Capability location or coverage change: update `docs/agent/feature-map.md`.
- Relevant file added/moved/deleted: regenerate `docs/agent/repository-index.md`.

Do not mechanically edit every document. Update every affected canonical document and leave unrelated documents alone.

## Test and validation commands

Windows:

```powershell
.\gradlew.bat clean check
.\scripts\agent_validate.ps1
```

macOS/Linux:

```bash
./gradlew clean check
./scripts/agent_validate.sh
```

Agent-contract-only checks:

```bash
python scripts/verify_agent_contract.py
python scripts/refresh_agent_index.py --check
```

When comparing a change branch with a base branch:

```bash
python scripts/verify_agent_contract.py --base-ref origin/main --strict
```

Run the existing `scripts/verify-agent-docs.ps1` or `.sh` checks when available. Run the JavaFX application for manual UI verification when the change affects interaction or rendering and the environment supports it.

Never claim a command passed unless it was actually run. State environmental limitations plainly.

## Change boundaries

- Preserve public behavior not targeted by the request.
- Prefer focused changes over speculative redesign.
- Do not invent hidden requirements.
- Do not weaken or delete tests merely to make a change pass.
- Do not silently alter sample `workspace.json` semantics.
- Do not commit generated build output, IDE state, secrets, or local machine paths.
- Do not commit or push unless the user explicitly asks and the environment permits it.
- Keep agent-specific adapter files short. Put shared truth here or under `.ai/`, not duplicated across every tool adapter.

## Definition of done

A change is complete only when all applicable items are true:

- Requested behavior is implemented.
- Existing behavior outside the request remains intact.
- Focused regression/feature tests are present or a justified manual check is documented.
- Required `.ai` documents are current.
- `docs/agent/feature-map.md` still points to the correct implementation and tests.
- The generated repository index is current.
- Relevant validation has passed, or limitations are reported accurately.
- The final summary identifies behavior, files, tests, documentation, and unresolved risks.

## Read-only and external agents

An agent that can only read the GitHub repository should still begin here, inspect the linked context, and base recommendations or patches on it. It must not assume the repository page summary alone contains the full contract. When it cannot edit or execute, it should provide a concrete patch/change plan and clearly distinguish verified facts from proposed changes.
