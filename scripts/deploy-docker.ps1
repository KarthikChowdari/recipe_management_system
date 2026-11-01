# Windows equivalent of deploy-docker.sh: replace the app container, wait for Docker's health
# check, and restart the previous image if the new one is unhealthy.
#
# Usage: powershell -ExecutionPolicy Bypass -File scripts\deploy-docker.ps1 -Image recipe-management-system:42 -Port 8085
param(
    [Parameter(Mandatory = $true)][string]$Image,
    [Parameter(Mandatory = $true)][int]$Port,
    [string]$Name = "recipe-app",
    [int]$TimeoutSeconds = 150
)

function Start-AppContainer([string]$img) {
    docker run -d --name $Name -p "${Port}:8080" --restart unless-stopped `
        --health-interval 5s --health-start-period 120s $img | Out-Null
}

function Wait-Healthy {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $status = docker inspect --format '{{.State.Health.Status}}' $Name 2>$null
        if ($status -eq "healthy") { return $true }
        if ($status -eq "unhealthy" -or -not $status) { return $false }
        if ((docker inspect --format '{{.State.Running}}' $Name) -ne "true") { return $false }
        Start-Sleep -Seconds 3
    }
    return $false
}

$previous = docker inspect --format '{{.Config.Image}}' $Name 2>$null
Write-Host "Current image: $(if ($previous) { $previous } else { '<none>' })  ->  new image: $Image"

docker rm -f $Name 2>$null | Out-Null
Start-AppContainer $Image
Write-Host "Started $Name on port $Port, waiting for health check (timeout ${TimeoutSeconds}s)..."

if (Wait-Healthy) {
    Write-Host "Deployment healthy: $Image on http://localhost:$Port"
    exit 0
}

Write-Warning "$Image did not become healthy. Last log lines:"
docker logs --tail 80 $Name
docker rm -f $Name 2>$null | Out-Null

if ($previous -and $previous -ne $Image) {
    Write-Warning "Rolling back to $previous"
    Start-AppContainer $previous
    if (Wait-Healthy) { Write-Warning "Rollback succeeded: $previous is serving on port $Port" }
    else { Write-Warning "Rollback FAILED: $previous is not healthy either" }
}
exit 1
