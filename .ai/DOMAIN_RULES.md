# Domain Rules and Mathematics

## Authoritative versus derived state

Authoritative circle state:

- ID
- center position
- radius
- starting angle in degrees
- powered toggle
- own rate in degrees per tick
- selected `slaveContactId`

Authoritative contact state:

- contact ID
- two participating circle IDs
- internal or external tangency type
- touch angle for each circle

Derived state:

- diameter
- circumference
- contact-unit values
- effective rotation rate
- inheritance depth
- effective drive mode
- source circle
- displayed angle at the current tick

Derived values must be recalculated. They must not become competing sources of truth.

## Circle measurements

For radius `r`:

```text
diameter = 2r
circumference = 2πr
```

For circumference `c`:

```text
radius = c / 2π
```

## Degree and circumference-unit conversion

A circumference-unit position `u` is a linear distance measured around a circle's circumference `c`.

```text
degrees = 360 × u / c
units   = c × degrees / 360
```

Angles should be normalized for display when useful, but calculations may preserve unbounded values where doing so maintains deterministic time behavior.

## Contact existence

A pair of circles is logically touching only if a valid `ContactState` exists. Geometric overlap, equality of circumferences, or visual proximity does not imply contact.

## Tangency

Let circles A and B have centers `CA`, `CB` and radii `rA`, `rB`.

External tangency requires:

```text
|CB - CA| = rA + rB
```

Internal tangency requires:

```text
|CB - CA| = |rA - rB|
```

Contact-angle edits from a selected circle must reposition only that selected circle while keeping the other endpoint fixed.

## Rotation modes

A circle resolves to exactly one effective mode:

1. `POWERED`: `powered == true`; use its own configured rate.
2. `SLAVED`: not powered, has a valid selected contact, and the upstream circle resolves to a non-stopped source.
3. `STOPPED`: neither of the above.

`slaveContactId` is the sole authoritative incoming-source selection. Do not create another boolean that must agree with it.

## Rotation propagation

Let:

- `ωp` = parent effective angular rate in degrees per tick
- `rp` = parent radius
- `rc` = child radius

External tangency:

```text
ωc = -ωp × rp / rc
```

Internal tangency:

```text
ωc =  ωp × rp / rc
```

A slaved result records inheritance depth as parent depth plus one.

## Deterministic displayed angle

For a circle's starting angle `θ0`, effective rate `ω`, and master tick `t`:

```text
θ(t) = θ0 + ωt
```

Changing power, own rate, or selected slave source at a nonzero tick should preserve the visible angle unless the user explicitly edits the angle or performs an operation whose requirement resets it. Preserve the visible angle by rebasing `θ0` against the newly resolved rate.

## Graph resolution

- Rebuild solver results from current authoritative state after every relevant edit.
- Memoize within one solve operation only.
- Detect cycles by the current recursion path or an equivalent graph algorithm.
- A source-free cycle resolves to stopped.
- Missing circles, missing contacts, or a selected contact that does not involve the circle resolve safely to stopped.
- Solver results must not depend on circle ID ordering or list insertion order.

## Contact transmission

Contact and transmission are separate:

- A contact may exist and remain passive.
- A non-powered circle transmits incoming rotation only through the contact selected by its own `slaveContactId`.
- The same contact may be passive from one circle's perspective and selected by the other.
- Powered circles do not accept incoming rates, but their contacts remain visible and may be used by other circles.
