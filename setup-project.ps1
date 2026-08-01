[CmdletBinding()]
param(
    [switch]$NoRun,
    [switch]$NoGit
)

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot

function Write-Step {
    param([string]$Message)

    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Stop-Setup {
    param([string]$Message)

    Write-Host ""
    Write-Host "SETUP FAILED: $Message" -ForegroundColor Red
    exit 1
}

function Invoke-NativeCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Command,

        [string[]]$Arguments = @(),

        [switch]$AllowFailure
    )

    & $Command @Arguments
    $ExitCode = $LASTEXITCODE

    if (-not $AllowFailure -and $ExitCode -ne 0) {
        Stop-Setup "$Command failed with exit code $ExitCode."
    }

    return $ExitCode
}

Set-Location -LiteralPath $ProjectRoot

Write-Step "Validating project files"

$RequiredFiles = @(
    "build.gradle.kts",
    "settings.gradle.kts",
    "gradlew",
    "gradlew.bat",
    "AGENTS.md",
    ".ai\PROJECT.md",
    ".ai\REQUIREMENTS.md",
    ".ai\DOMAIN_RULES.md",
    ".ai\ARCHITECTURE.md",
    ".ai\CURRENT_STATE.md",
    ".ai\CHANGELOG.md",
    "scripts\verify-agent-docs.ps1"
)

$MissingFiles = @()

foreach ($RelativePath in $RequiredFiles) {
    $FullPath = Join-Path $ProjectRoot $RelativePath

    if (-not (Test-Path -LiteralPath $FullPath -PathType Leaf)) {
        $MissingFiles += $RelativePath
    }
}

if ($MissingFiles.Count -gt 0) {
    Write-Host "Missing required files:" -ForegroundColor Red

    foreach ($MissingFile in $MissingFiles) {
        Write-Host "  - $MissingFile" -ForegroundColor Red
    }

    Stop-Setup "The project folder is incomplete."
}

Write-Step "Checking Java 25"

$JavaCommand = Get-Command java -ErrorAction SilentlyContinue

if (-not $JavaCommand) {
    Stop-Setup "Java was not found on PATH. Install JDK 25 and configure JAVA_HOME and PATH."
}

$PreviousErrorActionPreference = $ErrorActionPreference

try {
    # java -version normally writes its output to stderr.
    $ErrorActionPreference = "Continue"
    $VersionOutput = (& java -version 2>&1 | Out-String)
    $JavaExitCode = $LASTEXITCODE
}
finally {
    $ErrorActionPreference = $PreviousErrorActionPreference
}

if ($JavaExitCode -ne 0) {
    Stop-Setup "The java command failed with exit code $JavaExitCode."
}

if ($VersionOutput -notmatch 'version\s+"25(?:\.|")') {
    Write-Host $VersionOutput
    Stop-Setup "This project requires JDK 25."
}

Write-Host ($VersionOutput.Trim()) -ForegroundColor Green

Write-Step "Validating agent documentation"

& powershell `
    -NoProfile `
    -ExecutionPolicy Bypass `
    -File (Join-Path $ProjectRoot "scripts\verify-agent-docs.ps1") `
    -StructureOnly

if ($LASTEXITCODE -ne 0) {
    Stop-Setup "Agent-documentation validation failed."
}

Write-Step "Downloading dependencies and running tests"

& (Join-Path $ProjectRoot "gradlew.bat") `
    --no-configuration-cache `
    clean `
    test

if ($LASTEXITCODE -ne 0) {
    Stop-Setup "Gradle tests failed."
}

if (-not $NoGit) {
    Write-Step "Preparing Git"

    $GitCommand = Get-Command git -ErrorAction SilentlyContinue

    if (-not $GitCommand) {
        Write-Host "Git was not found. Skipping repository initialization and staging." `
            -ForegroundColor Yellow
    }
    else {
        # Avoid invoking git rev-parse outside a repository because PowerShell
        # may interpret Git's normal stderr output as an error.
        $GitDirectory = Join-Path $ProjectRoot ".git"

        if (-not (Test-Path -LiteralPath $GitDirectory -PathType Container)) {
            Write-Host "Initializing a new Git repository..."
            Invoke-NativeCommand -Command "git" -Arguments @("init") | Out-Null
        }
        else {
            Write-Host "Existing Git repository detected."
        }

        Invoke-NativeCommand -Command "git" -Arguments @(
            "add",
            "--all"
        ) | Out-Null

        Write-Host "Project files have been staged in Git." -ForegroundColor Green
        Write-Host "Review them with: git status"
        Write-Host 'Commit them with: git commit -m "Initialize circle workspace"'
    }
}

if (-not $NoRun) {
    Write-Step "Launching application"

    & (Join-Path $ProjectRoot "gradlew.bat") `
        --no-configuration-cache `
        run

    if ($LASTEXITCODE -ne 0) {
        Stop-Setup "The application failed to launch."
    }
}
else {
    Write-Host ""
    Write-Host "Setup completed without launching the application." `
        -ForegroundColor Green
}

Write-Host ""
Write-Host "Setup completed successfully." -ForegroundColor Green