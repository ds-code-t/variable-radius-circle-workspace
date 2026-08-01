# Product Requirements

Requirement IDs are stable references for tests, changelog entries, and future changes.

## Circle measurements

- `GEO-001`: Each circle has one positive radius value.
- `GEO-002`: Diameter equals `2 × radius`.
- `GEO-003`: Circumference equals `2π × radius`.
- `GEO-004`: Radius, diameter, and circumference fields are synchronized representations of the same size.
- `GEO-005`: Updating any size representation updates the rendered circle immediately after commit.
- `GEO-006`: Circumference markings use the same linear unit system for every circle.
- `GEO-007`: Circle rotation rotates the full face, including markings and labels.

## Workspace interaction

- `WRK-001`: Users can add, duplicate, select, and delete circles while the application remains open.
- `WRK-002`: Duplicating a circle copies its mathematical size and current rotation, assigns a new ID, and does not inherit contacts.
- `WRK-003`: Mouse dragging moves a circle and may create a snap on release.
- `WRK-004`: Dragging a circle resets that circle's rotation angle to zero.
- `WRK-005`: Arrow keys move the selected circle without snapping.
- `WRK-006`: Circles may overlap freely without creating a contact or rotation interaction.
- `WRK-007`: The workspace supports zoom around the pointer and panning.
- `WRK-008`: The application window is resizable and maximizable.

## Contact geometry

- `CON-001`: A logical contact exists only when represented by a `ContactState`.
- `CON-002`: Visual closeness or overlap alone is not a logical contact.
- `CON-003`: Contacts may be external or internal tangencies.
- `CON-004`: Drag snapping considers 15-degree angular increments.
- `CON-005`: Fine contact angles may be entered as any finite degree or fractional-degree value.
- `CON-006`: Contact positions may be edited in degrees or circumference units.
- `CON-007`: Editing one representation updates the paired representation.
- `CON-008`: Editing contact coordinates from the selected circle moves only the selected circle; the other circle remains fixed.
- `CON-009`: All contacts for the selected circle are displayed simultaneously in a vertically scrollable inspector.
- `CON-010`: Each logical contact has a visible marker distinct from mere visual proximity.

## Rotation state

- `ROT-001`: A circle is effectively powered, slaved, or stopped.
- `ROT-002`: A powered circle uses its own configured degrees-per-tick rate.
- `ROT-003`: Positive rates rotate clockwise according to the application's screen-coordinate convention; negative rates rotate counter-clockwise.
- `ROT-004`: Rotation rate may be edited in degrees per tick or circumference units per tick.
- `ROT-005`: Rate degree and unit fields remain synchronized.
- `ROT-006`: A non-powered circle may select at most one contact as its incoming slave source.
- `ROT-007`: Selecting a slave input is independent of whether the upstream graph currently resolves to a powered source.
- `ROT-008`: A powered circle ignores incoming slave selection while powered.
- `ROT-009`: Powered circles may still transmit their resulting rate to downstream circles.
- `ROT-010`: A slaved circle may transmit its resulting rate to downstream circles.
- `ROT-011`: Two powered circles may touch without either affecting the other's rate.
- `ROT-012`: A passive contact does not transmit rotation.
- `ROT-013`: External contact reverses inherited angular direction.
- `ROT-014`: Internal contact preserves inherited angular direction.
- `ROT-015`: Radius ratio changes inherited angular rate according to the domain formula.
- `ROT-016`: Effective rates are recalculated from authoritative circle and contact state rather than accumulated as mutable authoritative values.
- `ROT-017`: Detaching and recreating a contact creates a new contact identity; users may explicitly select it as a slave input.
- `ROT-018`: A cycle with no powered source resolves to stopped rather than recursing indefinitely.

## Master clock

- `CLK-001`: Rotation does not advance unless the master clock is running or the tick is explicitly changed.
- `CLK-002`: Clock speed is configurable in ticks per second.
- `CLK-003`: Users may set any future or past integral tick.
- `CLK-004`: Displayed circle angle is deterministic for a given starting state and master tick.
- `CLK-005`: Reset Start restores the saved starting state and master tick zero.

## Persistence

- `PER-001`: The workspace can be saved as JSON.
- `PER-002`: A saved workspace includes authoritative circle state, contacts, tick, and clock speed.
- `PER-003`: Derived solver results are not persisted as authoritative state.
- `PER-004`: Loading replaces the active workspace with the file contents after validation.
- `PER-005`: The bundled `workspace.json` provides an initial state.

## Inspector and fields

- `UI-001`: Numeric fields use standard JavaFX text editing with visible caret and selection.
- `UI-002`: A field commits when Enter is pressed.
- `UI-003`: A changed field also commits when it loses focus.
- `UI-004`: Invalid input shows a clear validation response and does not corrupt model state.
- `UI-005`: Selecting a circle displays its ID, size metrics, current rotation, own/effective rate, drive mode, and contacts.
- `UI-006`: Contact cards expose selected-circle and other-circle degree/unit values.
- `UI-007`: A non-powered circle can choose one contact as its rotation input or clear the choice.
- `UI-008`: Visual circumference styling distinguishes powered, slaved, and stopped states.
- `UI-009`: Inheritance depth may be indicated through a subtle shade variation.
- `UI-010`: Contact graphics distinguish passive contact from active rotation transmission.
