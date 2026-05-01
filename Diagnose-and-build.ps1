# Diagnose-and-build.ps1
Set-StrictMode -Version Latest

# Determine project root
$Root = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
if ($Root -is [System.Array]) { $Root = $Root[0] }
$Root = [string]$Root

# Logs folder
$LogsDir = Join-Path $Root "logs"
if (-not (Test-Path $LogsDir)) {
  New-Item -ItemType Directory -Path $LogsDir -Force | Out-Null
}

# Paths
$gradleCache = "$env:USERPROFILE\.gradle\caches"
$gradleDists = "$env:USERPROFILE\.gradle\wrapper\dists"
$buildDirs = @(
  "$Root\data\build",
  "$Root\core\build",
  "$Root\app\build"
)

Write-Host "=== Diagnose-and-build started ===" -ForegroundColor Cyan
Write-Host "Root: $Root"

# Stop Gradle daemon completely
Write-Host "[1] Stopping Gradle daemon..."
$gradlewBat = Join-Path $Root "gradlew.bat"
if (Test-Path $gradlewBat) {
  & $gradlewBat --stop 2>$null
} else {
  gradle --stop 2>$null
}
# Kill any remaining Gradle processes
Get-Process -Name "GradleDaemon" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like "*gradle*" } | Stop-Process -Force -ErrorAction SilentlyContinue

# Full Gradle cache cleanup
Write-Host "[2] Full cleanup Gradle caches..."
$gradleCacheRoot = "$env:USERPROFILE\.gradle"
if (Test-Path $gradleCacheRoot) {
  Remove-Item -Recurse -Force $gradleCacheRoot -ErrorAction SilentlyContinue
  Write-Host "   Removed: $gradleCacheRoot"
}
Write-Host "[3] Build started..."

# Build locally
Push-Location $Root
if (Test-Path $gradlewBat) {
  & $gradlewBat :core:compileKotlin :data:compileKotlin --stacktrace 2>&1 | Tee-Object -FilePath "$LogsDir\gradle-local.log"
  & $gradlewBat :app:installDist --no-daemon --stacktrace 2>&1 | Tee-Object -FilePath "$LogsDir\gradle-app.log"
} else {
  gradle :core:compileKotlin :data:compileKotlin --stacktrace 2>&1 | Tee-Object -FilePath "$LogsDir\gradle-local.log"
  gradle :app:installDist --no-daemon --stacktrace 2>&1 | Tee-Object -FilePath "$LogsDir\gradle-app.log"
}
Pop-Location

# Docker build
Write-Host "[4] Docker build started..."
docker-compose build --progress=plain 2>&1 | Tee-Object -FilePath "$LogsDir\docker-build.log"

# Show errors only
Write-Host "`n=== Build Errors ===" -ForegroundColor Red
Select-String -Path "$LogsDir\gradle-local.log" -Pattern "FAILURE|ERROR|Unresolved reference|Cannot find" -ErrorAction SilentlyContinue | Select-Object -First 20
Select-String -Path "$LogsDir\gradle-app.log" -Pattern "FAILURE|ERROR|Unresolved reference|Cannot find" -ErrorAction SilentlyContinue | Select-Object -First 20
Select-String -Path "$LogsDir\docker-build.log" -Pattern "FAILURE|ERROR|Cannot|Error" -ErrorAction SilentlyContinue | Select-Object -First 20

Write-Host "`n=== Logs saved to ===" -ForegroundColor Cyan
Write-Host "   logs/gradle-local.log"
Write-Host "   logs/gradle-app.log"
Write-Host "   logs/docker-build.log"