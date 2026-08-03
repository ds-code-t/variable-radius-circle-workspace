---
name: variable-radius-circle-functionality-change
description: Implement or review a functionality change in Variable Radius Circle Workspace while preserving geometry, rotation, persistence, tests, and living repository context.
---

# Variable Radius Circle Functionality Change

Use this workflow whenever a request changes behavior, requirements, UI interaction, geometry, rotation, persistence, build/runtime behavior, or architecture.

1. Read repository-root `AGENTS.md` completely.
2. Load every canonical `.ai` file in its mandatory boot sequence.
3. Locate the capability in `docs/agent/feature-map.md`.
4. Inspect implementation, tests, resources, and relevant ADRs.
5. Derive concrete acceptance conditions from the user's newest instruction.
6. Identify impacts on:
   - circle geometry and tangency,
   - contact versus rotation transmission,
   - rotation graph/source/resynchronization,
   - UI/input/rendering,
   - persistence compatibility,
   - tests and living documentation.
7. Make the smallest coherent implementation.
8. Add focused tests or document a concrete manual UI check.
9. Apply `.ai/UPDATE_PROTOCOL.md` in the same change.
10. Update the feature map when ownership/coverage changes.
11. Regenerate the repository index when relevant paths change.
12. Run the strongest applicable validation and report only actual results.

Never treat physical touching as automatic transmission, create a second source of truth for inherited rotation, persist effective solver results as authoritative, or leave requirements/current-state documentation stale.
