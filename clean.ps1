# Reliable `mvn clean` for this folder: retries a few times because
# OneDrive sync (and IntelliJ) sometimes holds handles on target\ briefly.
$max = 4
for ($i = 1; $i -le $max; $i++) {
    & .\mvnw.cmd clean
    if ($LASTEXITCODE -eq 0 -or $?) {
        if (Test-Path .\target) { $ok = $false } else { $ok = $true }
    } else { $ok = $false }

    if ($ok -and -not (Test-Path .\target)) { "clean OK (attempt $i)"; exit 0 }

    Write-Host "Clean failed with a file lock (attempt $i of $max). Retrying in 5s..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
}
Write-Host "Target folder still locked after $max attempts." -ForegroundColor Red
Write-Host "Pause OneDrive (right-click the cloud icon in the tray -> Pause syncing) and try again."
exit 1
