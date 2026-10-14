<#
.SYNOPSIS
  Toma las capturas del wiki en el emulador y regenera docs/wiki.

.DESCRIPTION
  1. Busca el SDK de Android, un JDK 17-21 y un dispositivo. Si no hay ninguno conectado,
     arranca el emulador (AVD) en segundo plano.
  2. Corre la prueba instrumentada WikiScreenshotsTest (siembra datos de ejemplo y recorre la app).
  3. Descarga las capturas a build/wiki-screenshots.
  4. Corre los tests unitarios y `generateWiki`, que reduce las capturas y escribe docs/wiki.

.PARAMETER Avd          Nombre del AVD a arrancar (por defecto el primero que exista).
.PARAMETER JavaHome     JDK a usar para Gradle (por defecto JAVA_HOME si es 17-21, o uno encontrado).
.PARAMETER KeepEmulator No apagar el emulador al terminar (solo aplica si lo arrancó este script).

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File tools\wiki\capture-screenshots.ps1
#>
param(
    [string]$Avd,
    [string]$JavaHome,
    [switch]$KeepEmulator
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$appId = 'com.alex.metasahorro'
$remoteDir = '/data/local/tmp/wiki'
$localDir = Join-Path $root 'build\wiki-screenshots'

function Step($text) { Write-Host "`n==> $text" -ForegroundColor Cyan }
function Fail($text) { Write-Host "ERROR: $text" -ForegroundColor Red; exit 1 }

# ---------------------------------------------------------------- SDK
$sdk = $env:ANDROID_HOME
if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
if (-not $sdk) {
    $lp = Join-Path $root 'local.properties'
    if (Test-Path $lp) {
        $line = Select-String -Path $lp -Pattern '^sdk\.dir=(.*)$' | Select-Object -First 1
        if ($line) { $sdk = $line.Matches[0].Groups[1].Value -replace '\\\\', '\' -replace '\\:', ':' }
    }
}
if (-not $sdk -or -not (Test-Path $sdk)) { Fail 'No encontré el SDK de Android (define ANDROID_HOME o sdk.dir en local.properties).' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$emulator = Join-Path $sdk 'emulator\emulator.exe'
if (-not (Test-Path $adb)) { Fail "No existe $adb" }

# ---------------------------------------------------------------- JDK (Gradle 8.x soporta 17-21)
function Get-JdkMajor([string]$path) {
    $rel = Join-Path $path 'release'
    if (-not (Test-Path (Join-Path $path 'bin\java.exe')) -or -not (Test-Path $rel)) { return 0 }
    $m = Select-String -Path $rel -Pattern 'JAVA_VERSION="?(\d+)' | Select-Object -First 1
    if ($m) { return [int]$m.Matches[0].Groups[1].Value }
    return 0
}
function Find-Jdk {
    $candidates = @($JavaHome, $env:JAVA_HOME)
    $candidates += Get-ChildItem (Join-Path $env:USERPROFILE '.jdks') -Directory -ErrorAction SilentlyContinue | ForEach-Object FullName
    $candidates += Get-ChildItem 'C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Microsoft' -Directory -ErrorAction SilentlyContinue | ForEach-Object FullName
    $candidates += 'C:\Program Files\Android\Android Studio\jbr'
    foreach ($c in $candidates) {
        if (-not $c) { continue }
        $major = Get-JdkMajor $c
        if ($major -ge 17 -and $major -le 21) { return $c }
    }
    return $null
}
$jdk = Find-Jdk
if (-not $jdk) { Fail 'No encontré un JDK 17-21. Pásalo con -JavaHome.' }
$env:JAVA_HOME = $jdk
Write-Host "JDK: $jdk"

$gradlew = Join-Path $root 'gradlew.bat'
if (-not (Test-Path $gradlew)) { Fail 'Falta gradlew.bat. Genera el wrapper con: gradle wrapper' }

# ---------------------------------------------------------------- dispositivo
function Get-Devices { @(& $adb devices | Select-String '\tdevice$') }

$startedEmulator = $null
if ((Get-Devices).Count -eq 0) {
    if (-not (Test-Path $emulator)) { Fail 'No hay dispositivo conectado y no encontré el emulador.' }
    if (-not $Avd) {
        $Avd = (& $emulator -list-avds | Select-Object -First 1)
        if (-not $Avd) { Fail 'No hay AVDs creados. Crea uno en Android Studio (Device Manager).' }
    }
    Step "Arrancando el emulador '$Avd' (puede tardar unos minutos)"
    $startedEmulator = Start-Process $emulator -ArgumentList @('-avd', $Avd, '-no-window', '-no-audio', '-no-boot-anim', '-no-snapshot-save') -PassThru
    & $adb wait-for-device | Out-Null
    $deadline = (Get-Date).AddMinutes(8)
    while ((Get-Date) -lt $deadline) {
        $boot = (& $adb shell getprop sys.boot_completed 2>$null | Out-String).Trim()
        if ($boot -eq '1') { break }
        Start-Sleep -Seconds 3
    }
    if ($boot -ne '1') { Fail 'El emulador no terminó de arrancar a tiempo.' }
    Start-Sleep -Seconds 5
}

# Sin animaciones: las capturas salen estables y la prueba corre más rápido.
$scales = 'window_animation_scale', 'transition_animation_scale', 'animator_duration_scale'
foreach ($s in $scales) { & $adb shell settings put global $s 0 | Out-Null }
& $adb shell input keyevent KEYCODE_WAKEUP | Out-Null
& $adb shell wm dismiss-keyguard | Out-Null

try {
    # ---------------------------------------------------------------- prueba instrumentada
    Step 'Instalación limpia, tests en el emulador y recorrido de la app'
    & $adb uninstall $appId 2>&1 | Out-Null
    & $gradlew --console=plain :app:connectedDebugAndroidTest
    if ($LASTEXITCODE -ne 0) { Fail 'Falló la prueba instrumentada (revisa app\build\reports\androidTests).' }

    Step 'Descargando capturas'
    if (Test-Path $localDir) { Remove-Item $localDir -Recurse -Force }
    & $adb pull $remoteDir $localDir | Out-Null
    if (-not (Test-Path $localDir)) { Fail 'No se descargaron capturas.' }
    & $adb shell rm -r $remoteDir | Out-Null
    Write-Host ("Capturas: " + (Get-ChildItem $localDir -Filter *.png).Count)

    # ---------------------------------------------------------------- wiki
    Step 'Tests unitarios y generación del wiki'
    & $gradlew --console=plain :app:testDebugUnitTest :app:generateWiki
    if ($LASTEXITCODE -ne 0) { Fail 'Falló la generación del wiki.' }
}
finally {
    foreach ($s in $scales) { & $adb shell settings put global $s 1.0 | Out-Null }
    & $adb shell am broadcast -a com.android.systemui.demo -e command exit 2>&1 | Out-Null
    if ($startedEmulator -and -not $KeepEmulator) {
        Step 'Apagando el emulador'
        & $adb emu kill 2>&1 | Out-Null
    }
}
Write-Host "`nListo: docs\wiki actualizado." -ForegroundColor Green
