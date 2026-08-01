# Architecture

## Current implementation

The current project uses four main classes in one package:

- `CircleWorkspaceApp`: JavaFX application, canvas rendering, UI controls, input handling, and orchestration.
- `Model`: immutable records and enums.
- `RotationSolver`: pure rotation graph calculation.
- `WorkspaceStore`: JSON persistence.

This is functional but concentrates too much responsibility in `CircleWorkspaceApp`.

## Target package structure

Refactor toward this structure when changes justify it:

```text
com.example.circleworkspace
├── application/
│   ├── CircleWorkspaceApplication.java
│   ├── WorkspaceController.java
│   └── WorkspaceCommands.java
├── model/
│   ├── WorkspaceModel.java
│   ├── CircleState.java
│   ├── ContactState.java
│   ├── Tangency.java
│   └── DriveMode.java
├── geometry/
│   ├── ContactGeometry.java
│   ├── SnapSolver.java
│   └── MeasurementConversions.java
├── rotation/
│   ├── RotationGraph.java
│   ├── RotationSolver.java
│   └── RotationResult.java
├── persistence/
│   ├── WorkspaceData.java
│   └── WorkspaceStore.java
└── ui/
    ├── WorkspaceView.java
    ├── WorkspaceCanvas.java
    ├── CircleInspector.java
    ├── ContactInspector.java
    └── ClockToolbar.java
```

Do not perform a large package move without updating tests, persistence compatibility, imports, entry points, and this document.

## Dependency direction

```text
ui -> application -> model
ui -> read-only geometry/rotation results
application -> model, geometry, rotation, persistence
geometry -> model
rotation -> model
persistence -> serializable model data

model -X-> JavaFX
geometry -X-> JavaFX
rotation -X-> JavaFX
persistence -X-> JavaFX
```

## Responsibilities

### Model

- Own immutable authoritative state.
- Validate local record invariants.
- Provide copy/with methods or command results.
- Contain no JavaFX classes.

### Geometry

- Convert radius, diameter, circumference, degrees, and units.
- Generate 15-degree snap candidates.
- Validate internal and external tangency.
- Reposition only the selected/moving circle for contact edits.
- Contain no rotation graph logic.

### Rotation

- Resolve effective mode, rate, depth, and source from current state.
- Use `slaveContactId` as the single incoming-selection source.
- Detect invalid references and cycles safely.
- Be deterministic and free of UI dependencies.

### Persistence

- Serialize authoritative state and clock settings.
- Do not persist effective solver results as authoritative values.
- Validate and report malformed files clearly.

### Application layer

- Apply commands atomically.
- Recalculate derived geometry and rotation results after edits.
- Rebase starting angles when a rate/source change must preserve current appearance.
- Publish a coherent snapshot to the UI.

### JavaFX UI

- Render snapshots.
- Collect user input and submit commands.
- Bind or synchronize paired fields.
- Commit on Enter and focus loss.
- Never become the authoritative rotation solver.

## State-update sequence

For a normal edit:

1. Commit pending field input.
2. Validate and create updated authoritative state.
3. Recalculate contacts if geometry requires it.
4. Resolve the full rotation graph.
5. Recalculate displayed angles for the current tick.
6. Render one coherent snapshot.
7. Update inspector values without generating recursive edits.

## Persistence compatibility

Changes to JSON record names, enum values, or field meanings are persistence-format changes. They require:

- a documented migration or compatibility strategy,
- tests loading an older representative file,
- an ADR when the change is architectural,
- updates to `PROJECT.md`, `REQUIREMENTS.md`, and `CURRENT_STATE.md`.
