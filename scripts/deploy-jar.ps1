# Deploys the built JAR as a background process on Windows.
# Stops whatever listens on the port, keeps the previous JAR for rollback, starts the new
# one and waits for the health check.
#
# Usage: powershell -ExecutionPolicy Bypass -File scripts\deploy-jar.ps1 -Port 8085 [-DeployDir C:\recipe-deploy]
param(
    [Parameter(Mandatory = $true)][int]$Port,
    [string]$DeployDir = "$env:USERPROFILE\recipe-deploy",
    [string]$Jar = "target\recipe-management-system.jar"
)
$ErrorActionPreference = "Stop"

New-Item -ItemType Directory -Force -Path $DeployDir | Out-Null

$listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
foreach ($conn in $listeners) {
    Write-Host "Stopping previous instance (pid $($conn.OwningProcess)) on port $Port"
    Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
}
Start-Sleep -Seconds 2

$current = Join-Path $DeployDir "app.jar"
if (Test-Path $current) {
    Copy-Item $current (Join-Path $DeployDir "app-previous.jar") -Force
}
Copy-Item $Jar $current -Force

# Prevent Jenkins from killing the process tree when the build finishes.
$env:JENKINS_NODE_COOKIE = "dontKillMe"
$env:BUILD_ID = "dontKillMe"
$process = Start-Process -FilePath "java" `
    -ArgumentList @("-jar", "`"$current`"", "--server.port=$Port") `
    -RedirectStandardOutput (Join-Path $DeployDir "app.log") `
    -RedirectStandardError (Join-Path $DeployDir "app-error.log") `
    -WindowStyle Hidden -PassThru
Set-Content -Path (Join-Path $DeployDir "app.pid") -Value $process.Id
Write-Host "Started pid $($process.Id) on port $Port; logs: $DeployDir\app.log"

& "$PSScriptRoot\health-check.ps1" -Url "http://localhost:$Port/actuator/health" -TimeoutSeconds 120
if ($LASTEXITCODE -ne 0) {
    Get-Content (Join-Path $DeployDir "app.log") -Tail 50
    exit 1
}
