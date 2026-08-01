[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$forbiddenFilePattern = '(^|/)(bin|target|upload|logs?|runtime)(/|$)|(^|/)\.env$|\.(log|class|pid|hprof|bak|dump)$|\.sql\.gz$'
$sensitivePropertyPattern = '^\s*(?<key>spring\.mail\.(username|password)|paypal\.client\.(app|secret)|spring\.datasource\.password)\s*=\s*(?<value>.*)\s*$'
$sensitiveEnvironmentPattern = '^\s*(?<key>MAIL_USERNAME|MAIL_PASSWORD|PAYPAL_CLIENT_ID|PAYPAL_CLIENT_SECRET|ADMIN_BOOTSTRAP_PASSWORD)\s*=\s*(?<value>.*)\s*$'
$findings = @()

Push-Location $repositoryRoot

try {
    $trackedFiles = & git ls-files
    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect tracked repository files."
    }

    $forbiddenFiles = @($trackedFiles | Where-Object { $_ -match $forbiddenFilePattern })
    foreach ($forbiddenFile in $forbiddenFiles) {
        $findings += "Forbidden tracked file: $forbiddenFile"
    }

    $propertyFiles = Get-ChildItem -Path "src\main\resources" -Filter "application*.properties" -File
    foreach ($propertyFile in $propertyFiles) {
        $lineNumber = 0
        foreach ($line in Get-Content -LiteralPath $propertyFile.FullName) {
            $lineNumber++
            if ($line -match $sensitivePropertyPattern) {
                $value = $Matches.value.Trim()
                if (-not [string]::IsNullOrWhiteSpace($value) -and -not $value.StartsWith('${')) {
                    $relativePath = $propertyFile.FullName.Substring($repositoryRoot.Length).TrimStart('\', '/')
                    $findings += "$relativePath`:$lineNumber contains a literal value for $($Matches.key)"
                }
            }
        }
    }

    if (Test-Path -LiteralPath ".env.example") {
        $lineNumber = 0
        foreach ($line in Get-Content -LiteralPath ".env.example") {
            $lineNumber++
            if ($line -match $sensitiveEnvironmentPattern -and -not [string]::IsNullOrWhiteSpace($Matches.value)) {
                $findings += ".env.example`:$lineNumber contains a literal value for $($Matches.key)"
            }
        }
    }

    if ($findings.Count -gt 0) {
        $findings | ForEach-Object { Write-Error $_ }
        throw "Repository contains runtime data, a local secret file, or a literal credential."
    }

    Write-Host "Repository hygiene checks passed." -ForegroundColor Green
}
finally {
    Pop-Location
}
