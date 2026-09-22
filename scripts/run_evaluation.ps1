# Evaluation — Observability Platform
# Injecte data/sample/*.log via POST /api/logs/raw/bulk, collecte les incidents
# et les metriques Prometheus, puis compare aux annotations ground_truth.csv.
#
# Usage:
#   .\scripts\run_evaluation.ps1                    # backend deja demarre
#   .\scripts\run_evaluation.ps1 -StartStack        # demarre docker compose d'abord
#   .\scripts\run_evaluation.ps1 -BaseUrl http://localhost:8082

param(
    [string]$BaseUrl = "http://localhost:8082",
    [switch]$StartStack,
    [int]$HealthTimeoutSec = 90
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$SampleDir = Join-Path $RepoRoot "data\sample"
$GroundTruth = Join-Path $RepoRoot "data\annotations\ground_truth.csv"
$ResultsDir = Join-Path $RepoRoot ("results\eval-" + (Get-Date -Format "yyyyMMdd-HHmmss"))
New-Item -ItemType Directory -Path $ResultsDir -Force | Out-Null

# --- 1) Stack optionnelle -----------------------------------------------------
if ($StartStack) {
    Write-Host "==> Demarrage de la stack (docker compose)"
    docker compose -f (Join-Path $RepoRoot "infra\docker-compose.yml") up --build -d
}

# --- 2) Attente du backend ----------------------------------------------------
Write-Host "==> Attente du backend sur $BaseUrl (timeout ${HealthTimeoutSec}s)"
$deadline = (Get-Date).AddSeconds($HealthTimeoutSec)
$healthy = $false
while ((Get-Date) -lt $deadline) {
    try {
        $health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -TimeoutSec 5
        if ($health.status -eq "UP") { $healthy = $true; break }
    } catch { Start-Sleep -Seconds 3 }
}
if (-not $healthy) { Write-Error "Backend non disponible sur $BaseUrl"; exit 1 }
Write-Host "    Backend UP"

# --- 3) Snapshot des incidents existants (pour ne compter que les nouveaux) ---
$beforeIds = @()
try {
    $beforeIds = @(Invoke-RestMethod -Uri "$BaseUrl/api/incidents" -TimeoutSec 10 | ForEach-Object { $_.id })
} catch { Write-Warning "Impossible de lister les incidents existants" }

# --- 4) Injection des fichiers de logs ----------------------------------------
$ingestRows = @()
foreach ($file in Get-ChildItem -Path $SampleDir -Filter *.log) {
    $source = $file.Name
    Write-Host "==> Injection $($file.Name)"
    $body = Get-Content $file.FullName -Raw
    try {
        $result = Invoke-RestMethod -Uri "$BaseUrl/api/logs/raw/bulk?source=$source" `
            -Method Post -Body $body -ContentType "text/plain; charset=utf-8" -TimeoutSec 60
        Write-Host ("    recus={0} ingeres={1} erreurs_parse={2} anomalies={3}" -f `
            $result.received, $result.ingested, $result.parseErrors, $result.anomaliesDetected)
        $ingestRows += [pscustomobject]@{
            file = $source; received = $result.received; ingested = $result.ingested
            parse_errors = $result.parseErrors; anomalies = $result.anomaliesDetected
        }
    } catch {
        Write-Warning "    Echec injection $($file.Name) : $($_.Exception.Message)"
        $ingestRows += [pscustomobject]@{
            file = $source; received = 0; ingested = 0; parse_errors = 0; anomalies = 0
        }
    }
}

# Laisse le temps aux notifications async et au refresh ES
Start-Sleep -Seconds 5

# --- 5) Collecte incidents + metriques ----------------------------------------
$incidents = @(Invoke-RestMethod -Uri "$BaseUrl/api/incidents" -TimeoutSec 15)
$newIncidents = @($incidents | Where-Object { $beforeIds -notcontains $_.id })
$incidents | ConvertTo-Json -Depth 5 | Out-File (Join-Path $ResultsDir "incidents.json") -Encoding utf8
try {
    Invoke-WebRequest -Uri "$BaseUrl/actuator/prometheus" `
        -OutFile (Join-Path $ResultsDir "prometheus.txt") -TimeoutSec 15
} catch { Write-Warning "Endpoint prometheus inaccessible" }

# --- 6) Comparaison avec le ground truth --------------------------------------
# Un fichier est "detecte" si au moins un NOUVEL incident porte source = nom du fichier.
$gt = Import-Csv $GroundTruth
$gtFiles = @($gt | ForEach-Object { Split-Path $_.file -Leaf })
$detectedSources = @($newIncidents | ForEach-Object { $_.source } | Select-Object -Unique)

$tp = 0; $fn = 0
$detailRows = @()
foreach ($f in $gtFiles) {
    $hit = $detectedSources -contains $f
    if ($hit) { $tp++ } else { $fn++ }
    $detailRows += [pscustomobject]@{
        file = $f; expected = "INCIDENT"; detected = $(if ($hit) { "YES" } else { "NO" })
        verdict = $(if ($hit) { "TP" } else { "FN" })
    }
}
# Incidents sur des sources non annotees = faux positifs
foreach ($s in $detectedSources) {
    if ($gtFiles -notcontains $s) {
        $detailRows += [pscustomobject]@{ file = $s; expected = "none"; detected = "YES"; verdict = "FP" }
    }
}
$fp = @($detailRows | Where-Object { $_.verdict -eq "FP" }).Count

$precision = if (($tp + $fp) -gt 0) { [math]::Round($tp / ($tp + $fp), 3) } else { 0 }
$recall    = if (($tp + $fn) -gt 0) { [math]::Round($tp / ($tp + $fn), 3) } else { 0 }
$f1        = if (($precision + $recall) -gt 0) { [math]::Round(2 * $precision * $recall / ($precision + $recall), 3) } else { 0 }

# --- 7) Ecriture des resultats -------------------------------------------------
$ingestRows  | Export-Csv (Join-Path $ResultsDir "ingestion.csv") -NoTypeInformation -Encoding utf8
$detailRows  | Export-Csv (Join-Path $ResultsDir "detection.csv") -NoTypeInformation -Encoding utf8
[pscustomobject]@{
    timestamp = (Get-Date -Format "o"); base_url = $BaseUrl
    files_evaluated = $gtFiles.Count; new_incidents = $newIncidents.Count
    TP = $tp; FP = $fp; FN = $fn
    precision = $precision; recall = $recall; f1 = $f1
} | ConvertTo-Json | Out-File (Join-Path $ResultsDir "summary.json") -Encoding utf8

Write-Host ""
Write-Host "================ RESULTATS ================"
$detailRows | Format-Table -AutoSize
Write-Host "Nouveaux incidents : $($newIncidents.Count)  |  TP=$tp FP=$fp FN=$fn"
Write-Host "Precision=$precision  Recall=$recall  F1=$f1"
Write-Host "Resultats dans : $ResultsDir"
