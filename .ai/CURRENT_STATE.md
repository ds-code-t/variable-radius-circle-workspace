# Current State

Last documentation refresh: 2026-08-01

## Current implementation

- Java 25 and JavaFX 25 desktop application.
- Gradle Kotlin DSL build.
- Immutable model records in `Model.java`.
- Rotation graph resolution in `RotationSolver.java`.
- JSON persistence in `WorkspaceStore.java`.
- JavaFX workspace, canvas, and inspector currently concentrated in `CircleWorkspaceApp.java`.
- Bundled starting state in `src/main/resources/workspace.json`.

## Confirmed architectural rules

- `CircleState.slaveContactId` is the sole incoming rotation-selection source.
- Powered circles use their own rate.
- External inheritance reverses direction.
- Internal inheritance preserves direction.
- Effective rates are derived from the graph.
- Gradle configuration caching is disabled/unsupported for the OpenJFX `run` task.

## Known test coverage

- A rotation chain test verifies authoritative slave contact IDs and external radius-ratio propagation.

## Known coverage gaps

- Detach/reconnect regression test
- Circle ordering independence
- Internal tangency
- cycle handling
- invalid contact references
- deterministic tick calculation
- geometry and persistence tests
- automated focus-loss field commit coverage

## Current focus

1. Expand rotation-solver regression coverage before further feature work.
2. Keep application behavior and agent documentation synchronized.
3. Gradually separate geometry and UI responsibilities from `CircleWorkspaceApp` when making related changes.

## Last verification

- Command: Not recorded by this drop-in package.
- Result: Not claimed.
- JDK: Not recorded.
- OS: Not recorded.

The next agent that runs verification must replace the entries above with actual results.

## Build compatibility update

- The test runtime explicitly includes `org.junit.platform:junit-platform-launcher`, as required by Gradle 9.6 test execution.
