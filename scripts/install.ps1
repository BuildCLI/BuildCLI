<#
.SYNOPSIS
  Installs BuildCLI for the current user on Windows.
.DESCRIPTION
  Downloads buildcli.jar from a GitHub release, verifies its SHA-256, puts it in %USERPROFILE%\.buildcli\bin and creates
  a buildcli.cmd launcher next to it. Nothing else is touched and no administrator rights are needed.
.PARAMETER FromFile
  Install a local jar instead of downloading (offline installs, CI).
.PARAMETER Version
  A release tag such as v1.0.0 (default: the latest release).
.PARAMETER Repo
  GitHub repository to download from (default: BuildCLI/BuildCLI).
.EXAMPLE
  irm https://github.com/BuildCLI/BuildCLI/releases/latest/download/install.ps1 | iex
#>
param(
  [string]$FromFile = "",
  [string]$Version = "latest",
  [string]$Repo = $(if ($env:BUILDCLI_REPO) { $env:BUILDCLI_REPO } else { "BuildCLI/BuildCLI" })
)
$ErrorActionPreference = "Stop"

function Fail($message) { Write-Error "error: $message"; exit 1 }

$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) { Fail "Java 21 or newer is required but 'java' was not found. Install a JDK 21+ first." }
$versionLine = (& java -version 2>&1 | Select-Object -First 1).ToString()
if ($versionLine -notmatch 'version "(\d+)') { Fail "could not read the Java version from: $versionLine" }
if ([int]$Matches[1] -lt 21) { Fail "Java 21 or newer is required (found $($Matches[1]))." }

$homeDir = if ($env:BUILDCLI_HOME) { $env:BUILDCLI_HOME } else { Join-Path $env:USERPROFILE ".buildcli" }
$binDir = Join-Path $homeDir "bin"
New-Item -ItemType Directory -Force -Path $binDir | Out-Null
$jar = Join-Path $binDir "buildcli.jar"
$tmp = Join-Path ([System.IO.Path]::GetTempPath()) ("buildcli-" + [System.Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tmp | Out-Null
try {
  $tmpJar = Join-Path $tmp "buildcli.jar"
  if ($FromFile) {
    if (-not (Test-Path $FromFile)) { Fail "no such file: $FromFile" }
    Copy-Item $FromFile $tmpJar
    Write-Host "Installing from $FromFile (checksum not verified for local files)"
  } else {
    $base = if ($env:BUILDCLI_BASE_URL) { $env:BUILDCLI_BASE_URL } elseif ($Version -eq "latest") { "https://github.com/$Repo/releases/latest/download" } else { "https://github.com/$Repo/releases/download/$Version" }
    Write-Host "Downloading $base/buildcli.jar"
    Invoke-WebRequest -Uri "$base/buildcli.jar" -OutFile $tmpJar -UseBasicParsing
    $sumFile = Join-Path $tmp "buildcli.jar.sha256"
    Invoke-WebRequest -Uri "$base/buildcli.jar.sha256" -OutFile $sumFile -UseBasicParsing
    $expected = ((Get-Content $sumFile -Raw).Trim() -split '\s+')[0].ToLowerInvariant()
    $actual = (Get-FileHash -Algorithm SHA256 $tmpJar).Hash.ToLowerInvariant()
    if ($expected -ne $actual) { Fail "checksum mismatch (expected $expected, got $actual); not installing" }
    Write-Host "Checksum verified"
  }
  Move-Item -Force $tmpJar $jar
} finally {
  Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
}

$launcher = Join-Path $binDir "buildcli.cmd"
# A lean JVM for a CLI (serial GC, quick JIT, small stacks, a class-data archive made on the first run); BUILDCLI_JAVA_OPTS adds options
Set-Content -Path $launcher -Encoding ASCII -Value "@echo off`r`njava -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+AutoCreateSharedArchive -XX:SharedArchiveFile=`"$jar.jsa`" -Xlog:disable %BUILDCLI_JAVA_OPTS% -jar `"$jar`" %*"
Write-Host "Installed: $launcher"
& $launcher --version

$onPath = ($env:PATH -split ';') -contains $binDir
if (-not $onPath) {
  Write-Host ""
  Write-Host "Add $binDir to your PATH to run 'buildcli' from anywhere, for example:"
  Write-Host "  [Environment]::SetEnvironmentVariable('PATH', `"$binDir;`" + [Environment]::GetEnvironmentVariable('PATH','User'), 'User')"
}
Write-Host ""
Write-Host "Next: cd into a project and run 'buildcli init', then 'buildcli doctor'."
