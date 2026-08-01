# Start Here

1. Install JDK 25 and ensure `java -version` reports Java 25.
2. Open PowerShell in this project folder.
3. Run exactly:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup-project.ps1
```

That one script validates the project, checks Java 25, validates the agent-context files, downloads Gradle/dependencies, runs tests, initializes/stages Git when available, and launches the application.

To set up without launching:

```powershell
powershell -ExecutionPolicy Bypass -File .\setup-project.ps1 -NoRun
```
