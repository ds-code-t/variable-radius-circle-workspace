# Context-Free Prompt Examples

These examples intentionally omit project introductions and file directions. Compatible agents should discover the repository context themselves.

## Behavior change

> When a circle's radius changes, immediately recheck its contacts and resynchronize rotation for it and directly touching circles.

## UI/readability change

> Keep the number labels inside their circles and hide labels that would overlap at the current zoom.

## Rotation bug

> Fix cases where a stopped circle fails to begin slaving after it snaps to a rotating circle.

## Persistence feature

> Save and restore the label-visibility preference with the workspace while remaining compatible with older saved files.

## Refactor

> Extract the pure tangency and snap-candidate calculations from the JavaFX application class without changing behavior.

## Review request

> Review this branch for correctness, missing tests, documentation drift, and persistence compatibility.

For each prompt, the agent should read `AGENTS.md`, inspect the mapped capability, implement or review the change, update affected living documents, run validation, and report actual results.
