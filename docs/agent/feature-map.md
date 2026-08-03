# Living Feature Map

This file maps user-visible and domain capabilities to current implementation, tests, and canonical context. Verify anchors in source before editing; update this map when ownership or coverage changes.

## Workspace shell, rendering, and input orchestration

- Current implementation:
  - `src/main/java/com/example/circleworkspace/CircleWorkspaceApp.java`
- Supporting model:
  - `src/main/java/com/example/circleworkspace/Model.java`
- Canonical context:
  - `.ai/PROJECT.md`
  - `.ai/ARCHITECTURE.md`
  - `.ai/UI_BEHAVIOR.md`
  - `.ai/CURRENT_STATE.md`
- Coverage:
  - Primarily integration/manual UI verification; add extracted pure-logic tests when changing behavior.
- Notes:
  - The class currently combines JavaFX application setup, canvas rendering, controls, input, and orchestration. Refactor incrementally only when a requested change benefits from it.

## Circle state, measurements, and authoritative records

- Current implementation:
  - `src/main/java/com/example/circleworkspace/Model.java`
  - `src/main/java/com/example/circleworkspace/CircleWorkspaceApp.java`
- Canonical context:
  - `.ai/REQUIREMENTS.md`
  - `.ai/DOMAIN_RULES.md`
  - `.ai/UI_BEHAVIOR.md`
- Coverage:
  - No dedicated measurement test class currently identified; add focused tests when extracting/changing conversion or invariant logic.
- Contracts:
  - Radius/diameter/circumference and mark/contact values must remain internally coherent.
  - Derived display values must not become competing authoritative state.

## Circle movement, snapping, and tangency

- Current implementation:
  - `src/main/java/com/example/circleworkspace/CircleWorkspaceApp.java`
  - `src/main/java/com/example/circleworkspace/Model.java`
- Canonical context:
  - `.ai/DOMAIN_RULES.md`
  - `.ai/REQUIREMENTS.md`
  - `.ai/UI_BEHAVIOR.md`
- Coverage:
  - Current UI/integration behavior plus manual verification; add pure geometry tests when changing snapping/tangency.
- Contracts:
  - Distinguish internal/external tangency.
  - Release-near-candidate snapping uses the accepted angular candidate rules.
  - Contact edits normally reposition only the selected/moving circle.

## Contact creation and contact metadata

- Current implementation:
  - `src/main/java/com/example/circleworkspace/Model.java`
  - `src/main/java/com/example/circleworkspace/CircleWorkspaceApp.java`
- Canonical context:
  - `.ai/DOMAIN_RULES.md`
  - `.ai/REQUIREMENTS.md`
  - `.ai/UI_BEHAVIOR.md`
- Coverage:
  - Rotation-related contact selection is covered through solver/link/resync tests; geometry/UI contact editing needs focused tests as logic is extracted.
- Contracts:
  - A physical contact does not by itself establish rotation transmission.

## Rotation graph and effective rotation

- Current implementation:
  - `src/main/java/com/example/circleworkspace/RotationSolver.java`
  - `src/main/java/com/example/circleworkspace/Model.java`
- Tests:
  - `src/test/java/com/example/circleworkspace/RotationSolverTest.java`
- Canonical context:
  - `.ai/DOMAIN_RULES.md`
  - `.ai/ARCHITECTURE.md`
  - `.ai/REQUIREMENTS.md`
  - `.ai/decisions/0002-single-source-for-rotation-input.md`
  - `.ai/decisions/0003-deterministic-rotation-graph.md`
  - `.ai/decisions/0004-separate-contact-from-transmission.md`
- Contracts:
  - `slaveContactId` is the sole incoming selection.
  - Effective mode/rate/source/depth are derived.
  - Resolution is deterministic and cycle-safe.
  - Displayed angle follows the accepted start-angle/effective-rate/master-tick rule.

## Rotation-link selection policy

- Current implementation:
  - `src/main/java/com/example/circleworkspace/RotationLinkPolicy.java`
- Tests:
  - `src/test/java/com/example/circleworkspace/RotationLinkPolicyTest.java`
- Canonical context:
  - `.ai/DOMAIN_RULES.md`
  - `.ai/REQUIREMENTS.md`
  - `.ai/decisions/0002-single-source-for-rotation-input.md`
  - `.ai/decisions/0004-separate-contact-from-transmission.md`
- Contracts:
  - Source eligibility and selection must be explicit and deterministic.
  - Touching alone must not silently create a transmission link.

## Rotation resynchronization after edits

- Current implementation:
  - `src/main/java/com/example/circleworkspace/RotationResyncPolicy.java`
  - orchestration call sites in `CircleWorkspaceApp.java`
- Tests:
  - `src/test/java/com/example/circleworkspace/RotationResyncPolicyTest.java`
- Canonical context:
  - `.ai/REQUIREMENTS.md`
  - `.ai/DOMAIN_RULES.md`
  - `.ai/UI_BEHAVIOR.md`
  - `.ai/CURRENT_STATE.md`
- Contracts:
  - Re-evaluate the edited circle and directly touching circles after relevant state changes.
  - Preserve deterministic selection and avoid stale slaving state.

## Clock, powered/slaved/stopped modes, and angle continuity

- Current implementation:
  - `src/main/java/com/example/circleworkspace/CircleWorkspaceApp.java`
  - `src/main/java/com/example/circleworkspace/RotationSolver.java`
  - `src/main/java/com/example/circleworkspace/Model.java`
- Tests:
  - `src/test/java/com/example/circleworkspace/RotationSolverTest.java`
- Canonical context:
  - `.ai/REQUIREMENTS.md`
  - `.ai/DOMAIN_RULES.md`
  - `.ai/UI_BEHAVIOR.md`
- Contracts:
  - Rebase start angles where required so a rate/source change does not visually jump.
  - UI rendering consumes a coherent solver snapshot.

## Zoom, pan, label readability, and inspector behavior

- Current implementation:
  - `src/main/java/com/example/circleworkspace/CircleWorkspaceApp.java`
- Canonical context:
  - `.ai/UI_BEHAVIOR.md`
  - `.ai/REQUIREMENTS.md`
  - `.ai/CURRENT_STATE.md`
- Coverage:
  - Manual UI verification unless behavior is extracted into pure layout/filtering functions.
- Contracts:
  - Readability decisions may change what is drawn, not underlying circle mathematics.
  - Text fields commit on Enter and focus loss.

## JSON save/load and sample workspace

- Current implementation:
  - `src/main/java/com/example/circleworkspace/WorkspaceStore.java`
  - `src/main/java/com/example/circleworkspace/Model.java`
- Resource:
  - `src/main/resources/workspace.json`
- Canonical context:
  - `.ai/ARCHITECTURE.md`
  - `.ai/REQUIREMENTS.md`
  - `.ai/DOMAIN_RULES.md`
  - `.ai/TESTING.md`
- Coverage:
  - Add round-trip and older-representative-file tests before changing persisted record names, enum values, fields, or meanings.
- Contracts:
  - Persist authoritative state and clock settings, not effective solver results.
  - Treat schema/meaning changes as compatibility changes.

## Build, runtime, and project setup

- Current implementation:
  - `build.gradle.kts`
  - `settings.gradle.kts`
  - `gradle.properties`
  - `gradle/wrapper/`
  - `setup-project.ps1`
- Canonical context:
  - `.ai/PROJECT.md`
  - `.ai/ARCHITECTURE.md`
  - `.ai/TESTING.md`
  - `.ai/decisions/0001-use-java-25-and-javafx-25.md`
- Contracts:
  - Preserve Java 25 / JavaFX 25 unless explicitly changed.
  - Keep configuration-cache behavior compatible with the OpenJFX run task.

## Repository-native AI context and enforcement

- Canonical contract:
  - `AGENTS.md`
  - `.ai/AGENT_FRAMEWORK.md`
  - `.ai/UPDATE_PROTOCOL.md`
- Discovery/review:
  - `docs/agent/`
  - `REVIEW.md`
- Automation:
  - `scripts/verify_agent_contract.py`
  - `scripts/refresh_agent_index.py`
  - `scripts/agent_validate.ps1`
  - `scripts/agent_validate.sh`
  - `.github/workflows/agent-context.yml`
- Adapters:
  - root and hidden tool-specific instruction/skill files listed in `docs/agent/repository-index.md`
- Contracts:
  - Adapters point to `AGENTS.md`.
  - Skill copies remain identical.
  - Code changes update tests and affected living context in the same change.
