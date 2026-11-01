# Polls the Spring Boot health endpoint until it reports UP.
# Usage: powershell -File scripts\health-check.ps1 -Url http://localhost:8080/actuator/health -TimeoutSeconds 90
param(
    [string]$Url = "http://localhost:8080/actuator/health",
    [int]$TimeoutSeconds = 90
)

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$last = "none"
Write-Host "Waiting for $Url (timeout ${TimeoutSeconds}s)..."
while ((Get-Date) -lt $deadline) {
    try {
        # Invoke-RestMethod parses the JSON (Invoke-WebRequest returns bytes for the actuator media type)
        $response = Invoke-RestMethod -Uri $Url -UseBasicParsing -TimeoutSec 5
        $last = $response | ConvertTo-Json -Compress
        if ($response.status -eq "UP") {
            Write-Host "Healthy: $last"
            exit 0
        }
    } catch {
        $last = $_.Exception.Message
    }
    Start-Sleep -Seconds 3
}

Write-Error "$Url did not report UP within ${TimeoutSeconds}s (last response: $last)"
exit 1
