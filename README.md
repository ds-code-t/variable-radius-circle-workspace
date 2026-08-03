# Variable Radius Circle Workspace — Java 25 / JavaFX 25

A fresh rewrite of the variable-radius circle workspace. It uses JavaFX controls and canvas rendering instead of Processing.

## Requirements

- JDK 25
- Internet access on first Gradle run so Gradle can obtain JavaFX 25 and Jackson

## Repository-native AI context

Coding agents should begin with [`AGENTS.md`](AGENTS.md). It automatically directs them to the project purpose, accepted requirements, mathematical/domain rules, architecture, UI behavior, current implementation state, tests, decisions, and living feature map.

The intent is that normal prompts can describe only the requested change; compatible agents should load the repository context, update affected tests and living documents, and run validation without requiring a separate project introduction. See [`AI-DLC.md`](AI-DLC.md).

Initialize or verify the context on Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup-agent.ps1 -StageGit
```

## Run

Windows:

```bat
gradlew.bat run
```

macOS/Linux:

```bash
./gradlew run
```

## Architectural changes

- Immutable Java records are the model.
- Rotation is recalculated from a directed contact graph on every update.
- `slaveContactId` is the only source of truth for inherited rotation.
- Powered, slaved, and stopped states are explicit.
- JavaFX text fields commit on Enter **and focus loss**.
- JavaFX controls provide native caret, selection, scrolling, resizing, and maximization.
- The displayed angle is deterministic: `startAngle + effectiveRate × masterTick`.

## Interaction

- Drag a circle: resets its rotation to 0 and removes its current contacts.
- Release near a 15° tangency candidate: snaps externally or internally.
- Arrow keys: move without snapping and detach contacts.
- Mouse wheel: zoom around the pointer.
- Middle/right drag: pan.
- Inspector: edit radius/diameter/circumference, rotation, rates, contacts, and slave input.
- Contact degree and unit fields update each other through the model.
- Save/Load JSON and Reset Start are available in the toolbar.

JavaFX 25 requires a modern JDK; this project deliberately targets Java 25.

## Gradle configuration-cache note

This JavaFX project deliberately disables Gradle's configuration cache. The
OpenJFX Gradle plugin currently prepares JavaFX runtime arguments for the
`run` JavaExec task at execution time, which Gradle 9.x does not allow while
storing the configuration cache.

If IntelliJ still passes `--configuration-cache`, the `run` task is explicitly
marked incompatible and Gradle will run it without caching. You can also run:

```bat
gradlew.bat --no-configuration-cache clean run
```
