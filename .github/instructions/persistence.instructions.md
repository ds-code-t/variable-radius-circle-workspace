---
applyTo: "src/main/java/**/WorkspaceStore.java,src/main/java/**/Model.java,src/main/resources/**/*.json"
---

Read `/AGENTS.md`, `.ai/ARCHITECTURE.md`, `.ai/DOMAIN_RULES.md`, `.ai/REQUIREMENTS.md`, and `.ai/TESTING.md`.

Treat field names, enum values, and meanings as compatibility contracts. Persist authoritative state, not effective solver results. Include round-trip and representative older-file tests for format/meaning changes and update all persistence documents required by `.ai/UPDATE_PROTOCOL.md`.
