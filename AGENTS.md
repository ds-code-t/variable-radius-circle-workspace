# Agent Instructions

This repository contains a Java 25 and JavaFX 25 desktop application for creating, positioning, snapping, rotating, and mathematically coupling variable-radius circles.

## Required reading order

Before changing code, tests, build files, persistence formats, or behavior, read:

1. `.ai/PROJECT.md`
2. `.ai/REQUIREMENTS.md`
3. `.ai/DOMAIN_RULES.md`
4. `.ai/ARCHITECTURE.md`
5. `.ai/UI_BEHAVIOR.md`
6. `.ai/CURRENT_STATE.md`
7. `.ai/TESTING.md`
8. `.ai/UPDATE_PROTOCOL.md`

Review relevant records under `.ai/decisions/` before changing an established design decision.

## Mandatory workflow

For every implementation change:

1. Identify the affected requirement IDs and invariants.
2. Inspect existing tests before editing implementation code.
3. Preserve the boundaries between model, geometry, rotation, persistence, application, and JavaFX UI code.
4. Add or update automated tests for changed behavior and bug fixes.
5. Run the verification commands in `.ai/TESTING.md`.
6. Update all affected canonical `.ai` documents in the same change.
7. Update `.ai/CURRENT_STATE.md` with the actual current status.
8. Add an entry under `Unreleased` in `.ai/CHANGELOG.md`.
9. Add or revise an ADR when an architectural decision changes.

A task is incomplete when code, tests, and canonical documentation disagree.

## Critical restrictions

- Do not introduce a second source of truth for rotation inheritance.
- `CircleState.slaveContactId` is the authoritative selected incoming contact.
- Do not use a second per-contact boolean to authorize the same slave relationship.
- Do not persist derived effective rotation rates as authoritative state.
- Do not confuse geometric contact existence with rotation transmission.
- Do not calculate rotation propagation in JavaFX view/controller code.
- Do not mutate the stationary/reference circle when editing a selected circle's contact coordinates.
- Do not silently change domain behavior without updating requirements and tests.
- Do not remove or weaken tests merely to make a build pass.
- Do not claim a command or test passed unless it was actually executed.
- Preserve Java 25 and JavaFX 25 unless a documented architecture decision changes them.

## Required commands

Windows:

```powershell
.\gradlew.bat --no-configuration-cache clean check
.\scripts\verify-agent-docs.ps1
```

macOS/Linux:

```bash
./gradlew --no-configuration-cache clean check
./scripts/verify-agent-docs.sh
```

See `.ai/TESTING.md` for focused and manual verification.
