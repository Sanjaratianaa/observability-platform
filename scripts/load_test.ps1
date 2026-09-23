# Load test — Observability Platform
# Envoie N batches de logs via POST /api/logs/raw/bulk et mesure le debit.
#
# Usage:
#   .\scripts\load_test.ps1                          # 10 batches x 100 lignes
#   .\scripts\load_test.ps1 -Batches 50 -LinesPerBatch 200
#   .\scripts\load_test.ps1 -BaseUrl http://localhost:8082

param(
    [string]$BaseUrl = "http://localhost:8082",
    [int]$Batches = 10,
    [int]$LinesPerBatch = 100,
    [int]$HealthTimeoutSec = 30
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$ResultsDir = Join-Path $RepoRoot ("results\load-" + (Get-Date -Format "yyyyMMdd-HHmmss"))
New-Item -ItemType Directory -Path $ResultsDir -Force | Out-Null

# --- Attente du backend -------------------------------------------------------
Write-Host "==> Verification du backend sur $BaseUrl"
$deadline = (Get-Date).AddSeconds($HealthTimeoutSec)
$healthy = $false
while ((Get-Date) -lt $deadline) {
    try {
        $health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -TimeoutSec 5
        if ($health.status -eq "UP") { $healthy = $true; break }
    } catch { Start-Sleep -Seconds 2 }
}
if (-not $healthy) { Write-Error "Backend non disponible sur $BaseUrl"; exit 1 }
Write-Host "    Backend UP"

# --- Generation du payload ----------------------------------------------------
$levels = @("INFO", "INFO", "INFO", "WARN", "ERROR")
$services = @("api", "web", "auth", "collector", "worker")
$messages = @(
    "request processed successfully",
    "cache hit for key=user_profile",
    "slow query detected (230ms)",
    "connection timeout to database",
    "NullPointerException at Service.java:42"
)

function New-LogBatch([int]$count) {
    $lines = for ($i = 0; $i -lt $count; $i++) {
        $ts = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
        $lvl = $levels | Get-Random
        $svc = $services | Get-Random
        $msg = $messages | Get-Random
        "{`"timestamp`":`"$ts`",`"level`":`"$lvl`",`"service`":`"$svc`",`"message`":`"$msg`"}"
    }
    return $lines -join "`n"
}

# --- Snapshot compteur avant --------------------------------------------------
$beforeCount = 0
try {
    $logs = Invoke-RestMethod -Uri "$BaseUrl/api/logs?page=0&size=1" -TimeoutSec 10
    $beforeCount = $logs.totalElements
} catch { Write-Warning "Impossible de lire le compteur de logs" }

# --- Boucle d'injection -------------------------------------------------------
Write-Host "==> Injection de $Batches batches x $LinesPerBatch lignes = $($Batches * $LinesPerBatch) logs"
$latencies = @()
$totalIngested = 0
$totalErrors = 0
$sw = [System.Diagnostics.Stopwatch]::StartNew()

for ($b = 1; $b -le $Batches; $b++) {
    $body = New-LogBatch $LinesPerBatch
    $batchSw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $r = Invoke-RestMethod -Uri "$BaseUrl/api/logs/raw/bulk?source=load-test" `
            -Method Post -Body $body -ContentType "text/plain; charset=utf-8" -TimeoutSec 120
        $batchSw.Stop()
        $latencies += $batchSw.ElapsedMilliseconds
        $totalIngested += $r.ingested
        $totalErrors += $r.parseErrors
        if ($b % 5 -eq 0 -or $b -eq $Batches) {
            Write-Host ("    batch {0,3}/{1} : {2} logs en {3} ms" -f $b, $Batches, $r.ingested, $batchSw.ElapsedMilliseconds)
        }
    } catch {
        $batchSw.Stop()
        Write-Warning "    batch $b echoue : $($_.Exception.Message)"
        $totalErrors += $LinesPerBatch
    }
}
$sw.Stop()

# --- Resultats ----------------------------------------------------------------
$totalSec = $sw.Elapsed.TotalSeconds
$throughput = [math]::Round($totalIngested / $totalSec, 1)
$avgLatency = if ($latencies.Count -gt 0) { [math]::Round(($latencies | Measure-Object -Average).Average, 1) } else { 0 }
$minLatency = if ($latencies.Count -gt 0) { ($latencies | Measure-Object -Minimum).Minimum } else { 0 }
$maxLatency = if ($latencies.Count -gt 0) { ($latencies | Measure-Object -Maximum).Maximum } else { 0 }
$p95 = if ($latencies.Count -gt 0) {
    $sorted = $latencies | Sort-Object
    $sorted[[math]::Floor($sorted.Count * 0.95)]
} else { 0 }

# Compteur apres
Start-Sleep -Seconds 3
$afterCount = 0
try {
    $logs = Invoke-RestMethod -Uri "$BaseUrl/api/logs?page=0&size=1" -TimeoutSec 10
    $afterCount = $logs.totalElements
} catch {}

$summary = [pscustomobject]@{
    timestamp          = (Get-Date -Format "o")
    base_url           = $BaseUrl
    batches            = $Batches
    lines_per_batch    = $LinesPerBatch
    total_sent         = $Batches * $LinesPerBatch
    total_ingested     = $totalIngested
    parse_errors       = $totalErrors
    duration_sec       = [math]::Round($totalSec, 2)
    throughput_logs_s  = $throughput
    avg_batch_ms       = $avgLatency
    min_batch_ms       = $minLatency
    max_batch_ms       = $maxLatency
    p95_batch_ms       = $p95
    es_docs_before     = $beforeCount
    es_docs_after      = $afterCount
}

$summary | ConvertTo-Json | Out-File (Join-Path $ResultsDir "summary.json") -Encoding utf8
$latencies | ForEach-Object { [pscustomobject]@{ batch_ms = $_ } } | Export-Csv (Join-Path $ResultsDir "latencies.csv") -NoTypeInformation

Write-Host ""
Write-Host "================ LOAD TEST ================"
Write-Host "Logs envoyes    : $($Batches * $LinesPerBatch)"
Write-Host "Logs ingeres    : $totalIngested"
Write-Host "Erreurs parsing : $totalErrors"
Write-Host "Duree totale    : $([math]::Round($totalSec, 2)) s"
Write-Host "Debit           : $throughput logs/s"
Write-Host "Latence batch   : avg=$avgLatency ms  min=$minLatency ms  max=$maxLatency ms  p95=$p95 ms"
Write-Host "Docs ES         : $beforeCount -> $afterCount"
Write-Host "Resultats dans  : $ResultsDir"
