[CmdletBinding()]
param(
    [switch]$Quick,
    [string]$BaseRef
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$RepositoryRoot = Split-Path -Parent $PSScriptRoot

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
    throw "Python 3 is required for agent contract validation."
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
    $VerifyArgs = @("scripts/verify_agent_contract.py")
    if (-not [string]::IsNullOrWhiteSpace($BaseRef)) {
        $VerifyArgs += @("--base-ref", $BaseRef)
    }
    Invoke-Python @VerifyArgs
    Invoke-Python scripts/refresh_agent_index.py --check

    if (Test-Path -LiteralPath ".\scripts\verify-agent-docs.ps1" -PathType Leaf) {
        & .\scripts\verify-agent-docs.ps1
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }

    if ($Quick) {
        & .\gradlew.bat test
    }
    else {
        & .\gradlew.bat clean check
    }
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    $Mode = if ($Quick) { "quick" } else { "full" }
    Write-Host "Variable Radius Circle Workspace validation completed ($Mode mode)."
}
finally {
    Pop-Location
}
