[CmdletBinding()]
param(
    [switch]$StructureOnly,
    [string]$BaseRef = ""
)

$ErrorActionPreference = "Stop"

function Invoke-GitTest {
    param([Parameter(Mandatory)][string[]]$Arguments)
    & git @Arguments *> $null
    return $LASTEXITCODE -eq 0
}

function Get-GitLines {
    param([Parameter(Mandatory)][string[]]$Arguments)
    $output = & git @Arguments 2>$null
    if ($LASTEXITCODE -ne 0) { return @() }
    return @($output | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location -LiteralPath $ProjectRoot

$RequiredFiles = @(
    "AGENTS.md",
    "CLAUDE.md",
    "GEMINI.md",
    ".github/copilot-instructions.md",
    ".github/workflows/agent-context.yml",
    ".cursor/rules/project.mdc",
    ".ai/PROJECT.md",
    ".ai/REQUIREMENTS.md",
    ".ai/DOMAIN_RULES.md",
    ".ai/ARCHITECTURE.md",
    ".ai/UI_BEHAVIOR.md",
    ".ai/TESTING.md",
    ".ai/CURRENT_STATE.md",
    ".ai/CHANGELOG.md",
    ".ai/UPDATE_PROTOCOL.md",
    ".ai/decisions/README.md",
    ".ai/decisions/0001-use-java-25-and-javafx-25.md",
    ".ai/decisions/0002-single-source-for-rotation-input.md",
    ".ai/decisions/0003-deterministic-rotation-graph.md",
    ".ai/decisions/0004-separate-contact-from-transmission.md",
    "scripts/verify-agent-docs.ps1",
    "scripts/verify-agent-docs.sh"
)

$MissingFiles = @($RequiredFiles | Where-Object {
    -not (Test-Path -LiteralPath (Join-Path $ProjectRoot $_) -PathType Leaf)
})

if ($MissingFiles.Count -gt 0) {
    throw "Missing required agent-context files:`n - $($MissingFiles -join "`n - ")"
}

$AgentsText = Get-Content -LiteralPath "AGENTS.md" -Raw
$RequiredReferences = @(
    ".ai/PROJECT.md",
    ".ai/REQUIREMENTS.md",
    ".ai/DOMAIN_RULES.md",
    ".ai/ARCHITECTURE.md",
    ".ai/UI_BEHAVIOR.md",
    ".ai/CURRENT_STATE.md",
    ".ai/TESTING.md",
    ".ai/UPDATE_PROTOCOL.md"
)
foreach ($Reference in $RequiredReferences) {
    if (-not $AgentsText.Contains($Reference)) {
        throw "AGENTS.md does not reference $Reference"
    }
}

$RequirementsText = Get-Content -LiteralPath ".ai/REQUIREMENTS.md" -Raw
foreach ($Prefix in @("GEO-", "WRK-", "CON-", "ROT-", "CLK-", "PER-", "UI-")) {
    if (-not $RequirementsText.Contains($Prefix)) {
        throw "REQUIREMENTS.md is missing requirement family $Prefix"
    }
}

Write-Host "Agent-context structure is valid." -ForegroundColor Green
if ($StructureOnly) { exit 0 }

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Warning "Git is unavailable; change-set checks were skipped."
    exit 0
}
if (-not (Invoke-GitTest -Arguments @("rev-parse", "--is-inside-work-tree"))) {
    Write-Warning "Not inside a Git work tree; change-set checks were skipped."
    exit 0
}

if ([string]::IsNullOrWhiteSpace($BaseRef)) {
    if (-not [string]::IsNullOrWhiteSpace($env:AGENT_DOCS_BASE_REF)) {
        $BaseRef = $env:AGENT_DOCS_BASE_REF
    } elseif (Invoke-GitTest -Arguments @("rev-parse", "--verify", "HEAD~1")) {
        $BaseRef = "HEAD~1"
    }
}

$ChangedFiles = @()
if (-not [string]::IsNullOrWhiteSpace($BaseRef)) {
    $ChangedFiles += Get-GitLines -Arguments @("diff", "--name-only", "$BaseRef...HEAD")
}
$ChangedFiles += Get-GitLines -Arguments @("diff", "--cached", "--name-only")
$ChangedFiles += Get-GitLines -Arguments @("diff", "--name-only")
$ChangedFiles += Get-GitLines -Arguments @("ls-files", "--others", "--exclude-standard")
$ChangedFiles = @($ChangedFiles | ForEach-Object { $_.Trim().Replace("\", "/") } | Sort-Object -Unique)

$CodeChanged = @($ChangedFiles | Where-Object {
    $_ -like "src/main/*" -or
    $_ -eq "build.gradle.kts" -or
    $_ -eq "settings.gradle.kts" -or
    $_ -eq "gradle.properties" -or
    $_ -like "gradle/*"
})

if ($CodeChanged.Count -gt 0) {
    foreach ($RequiredDoc in @(".ai/CURRENT_STATE.md", ".ai/CHANGELOG.md")) {
        if ($ChangedFiles -notcontains $RequiredDoc) {
            throw "Production/build code changed, but $RequiredDoc was not updated."
        }
    }
    if (-not ($ChangedFiles | Where-Object { $_ -like ".ai/*.md" -or $_ -like ".ai/decisions/*.md" })) {
        throw "Production/build code changed, but no canonical .ai document was updated."
    }
}

Write-Host "Agent-context verification passed." -ForegroundColor Green
