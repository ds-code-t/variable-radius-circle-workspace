# Drop-In Installation

This ZIP is an overlay tailored to `ds-code-t/variable-radius-circle-workspace`.

1. Make sure local work is committed or backed up.
2. Extract the ZIP into the repository root, preserving directories and allowing the included upgraded agent files to replace their older versions.
3. From PowerShell in the repository root, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup-agent.ps1 -StageGit
```

The script:

- generates `docs/agent/repository-index.md`,
- verifies canonical files, adapters, and identical skill copies,
- runs existing agent-document checks,
- runs Gradle project validation,
- stages only the files listed in `scripts/agent_file_manifest.txt` when `-StageGit` is supplied,
- never commits or pushes.

Faster contract/test mode:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup-agent.ps1 -Quick
```

Configure/verify without Gradle tests:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup-agent.ps1 -SkipTests
```

After reviewing the staged changes, commit normally:

```powershell
git commit -m "Add repository-native AI development context"
git push
```

Python 3 and JDK 25 are required for the full setup. The existing Gradle Wrapper supplies Gradle.
