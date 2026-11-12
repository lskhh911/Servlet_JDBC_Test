# 停止由 start.ps1 启动的 Tomcat
$ErrorActionPreference = "Stop"

$serverRoot = Join-Path $env:LOCALAPPDATA "my-app-tomcat"
$tomcatHome = Join-Path $serverRoot "apache-tomcat-9.0.123"
$pidFile = Join-Path $serverRoot "tomcat.pid"

function Get-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        return $env:JAVA_HOME
    }

    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        return $null
    }

    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $javaSettings = & java -XshowSettings:properties -version 2>&1
    } finally {
        $ErrorActionPreference = $previousPreference
    }

    $line = ($javaSettings |
        Select-String "java.home =" |
        Select-Object -First 1)

    if (-not $line) {
        return $null
    }
    return ($line.ToString() -replace "^.*java\.home =\s*", "").Trim()
}

$serverPid = $null
if (Test-Path $pidFile) {
    $rawPid = (Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
    if ($rawPid -match "^\d+$") {
        $serverPid = [int]$rawPid
    }
}

$isListening = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if (-not $isListening -and (-not $serverPid -or -not (Get-Process -Id $serverPid -ErrorAction SilentlyContinue))) {
    Write-Host "Tomcat 当前没有运行。" -ForegroundColor Yellow
    exit 0
}

if (Test-Path (Join-Path $tomcatHome "bin\shutdown.bat")) {
    $javaHome = Get-JavaHome
    if ($javaHome) {
        $env:JAVA_HOME = $javaHome
        $env:CATALINA_HOME = $tomcatHome
        Write-Host "正在停止 Tomcat..."
        $previousPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        try {
            & (Join-Path $tomcatHome "bin\shutdown.bat") 2>&1 | Out-Null
        } finally {
            $ErrorActionPreference = $previousPreference
        }
    }
}

for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Milliseconds 500
    if (-not (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)) {
        break
    }
}

if ($serverPid -and (Get-Process -Id $serverPid -ErrorAction SilentlyContinue)) {
    Stop-Process -Id $serverPid -Force -ErrorAction SilentlyContinue
}

Remove-Item $pidFile -ErrorAction SilentlyContinue
Write-Host "Tomcat 已停止。" -ForegroundColor Green
