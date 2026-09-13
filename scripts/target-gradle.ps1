[CmdletBinding()]
param(
  [Parameter(Position=0)][string]$Target,
  [Parameter(Position=1,ValueFromRemainingArguments=$true)][string[]]$GradleArgs
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if ([string]::IsNullOrWhiteSpace($Target)) { $Target = 'forge-1.20.1' }
$javaHome = $env:MC_JAVA_HOME_21
if ([string]::IsNullOrWhiteSpace($javaHome)) { $javaHome = $env:JAVA_HOME }
if ([string]::IsNullOrWhiteSpace($javaHome)) { throw 'Set MC_JAVA_HOME_21 (bootstrap) or JAVA_HOME.' }
$java = Join-Path $javaHome 'bin\java.exe'
if (!(Test-Path -LiteralPath $java)) { throw "Java executable not found: $java" }
Push-Location $root
try {
  & (Join-Path $root 'gradlew.bat') ':tools:target-launcher:installDist' '--no-daemon'
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
  & (Join-Path $root 'tools\target-launcher\dist\bin\target-launcher.bat') $Target @GradleArgs
  exit $LASTEXITCODE
} finally { Pop-Location }
