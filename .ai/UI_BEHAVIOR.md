# UI Behavior

## General layout

- The application uses one resizable and maximizable JavaFX window.
- The workspace occupies the main area.
- The selected-circle inspector remains visible at the side.
- Contact details appear as a vertical list in a scrollable region.
- Do not reintroduce overlapping previous/next contact arrows.

## Selection

- Clicking a circle selects it.
- The inspector updates to that circle's current authoritative and derived state.
- Clicking empty workspace may clear selection unless the click is part of panning.

## Text fields

- Use standard JavaFX `TextField` controls.
- Caret and selection must remain visible.
- Enter commits the edited value.
- Losing focus commits a changed value before the newly focused action proceeds.
- Avoid feedback loops when programmatically refreshing paired fields.
- Invalid text must not replace the last valid model value.
- After rejection, restore or clearly display the valid value and show a concise error.

## Paired measurements

The following are synchronized pairs/groups:

- radius / diameter / circumference
- rotation degrees / rotation circumference units
- own rate degrees per tick / own rate circumference units per tick
- each contact's degree / circumference-unit value

The field the user edits is the input representation. Other fields are derived and refreshed after commit.

## Dragging and movement

- Dragging a circle resets its rotation angle to zero and detaches contacts according to the product rules.
- Drag release may snap to internal or external tangency using 15-degree candidates.
- Arrow-key movement does not snap.
- Moving through arrow keys detaches contacts that no longer remain authoritative.
- Overlap without an explicit contact is allowed.

## Contact cards

Each contact card for the selected circle should show:

- contact ID
- other circle ID
- internal/external type
- selected-circle touch degrees
- selected-circle touch units
- other-circle touch degrees
- other-circle touch units
- whether this contact is the selected rotation input
- controls to select/clear the rotation input where allowed
- a break/remove action

All contact cards are visible in one vertical list; the region scrolls when necessary.

## Rotation controls

- Powered toggle and own-rate fields are editable for the selected circle.
- A powered circle's incoming slave selection is ignored while power is on.
- A slaved circle's own-rate fields may remain visible but must clearly distinguish configured own rate from current effective inherited rate.
- Effective mode, rate, source, and depth are display values from the solver.
- State colors must distinguish powered, slaved, and stopped circles.
- A subtle shade variation may indicate inheritance depth, but color must not be the only cue.

## Contact graphics

- Every logical contact has a visible touch-point marker.
- Passive and transmitting contacts use different marker styling.
- Mere visual closeness has no marker.

## Clock controls

- Start/stop master clock.
- Set ticks per second.
- Set an exact positive, zero, or negative tick.
- Provide reset to saved start/tick zero.
- Field commits follow the same Enter/focus-loss rules.
