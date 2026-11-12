# 一键启动本地开发环境：构建 WAR + 下载 Tomcat 9 + 部署 + 启动
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$tomcatVersion = "9.0.123"
$serverRoot = Join-Path $env:LOCALAPPDATA "my-app-tomcat"
$tomcatHome = Join-Path $serverRoot "apache-tomcat-$tomcatVersion"
$appUrl = "http://localhost:8080/my-app/"
$apiUrl = "${appUrl}api/students"
$pidFile = Join-Path $serverRoot "tomcat.pid"

function Get-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        return $env:JAVA_HOME
    }

    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        throw "未找到 Java，请先安装 JDK 8 或更高版本。"
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
        throw "无法识别 Java 安装目录，请设置 JAVA_HOME 环境变量后重试。"
    }

    $candidate = ($line.ToString() -replace "^.*java\.home =\s*", "").Trim()
    if (-not (Test-Path (Join-Path $candidate "bin\java.exe"))) {
        throw "Java 安装目录不可用：$candidate"
    }
    return $candidate
}

function Test-AppOnline {
    try {
        $response = Invoke-WebRequest -Uri $apiUrl -UseBasicParsing -TimeoutSec 2
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

Push-Location $projectRoot
try {
    if (Test-AppOnline) {
        Write-Host "应用已经在运行：$appUrl" -ForegroundColor Green
        exit 0
    }

    $env:JAVA_HOME = Get-JavaHome
    Write-Host "Java：$env:JAVA_HOME"

    Write-Host "正在构建 WAR..."
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & mvn -q clean package
        $buildExitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($buildExitCode -ne 0) {
        throw "Maven 构建失败，请查看上方输出。"
    }

    $catalina = Join-Path $tomcatHome "bin\catalina.bat"
    if (-not (Test-Path $catalina)) {
        New-Item -ItemType Directory -Force -Path $serverRoot | Out-Null
        $zipPath = Join-Path $serverRoot "apache-tomcat-$tomcatVersion.zip"
        $downloadUrls = @(
            "https://dlcdn.apache.org/tomcat/tomcat-9/v$tomcatVersion/bin/apache-tomcat-$tomcatVersion.zip",
            "https://archive.apache.org/dist/tomcat/tomcat-9/v$tomcatVersion/bin/apache-tomcat-$tomcatVersion.zip"
        )

        Write-Host "首次运行，正在下载 Tomcat $tomcatVersion..."
        $downloaded = $false
        foreach ($downloadUrl in $downloadUrls) {
            try {
                Invoke-WebRequest -Uri $downloadUrl -OutFile $zipPath -UseBasicParsing
                $downloaded = $true
                break
            } catch {
                Write-Host "下载地址不可用，尝试下一个..."
            }
        }

        if (-not $downloaded) {
            throw "Tomcat 下载失败，请检查网络后重试。"
        }

        Write-Host "正在解压 Tomcat..."
        Expand-Archive -Path $zipPath -DestinationPath $serverRoot -Force
        Remove-Item $zipPath -ErrorAction SilentlyContinue
    }

    if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) {
        throw "端口 8080 已被占用，请先关闭占用 8080 的程序。"
    }

    $webapps = Join-Path $tomcatHome "webapps"
    Get-ChildItem -Path $webapps -Filter "my-app*" -Force -ErrorAction SilentlyContinue |
        Remove-Item -Recurse -Force
    Copy-Item (Join-Path $projectRoot "target\my-app.war") (Join-Path $webapps "my-app.war") -Force

    $logRoot = Join-Path $serverRoot "logs"
    New-Item -ItemType Directory -Force -Path $logRoot | Out-Null
    $stdout = Join-Path $logRoot "console.log"
    $stderr = Join-Path $logRoot "console-error.log"
    Remove-Item $stdout, $stderr -ErrorAction SilentlyContinue

    $env:CATALINA_HOME = $tomcatHome
    Write-Host "正在启动 Tomcat..."
    $tomcatProcess = Start-Process -FilePath $catalina `
        -ArgumentList "run" `
        -WorkingDirectory $tomcatHome `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -PassThru
    Set-Content -Path $pidFile -Value $tomcatProcess.Id -Encoding ASCII

    $online = $false
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Milliseconds 500
        if (Test-AppOnline) {
            $online = $true
            break
        }
        if ($tomcatProcess.HasExited) {
            break
        }
    }

    if (-not $online) {
        Write-Host "启动失败，最后 40 行日志：" -ForegroundColor Red
        if (Test-Path $stdout) { Get-Content $stdout -Tail 40 }
        if (Test-Path $stderr) { Get-Content $stderr -Tail 40 }
        throw "Tomcat 未能正常启动。"
    }

    Write-Host ""
    Write-Host "启动成功！" -ForegroundColor Green
    Write-Host "访问地址：$appUrl"
    Write-Host "停止服务：运行 stop.bat 或 .\stop.ps1"
} finally {
    Pop-Location
}
