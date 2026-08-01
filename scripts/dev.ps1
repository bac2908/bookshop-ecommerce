[CmdletBinding()]
param(
    [switch]$NoBuild
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repositoryRoot

try {
    $composeArguments = @("compose", "up", "--detach")
    if (-not $NoBuild) {
        $composeArguments += "--build"
    }

    & docker @composeArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose could not start the project."
    }

    $portLine = @(& docker compose port app 8080) | Select-Object -First 1
    if ($LASTEXITCODE -ne 0 -or $portLine -notmatch ":(?<port>\d+)$") {
        throw "Could not determine the published application port."
    }

    $applicationUrl = "http://localhost:$($Matches.port)"
    $deadline = (Get-Date).AddSeconds(90)
    $ready = $false

    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $applicationUrl -UseBasicParsing -TimeoutSec 5
            if ($response.StatusCode -eq 200) {
                $ready = $true
                break
            }
        }
        catch {
            Start-Sleep -Seconds 2
        }
    }

    if (-not $ready) {
        & docker compose logs --tail 100 app
        throw "The application did not become ready within 90 seconds."
    }

    Write-Host "Bookshop is ready: $applicationUrl" -ForegroundColor Green
    Write-Host "Swagger UI: $applicationUrl/swagger-ui.html"
}
finally {
    Pop-Location
}
