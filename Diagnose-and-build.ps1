# Diagnose-and-build.ps1
Set-StrictMode -Version Latest

# Определяем корень проекта корректно (одна строка)
$Root = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
# Ensure $Root is a single string (not an array)
if ($Root -is [System.Array]) {
    $Root = $Root[0]
}
$Root = [string]$Root

# Пути к кэшу Gradle и каталогам сборки
$gradleCache = "$env:USERPROFILE\.gradle\caches"
$gradleDists = "$env:USERPROFILE\.gradle\wrapper\dists"
$buildDirs = @(
  Join-Path $Root "data/build",
  Join-Path $Root "core/build",
  Join-Path $Root "app/build"
)

Write-Host "`n[Step 1] Очистка кэшей Gradle..."
foreach ($p in @($gradleCache, $gradleDists)) {
  if (Test-Path $p) {
    Remove-Item -Recurse -Force $p -ErrorAction SilentlyContinue
    Write-Host "Cleared: $p"
  } else {
    Write-Host "Not found: $p"
  }
}

Write-Host "`n[Step 1] Очистка сборочных директорий..."
foreach ($d in $buildDirs) {
  if (Test-Path $d) {
    Remove-Item -Recurse -Force $d -ErrorAction SilentlyContinue
    Write-Host "Cleared: $d"
  } else {
    Write-Host "Not found: $d"
  }
}

# Локальная компиляция с логами
$gradlewBat = Join-Path $Root "gradlew.bat"
$gradleLogLocal = Join-Path $Root "gradle-local.log"
$gradleAppLog = Join-Path $Root "gradle-app-install.log"

Write-Host "`n[Step 2] Локальная сборка (core & data) с stacktrace и info..."
if (Test-Path $gradlewBat) {
  Push-Location $Root
  & $gradlewBat clean
  & $gradlewBat :core:compileKotlin :data:compileKotlin --stacktrace --info 2>&1 | Tee-Object -FilePath $gradleLogLocal
  & $gradlewBat :app:installDist --no-daemon --stacktrace --info 2>&1 | Tee-Object -FilePath $gradleAppLog
  Pop-Location
} else {
  Push-Location $Root
  gradle clean
  gradle :core:compileKotlin :data:compileKotlin --stacktrace --info 2>&1 | Tee-Object -FilePath $gradleLogLocal
  gradle :app:installDist --no-daemon --stacktrace --info 2>&1 | Tee-Object -FilePath $gradleAppLog
  Pop-Location
}

# Docker сборка с логами
$dockerLog = Join-Path $Root "docker-build.log"
Write-Host "`n[Step 3] Docker сборка (docker-compose) с логами..."
docker-compose build --progress=plain 2>&1 | Tee-Object -FilePath $dockerLog -Append

# Вывод последних логов
Write-Host "`n=== Последние строки логов ==="
Get-Content -Tail 300 $gradleLogLocal -ErrorAction SilentlyContinue
Get-Content -Tail 300 $gradleAppLog -ErrorAction SilentlyContinue
Get-Content -Tail 300 $dockerLog -ErrorAction SilentlyContinue

# Быстрые подсказки по ошибкам
Write-Host "`n=== Подсказки ==="
Write-Host "Поиск ошибок Unresolved reference или Cannot find symbol в логах: (копируйте фрагменты логов и пришлите сюда)"
