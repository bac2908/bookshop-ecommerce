param(
    [switch]$SkipBuild,
    [ValidateSet('Ampps', 'Docker')]
    [string]$Database = 'Ampps'
)

$ErrorActionPreference = 'Stop'

$adoptiumRoot = 'C:\Program Files\Eclipse Adoptium'
$jdkCandidates = @()
if (Test-Path -LiteralPath $adoptiumRoot) {
    $jdkCandidates += Get-ChildItem -LiteralPath $adoptiumRoot -Directory -Filter 'jdk-17*' |
        Sort-Object Name -Descending |
        Select-Object -ExpandProperty FullName
}
$jdkCandidates += 'C:\Program Files\Java\jdk-17'

$selectedJdk = $jdkCandidates | Where-Object {
    Test-Path -LiteralPath (Join-Path $_ 'bin\java.exe')
} | Select-Object -First 1

if (-not $selectedJdk) {
    throw 'Cần cài JDK 17. Có thể dùng: winget install EclipseAdoptium.Temurin.17.JDK'
}

$env:JAVA_HOME = $selectedJdk
$env:Path = "$selectedJdk\bin;$env:Path"
$env:SPRING_PROFILES_ACTIVE = 'dev'

if ($Database -eq 'Docker') {
    $env:DB_URL = 'jdbc:mysql://localhost:3308/bookshop?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8'
    $env:DB_USERNAME = 'bookshop'
    $env:DB_PASSWORD = 'bookshop_dev'
}
else {
    $amppsIniPath = 'C:\Program Files\Ampps\mysql\my.ini'
    if (-not (Test-Path -LiteralPath $amppsIniPath)) {
        throw "Không tìm thấy cấu hình AMPPS MySQL tại: $amppsIniPath"
    }

    $insideClientSection = $false
    $databasePassword = $null
    foreach ($iniLine in Get-Content -LiteralPath $amppsIniPath) {
        if ($iniLine -match '^\s*\[(.+)\]') {
            $insideClientSection = ($matches[1] -match '^client$')
        }
        elseif ($insideClientSection -and $iniLine -match '^\s*password\s*=\s*(.*)$') {
            $databasePassword = $matches[1].Trim()
            break
        }
    }

    if ([string]::IsNullOrWhiteSpace($databasePassword)) {
        throw 'Không đọc được mật khẩu MySQL từ AMPPS.'
    }

    $env:DB_URL = 'jdbc:mysql://localhost:3306/bookshop?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8'
    $env:DB_USERNAME = 'root'
    $env:DB_PASSWORD = $databasePassword
}

if (-not $SkipBuild) {
    & "$PSScriptRoot\mvnw.cmd" clean package
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

& "$selectedJdk\bin\java.exe" -jar "$PSScriptRoot\target\CT07-1.0.jar"
exit $LASTEXITCODE
