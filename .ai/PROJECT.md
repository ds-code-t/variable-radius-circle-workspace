# Project Overview

## Purpose

The Variable Radius Circle Workspace is an interactive JavaFX desktop application for creating circles of different mathematical sizes, positioning them freely, defining explicit internal or external tangencies, and simulating deterministic rotation propagation through selected contacts.

The application is a mathematical workspace, not a physical collision engine. Circles may overlap without interacting unless an explicit logical contact exists.

## Technology

- Java 25
- JavaFX 25 (`javafx.controls`, `javafx.graphics`)
- Gradle Kotlin DSL
- Jackson JSON persistence
- JUnit 5

## Current source layout

```text
src/main/java/com/example/circleworkspace/
├── CircleWorkspaceApp.java   JavaFX application, workspace canvas, controls, interaction
├── Model.java                Immutable records and domain enums
├── RotationSolver.java       Effective-rate graph solver
└── WorkspaceStore.java       JSON load/save

src/main/resources/
└── workspace.json            Bundled initial workspace

src/test/java/com/example/circleworkspace/
└── RotationSolverTest.java   Rotation-solver tests
```

The current code is compact. Future refactoring should move geometry, rotation, persistence, and UI into separate packages as described in `ARCHITECTURE.md`, without changing behavior unintentionally.

## Main entry point

`com.example.circleworkspace.CircleWorkspaceApp`

## Build and run

Windows:

```powershell
.\gradlew.bat --no-configuration-cache clean run
```

macOS/Linux:

```bash
./gradlew --no-configuration-cache clean run
```

The OpenJFX Gradle plugin's `run` task is intentionally incompatible with Gradle configuration caching. Do not re-enable configuration caching for `run` without verifying a newer plugin and updating the build documentation.

## Core terminology

- **Circle**: A variable-radius geometric object with position, starting angle, power setting, own rate, and optional selected slave contact.
- **Contact**: An explicit logical internal or external tangency between two circles.
- **Powered circle**: Uses its own configured angular rate.
- **Slaved circle**: Derives its effective rate from exactly one selected contact.
- **Stationary circle**: Has neither active power nor a resolvable slave source.
- **Passive contact**: A valid contact that is not selected as an incoming rotation source.
- **Master tick**: A deterministic simulation-time coordinate; it may be positive, zero, or negative.
- **Starting state**: The saved reference state used by Reset Start and deterministic tick calculations.

## Non-goals

- Rigid-body collision simulation
- Friction, inertia, acceleration, torque, or force modeling
- Automatic interaction merely because shapes overlap or appear visually close
- Mechanical deformation
- Multiple simultaneous incoming rotation sources for one non-powered circle
