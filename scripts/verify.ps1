param(
    [int]$Port = 18080,
    [switch]$SkipBuild,
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$MavenCommand = ''
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $root 'target\studyforge-ai-0.1.0-SNAPSHOT.jar'
$proc = $null

try {
    if (-not $JavaHome) {
        $knownJava21 = Join-Path $env:USERPROFILE 'Downloads\OpenJDK21U-jdk_x64_windows_hotspot_21.0.12.1_1\jdk-21.0.12.1+1'
        if (Test-Path (Join-Path $knownJava21 'bin\java.exe')) { $JavaHome = $knownJava21 }
    }
    if ($JavaHome) { $env:JAVA_HOME = $JavaHome; $env:Path = "$JavaHome\bin;$env:Path" }
    $windowsHost = ($env:OS -eq 'Windows_NT') -or ($PSVersionTable.Platform -eq 'Win32NT')
    if ($windowsHost) {
        $javaVersion = (& cmd /c 'java -version 2>&1' | Select-Object -First 1).ToString()
    } else {
        $javaVersion = (& java -version 2>&1 | Select-Object -First 1).ToString()
    }
    if ($javaVersion -notmatch 'version "21\.') { throw "Java 21 is required; detected $javaVersion" }
    if (-not $MavenCommand) {
        $daemon = Get-Command mvnd -ErrorAction SilentlyContinue
        if ($daemon) {
            $MavenCommand = 'mvnd'
        } else {
            $knownMvnd = Join-Path $env:USERPROFILE 'Downloads\maven-mvnd-1.0.6-windows-amd64\maven-mvnd-1.0.6-windows-amd64\bin\mvnd.cmd'
            $MavenCommand = if (Test-Path $knownMvnd) { $knownMvnd } else { 'mvn' }
        }
    }
    if (-not $SkipBuild) {
        Push-Location $root
        try { & $MavenCommand -B clean package } finally { Pop-Location }
    }
    if (-not (Test-Path $jar)) { throw "Packaged JAR was not found: $jar" }
    $proc = Start-Process -FilePath 'java' -ArgumentList @('-jar', $jar, "--server.port=$Port") -WorkingDirectory $root -PassThru -WindowStyle Hidden
    $base = "http://localhost:$Port"
    $ready = $false
    for ($i = 0; $i -lt 45; $i++) {
        Start-Sleep -Milliseconds 500
        try {
            $response = Invoke-WebRequest "$base/" -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -eq 200) { $ready = $true; break }
        } catch { }
    }
    if (-not $ready) { throw "Application did not become ready on port $Port." }
    $checks = @('/', '/courses', '/ai/status')
    foreach ($path in $checks) {
        $response = Invoke-WebRequest "$base$path" -UseBasicParsing -TimeoutSec 5
        if ($response.StatusCode -ne 200) { throw "$path returned HTTP $($response.StatusCode)." }
    }
    Write-Host "Fresh-JAR smoke verification passed on $base"
} finally {
    if ($proc -and -not $proc.HasExited) { Stop-Process -Id $proc.Id -Force }
}
