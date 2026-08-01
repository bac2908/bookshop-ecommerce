[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repositoryRoot

try {
    & "$PSScriptRoot\check-repository.ps1"
    if ($LASTEXITCODE -ne 0) {
        throw "Repository hygiene checks failed."
    }

    & .\mvnw.cmd --batch-mode --no-transfer-progress clean verify
    if ($LASTEXITCODE -ne 0) {
        throw "Maven quality gates failed."
    }

    Write-Host "All repository checks passed." -ForegroundColor Green
}
finally {
    Pop-Location
}
