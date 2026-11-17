$ErrorActionPreference = 'Stop'

$projectPath = [System.IO.Path]::GetFullPath($PSScriptRoot).TrimEnd('\')
$projectAlias = 'C:\AetherClient'
$toolsPath = Join-Path (Split-Path -Parent $projectPath) '.tools'
$javaHome = Join-Path $toolsPath 'jdk-21'
$tempPath = 'C:\tmp'

if (-not (Test-Path -LiteralPath (Join-Path $javaHome 'bin\java.exe'))) {
    throw "JDK 21 not found at $javaHome"
}

if (Test-Path -LiteralPath $projectAlias) {
    $aliasItem = Get-Item -LiteralPath $projectAlias
    if ($aliasItem.LinkType -ne 'Junction') {
        throw "$projectAlias exists and is not a junction to this project ($projectPath)"
    }
    $aliasTarget = [System.IO.Path]::GetFullPath([string]$aliasItem.Target).TrimEnd('\')
    if ($aliasTarget -ne $projectPath) {
        throw "$projectAlias exists and does not point to this project ($projectPath)"
    }
} else {
    New-Item -ItemType Junction -Path $projectAlias -Target $projectPath | Out-Null
}

New-Item -ItemType Directory -Force -Path $tempPath | Out-Null
$env:JAVA_HOME = $javaHome
$env:GRADLE_HOME = Join-Path $toolsPath 'gradle-8.14.3'
$env:TEMP = $tempPath
$env:TMP = $tempPath
$env:Path = "$javaHome\bin;$env:GRADLE_HOME\bin;$env:Path"

function Invoke-AetherGradle {
    & "$projectAlias\gradlew.bat" --project-dir $projectAlias --no-daemon --max-workers=1 @args
}

Write-Host "Aether environment ready: JDK 21, Gradle wrapper 8.14.3, temp $tempPath"
Write-Host 'Use Invoke-AetherGradle clean test or Invoke-AetherGradle run.'
