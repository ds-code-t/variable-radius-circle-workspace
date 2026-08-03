# Agent-Maintained Changelog

This file records technical and behavioral changes that future agents need to understand. Git history remains the authoritative commit history.

## Unreleased

### Added

- Canonical agent-context layer under `.ai/`.
- Cross-agent entry files for AGENTS, Claude, Gemini, GitHub Copilot, and Cursor.
- Documentation-update verification scripts for PowerShell and Bash.
- GitHub Actions workflow for agent-context consistency.
- Architectural decision records for JavaFX, rotation state, deterministic graph resolution, and contact/transmission separation.
- Full AI-DLC adapter layer for Agent Skills, JetBrains AI Assistant, Junie, Amazon Q, Continue, Cline, Windsurf, Cursor, Claude, Gemini, GitHub Copilot, and external repository-reading agents.
- Living capability map, deterministic repository index, review contract, pull-request checklist, and turnkey setup/validation scripts.
- Change-coverage checks that flag behavior changes without tests or canonical living-document updates.

### Changed

- The documentation update protocol now covers feature-map maintenance, generated discovery, repository-layout changes, and adapter ownership.
- Agent-specific files are thin adapters to the root `AGENTS.md`; shared requirements remain canonical under `.ai/`.

### Documentation

- Captured current goals, requirements, mathematics, architecture, UI behavior, testing expectations, and known coverage gaps.
- Documented how task-time context loading and living-document updates work, including the limits of passive automation.

### Fixed

- Added the JUnit Platform launcher to the test runtime so Gradle 9.6 can start JUnit test executors.

## Change-entry format

Future entries should use relevant sections such as:

- Added
- Changed
- Fixed
- Removed
- Tests
- Documentation
- Build

Reference requirement IDs when practical.
