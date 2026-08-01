# Agent-Maintained Changelog

This file records technical and behavioral changes that future agents need to understand. Git history remains the authoritative commit history.

## Unreleased

### Added

- Canonical agent-context layer under `.ai/`.
- Cross-agent entry files for AGENTS, Claude, Gemini, GitHub Copilot, and Cursor.
- Documentation-update verification scripts for PowerShell and Bash.
- GitHub Actions workflow for agent-context consistency.
- Architectural decision records for JavaFX, rotation state, deterministic graph resolution, and contact/transmission separation.

### Documentation

- Captured current goals, requirements, mathematics, architecture, UI behavior, testing expectations, and known coverage gaps.

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

### Fixed

- Added the JUnit Platform launcher to the test runtime so Gradle 9.6 can start JUnit test executors.
