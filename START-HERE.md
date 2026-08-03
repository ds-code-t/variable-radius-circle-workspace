# Start Here

## Run the project

1. Install JDK 25 and ensure `java -version` reports Java 25.
2. Open PowerShell in this project folder.
3. Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup-project.ps1
```

To set up without launching:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup-project.ps1 -NoRun
```

## Initialize or validate the AI-DLC repository context

After adding the AI-DLC files, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup-agent.ps1 -StageGit
```

This generates the repository index, verifies all agent adapters and living-context files, runs project validation, and stages the shared files. It never commits or pushes.

Agents should start with `AGENTS.md`; humans can read `AI-DLC.md` for how the framework works.
