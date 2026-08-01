# ADR 0001: Use Java 25 and JavaFX 25

## Status

Accepted

## Context

The earlier Processing implementation required custom-drawn controls and experienced native/OpenGL and field-editing limitations. The workspace needs native text fields, focus-loss commits, scrolling, responsive layout, resizing, and maximization.

## Decision

Use Java 25 as the project toolchain and JavaFX 25 for the desktop UI and canvas rendering.

## Consequences

- The application requires JDK 25.
- UI controls use native JavaFX behavior.
- Domain logic must remain independent of JavaFX.
- The OpenJFX Gradle plugin currently makes the `run` task incompatible with Gradle configuration caching; the build documents this limitation.
