# General geometric workspace with rotation-following contact

Copy this ZIP over the repository root, preserving paths. It supersedes the earlier generalized-shape drop-in.

## Contact-following rule

The authoritative `slaveContactId` now drives both inherited rotation and derived center placement. The driver remains fixed. The driven center stays on the contact's stored driver-to-driven bearing, moving only toward or away from the driver. Chained contacts are evaluated in rotation-graph depth order.

External contacts expose two modes:

- `FIXED_CONTACT_ALLOW_OVERLAP`: center distance is the sum of the two boundary radii evaluated on the original contact ray. Other protrusions may overlap and do not change the retained contact ray.
- `NO_OVERLAP_SWITCH_CONTACT`: center distance is the sum of the two outward support distances on that ray. Supporting protrusions can change during rotation, preventing overlap while the driven shape moves radially.

Circle calculations are exact. Radial support is a deterministic sampled convex-hull approximation using 720–16,384 angular samples. It guarantees a separating support plane but may be conservative for deeply concave profiles.

Internal tangency uses the fixed-ray containment rule; the mode selector is disabled for internal contacts.

## Validation

Run after extraction:

```powershell
.\gradlew.bat clean check
.\scripts\agent_validate.ps1
```

or:

```bash
./gradlew clean check
./scripts/agent_validate.sh
```

This package was produced with read-only GitHub access and was not compiled or launched here. Canonical `.ai` documents and the generated repository index were not overwritten with unverified generated content.

## Individual radial editing and contact-following performance update

The radial-shape inspector now includes an **Individual radii** text area. Enter one
positive radius per line, or separate values with commas, semicolons, or spaces,
then select **Apply radii**. Applying the list changes the radial sample count to
the number of entered values.

The no-overlap contact follower no longer scans 720–16,384 arbitrary boundary
samples per shape on every tick. It evaluates each piecewise-linear polar profile
segment directly, checking its endpoints and a bounded Newton solve for an
interior support maximum. Circle support remains exact. Radial support remains a
deterministic numerical calculation over the documented linear radial
interpolation.

Tick changes redraw the canvas without rebuilding the complete inspector.


## Contact-motion correction

This revision fixes an angular-coordinate mismatch in contact following. Stored
center-line bearings use screen coordinates (`0° = right`, `90° = down`), while
shape boundary evaluation uses the rendering convention (`0° = up`, `90° =
right`). Contact rays are now converted before boundary/support evaluation.

The animation clock also advances by the exact number of elapsed logical ticks
and resets its baseline while paused, avoiding frame-rate-dependent bursts.


## Circumference-line distribution

Each shape now stores a `DivisionDistribution`:

- `EQUAL_LENGTH` — default. Lines are placed at equal cumulative boundary-length
  fractions using a deterministic cached polyline arc-length table.
- `EQUAL_ANGLE` — lines remain at `2πi/N`.

Radial control samples remain evenly distributed by angle and are independent
of circumference-line distribution.

When rotation rate uses `DIVISIONS_PER_TICK`, powered radial shapes advance
through the selected line distribution. Equal-angle mode retains `q × 360/N`.
Equal-length mode advances by `q/N` of the perimeter and maps that cumulative
boundary progress back to angular orientation. Degree mode is unchanged.
Circles produce identical motion in either distribution mode.

The perimeter inverse is numerical for radial shapes and uses a deterministic
cached table with at least 256 samples and 32 samples per radial control point.


## Rolling no-overlap contact correction

`NO_OVERLAP_SWITCH_CONTACT` no longer uses the shapes' furthest axial
projections unconditionally. That rule moved a driven shape away before a
protrusion actually touched it and could open a visible gap.

The corrected rule is:

1. Compute the retained-ray perimeter contact used by overlap-allowed mode.
2. Keep that separation while the approximated closed boundaries have no
   positive-area overlap.
3. When overlap first occurs, search outward only along the stored contact line
   for the nearest non-overlapping configuration.
4. That blocking boundary contact becomes the effective rolling contact until
   the retained-ray contact is valid again or another protrusion blocks it.

Boundary-overlap detection uses deterministic polygonal approximations derived
from the mathematical radial boundary. Circles use 128 segments; radial shapes
use eight segments per radial control interval, bounded to 96–16,384 segments.
The final switching separation is found with 28 bisection steps. This is a
numerical contact approximation, not an exact arbitrary-curve rolling solver.


## Global circumference-line length

`EQUAL_LENGTH` now means a true workspace-wide physical unit rather than merely
equal fractions of each shape's current perimeter.

The workspace stores one positive `globalMarkerLength`. For every shape using
`EQUAL_LENGTH`:

```text
target perimeter = circumference division count × global marker length
```

Changing the circumference division count uniformly rescales that shape to the
new target perimeter. Circles change radius. Radial shapes change base radius,
which scales every individual radius by the same factor and therefore preserves
the complete radial profile and all radius-to-radius proportions.

Changing the global marker length rescales every equal-length shape. Shapes
using `EQUAL_ANGLE` are not resized. Base-radius editing is disabled while a
shape uses equal-length distribution because size is then derived from the
global unit and division count. Direct radial-list, variation, and sample-count
edits remain available; after such an edit the profile is uniformly normalized
back to the required perimeter.

Persistence schema version 3 adds:

```json
"globalMarkerLength": 10.0
```

Older workspaces use `10.0` when the field is absent.


## Workspace pan and zoom restoration

This revision restores the viewport camera that was lost when the controller was
generalized.

- Mouse wheel zooms from `0.1×` to `8×`.
- Zoom is anchored at the pointer, so the world point under the pointer remains
  stationary.
- Primary-button dragging on empty workspace pans.
- Middle- or right-button dragging pans from anywhere.
- `Reset view` restores identity zoom and pan.
- Palette drops, click-to-add placement, hit testing, shape dragging, and
  visual snap tolerance all convert through the same camera transform.
- Camera state is session-only. Shape positions, contact bearings, persistence,
  and geometry calculations remain in unchanged world coordinates.


## Numbered circumference lines and rotation telemetry

Circumference markers are numbered from `1` through the configured division
count. Labels are adaptively thinned only when their projected screen spacing
would overlap; zooming in reveals the intervening numbers.

The selected-shape inspector reports:

- workspace ticks since start;
- angular movement for the current tick;
- cumulative signed degrees since tick zero;
- cumulative signed rotations and completed full rotations.

Telemetry is derived from the rotation solver and incrementally cached. It is
not additional persisted rotation state.

Perimeters, selection outlines, contact lines, circumference lines, marker
lengths, and text use inverse-zoom screen sizing. Zooming in therefore reveals
more geometric detail without making strokes or labels excessively thick.


## Circumference units and line divisions

The circumference control now represents **circumference unit points**, not the
number of visible lines. The closing endpoint coincides with the starting point,
so:

```text
line divisions = circumference units - 1
target perimeter = line divisions × global line length
```

For example, `7` circumference units produces `6` visible numbered line
divisions and an equal-length target perimeter of `6 × global line length`.

`DIVISIONS_PER_TICK` also uses the derived line-division count. Thus one line
per tick on a shape with seven circumference units advances by one sixth of its
perimeter. Degree-per-tick mode is unchanged.

Persistence schema version 4 writes `circumferenceUnits`. Versions 1–3 that
stored `divisions` are migrated by adding one, preserving their previous visible
line count and rotation-per-line behavior.


## Per-shape telemetry and pointer interaction

Every shape now renders its own cumulative rotation telemetry without requiring
selection:

- current workspace tick,
- total signed degrees rotated since tick zero,
- total signed revolutions,
- completed signed full revolutions.

The values use the same incremental `RotationTelemetry` cache as the inspector
and include powered and inherited/slaved rotation.

New external contacts and legacy contacts without an explicit `followMode`
default to `NO_OVERLAP_SWITCH_CONTACT`. Explicitly persisted contact modes are
preserved.

Pointer behavior is now:

- right-click a shape: select it and show its configuration without moving or
  detaching it;
- left-press and drag a shape: detach only after a four-pixel drag threshold,
  then move and snap it;
- left-drag empty workspace: pan;
- middle-drag: pan;
- a simple click on a shape never breaks its contacts.


## Rotation-total accounting correction

Cumulative rotation is now event-based rather than retrospectively rebuilt.
Changing a rate, radial profile, contact, selection-related state, or coupling
at tick T preserves all rotation accrued through tick T. The new configuration
affects only subsequent ticks. Adding a shape starts that shape at zero without
resetting existing shapes; deleting a shape does not alter surviving totals.
Rewinding the workspace tick still intentionally recomputes from tick zero
using the currently loaded configuration because prior edit history is not
persisted.
