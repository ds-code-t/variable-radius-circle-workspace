# Testing and Verification

## Required automated command

Windows:

```powershell
.\gradlew.bat --no-configuration-cache clean check
```

macOS/Linux:

```bash
./gradlew --no-configuration-cache clean check
```

Do not omit `--no-configuration-cache` for the JavaFX `run` task. `check` may work with cache settings, but using one documented command avoids IDE/environment differences.

## Agent-document verification

Windows:

```powershell
.\scripts\verify-agent-docs.ps1
```

macOS/Linux:

```bash
./scripts/verify-agent-docs.sh
```

To verify only structure without comparing Git changes:

```powershell
.\scripts\verify-agent-docs.ps1 -StructureOnly
```

```bash
./scripts/verify-agent-docs.sh --structure-only
```

## Required solver regression tests

Maintain automated coverage for:

| Requirement | Scenario |
|---|---|
| ROT-002 | Powered circle returns its own positive or negative rate |
| ROT-013 | External child reverses direction |
| ROT-014 | Internal child preserves direction |
| ROT-015 | Unequal radii apply the exact radius ratio |
| ROT-010 | Powered -> child -> grandchild chain |
| ROT-016 | Results do not depend on list or ID order |
| ROT-017 | Detach, recreate contact, select new contact, rotate again |
| ROT-006 | Changing selected slave contact changes the parent source |
| ROT-008 | Powering a formerly slaved circle uses its own rate |
| ROT-018 | Source-free cycle resolves safely to stopped |
| ROT-018 | Invalid/missing contact resolves safely to stopped |
| CLK-004 | Tick jump is deterministic and reversible |

## Geometry tests to add or retain

- radius/diameter/circumference conversion
- degree/unit conversion round trip
- external tangency calculation
- internal tangency calculation
- 15-degree snap candidate generation
- fine fractional-degree contact positioning
- editing selected-circle contact moves only selected circle
- duplicate copies size/rotation but not contacts

## Persistence tests to add or retain

- save/load authoritative state round trip
- older supported JSON sample loads
- malformed references produce clear validation failure or safe normalization
- derived rotation result is not serialized as authoritative state

## UI-focused tests

Use TestFX only if it remains stable and valuable. Otherwise keep controller/field-commit logic outside JavaFX nodes where it can be unit tested, and perform the following manual checks:

1. Numeric field commits on Enter.
2. Numeric field commits when clicking another field.
3. Numeric field commits when clicking a button or circle.
4. Invalid input does not corrupt the last valid value.
5. Caret is visible.
6. Contact list scrolls with many contacts.
7. Detach/reconnect allows selecting the recreated contact and resumes slaving.
8. Circle ID 3 or any later-added circle can be powered and slaved identically to lower IDs.
9. Maximizing and resizing preserve usable layout.
10. Save, modify, reset, load, and negative tick workflows remain deterministic.

## Verification reporting

Update `.ai/CURRENT_STATE.md` with:

- exact command run
- date
- JDK version
- operating system when known
- pass/fail result
- any unexecuted checks and why

Never describe a compile-only check as a full test pass.
