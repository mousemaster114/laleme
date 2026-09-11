# Build script for the "LaLeMe" (拉了吗) Android app.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File build.ps1
#
# It uses the bundled Gradle Wrapper (gradlew.bat), so no global Gradle
# install is needed. Requires JDK 17, either via JAVA_HOME or on PATH.

param(
    # Where Gradle keeps its caches and the downloaded distribution.
    # Defaults to the normal per-user location; the script falls back to a
    # project-local one only if the per-user location is not writable.
    [string]$GradleUserHome = ""
)

$ErrorActionPreference = "Stop"

$projectDir = $PSScriptRoot

# --- JDK 17 ---------------------------------------------------------------
# Respect an existing JAVA_HOME; otherwise try the usual Windows install
# locations, and finally fall back to whatever "java" is on PATH.
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    $candidates = @(
        "C:\Program Files\Java\jdk-17",
        "C:\Program Files\Eclipse Adoptium\jdk-17*",
        "C:\Program Files\Microsoft\jdk-17*",
        "$env:LOCALAPPDATA\Programs\Android Studio\jbr"
    )
    foreach ($pattern in $candidates) {
        $hit = Get-Item -Path $pattern -ErrorAction SilentlyContinue |
               Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName "bin\java.exe") } |
               Select-Object -First 1
        if ($hit) { $env:JAVA_HOME = $hit.FullName; break }
    }
}
if ($env:JAVA_HOME) {
    Write-Host "==> JAVA_HOME = $env:JAVA_HOME" -ForegroundColor DarkGray
} else {
    Write-Host "==> JAVA_HOME not set, relying on 'java' from PATH" -ForegroundColor DarkYellow
}

# --- Gradle ---------------------------------------------------------------
$wrapper = Join-Path $projectDir "gradlew.bat"
if (Test-Path -LiteralPath $wrapper) {
    $gradleExe = $wrapper
} else {
    $gradleExe = "gradle"   # fall back to whatever is on PATH
}

# Gradle extracts its native library into a writable directory; keeping it
# inside the project avoids trouble with read-only or non-ASCII user profiles.
$nativeDir = Join-Path $projectDir ".build-tmp\gradle-native"
New-Item -ItemType Directory -Force -Path $nativeDir | Out-Null

if ($GradleUserHome) {
    $env:GRADLE_USER_HOME = $GradleUserHome
    Write-Host "==> GRADLE_USER_HOME = $env:GRADLE_USER_HOME" -ForegroundColor DarkGray
}

$gradleArgs = @(
    "-Dorg.gradle.native.dir=$nativeDir"
    ":app:assembleDebug"
    ":app:assembleRelease"
    "--console=plain"
    "--project-dir", $projectDir
)

Write-Host "==> Building debug + release APKs ..." -ForegroundColor Yellow

# Run; if Gradle cannot even bootstrap (for example the per-user Gradle home
# is not writable, which happens on some locked-down Windows accounts), retry
# once with a project-local Gradle home.
& $gradleExe @gradleArgs
$exitCode = $LASTEXITCODE

if ($exitCode -ne 0 -and -not $GradleUserHome) {
    Write-Host ""
    Write-Host "==> Gradle bootstrap failed; retrying with a project-local Gradle home ..." -ForegroundColor DarkYellow
    $localHome = Join-Path $projectDir ".build-tmp\gradle-home"
    New-Item -ItemType Directory -Force -Path $localHome | Out-Null
    $env:GRADLE_USER_HOME = $localHome
    & $gradleExe @gradleArgs
    $exitCode = $LASTEXITCODE
}

if ($exitCode -ne 0) {
    Write-Host "Build FAILED - see the errors above." -ForegroundColor Red
    exit $exitCode
}

Write-Host ""
Write-Host "==> Build finished. Artifacts:" -ForegroundColor Green
Get-ChildItem -Path (Join-Path $projectDir "app\build\outputs\apk") -Recurse -Filter "*.apk" |
    ForEach-Object { "    {0}  ({1:N1} MB)" -f $_.FullName, ($_.Length / 1MB) }
