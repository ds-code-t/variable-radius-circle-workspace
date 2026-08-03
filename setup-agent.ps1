[CmdletBinding()]
param(
    [switch]$Quick,
    [switch]$SkipTests,
    [switch]$StageGit
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$RepositoryRoot = $PSScriptRoot

if (-not (Test-Path -LiteralPath (Join-Path $RepositoryRoot "build.gradle.kts") -PathType Leaf) -or
    -not (Test-Path -LiteralPath (Join-Path $RepositoryRoot "src\main\java\com\example\circleworkspace") -PathType Container)) {
    throw "Run setup-agent.ps1 from the Variable Radius Circle Workspace repository root."
}

$PythonExe = $null
$PythonPrefix = @()
if (Get-Command python -ErrorAction SilentlyContinue) {
    $PythonExe = "python"
}
elseif (Get-Command py -ErrorAction SilentlyContinue) {
    $PythonExe = "py"
    $PythonPrefix = @("-3")
}
else {
    throw "Python 3 is required."
}

function Invoke-Python {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)
    $CommandArgs = @($script:PythonPrefix) + @($Arguments)
    & $script:PythonExe @CommandArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Python command failed: $($Arguments -join ' ')"
    }
}

Push-Location $RepositoryRoot
try {
    Invoke-Python scripts/refresh_agent_index.py
    Invoke-Python scripts/verify_agent_contract.py
    Invoke-Python scripts/refresh_agent_index.py --check

    if (-not $SkipTests) {
        if ($Quick) {
            & .\scripts\agent_validate.ps1 -Quick
        }
        else {
            & .\scripts\agent_validate.ps1
        }
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }

    if ($StageGit) {
        if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
            throw "Git was not found; setup succeeded but files could not be staged."
        }
        git rev-parse --is-inside-work-tree | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "This folder is not a Git work tree."
        }
        $AgentFiles = Get-Content -LiteralPath ".\scripts\agent_file_manifest.txt" |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and -not $_.TrimStart().StartsWith("#") }
        git add -- @AgentFiles
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        Write-Host "AI-DLC files staged. No commit or push was performed."
    }

    Write-Host "AI-DLC setup complete."
}
finally {
    Pop-Location
}
