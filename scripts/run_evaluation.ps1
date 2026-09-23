# Evaluation — Observability Platform
# Injecte data/sample/*.log via POST /api/logs/raw/bulk, collecte les incidents
# et les metriques Prometheus, puis compare aux annotations ground_truth.csv.
#
# Usage:
#   .\scripts\run_evaluation.ps1                    # backend deja demarre
#   .\scripts\run_evaluation.ps1 -StartStack        # demarre docker compose d'abord
#   .\scripts\run_evaluation.ps1 -Reset             # purge les index ES avant injection
#   .\scripts\run_evaluation.ps1 -Runs 3            # 3 repetitions (purge auto entre runs)
#   .\scripts\run_evaluation.ps1 -BaseUrl http://localhost:8082 -EsUrl http://localhost:9200

param(
    [string]$BaseUrl = "http://localhost:8082",
    [string]$EsUrl = "http://localhost:9200",
    [switch]$StartStack,
    [switch]$Reset,
    [int]$Runs = 1,
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

# --- Ground truth (charge une fois) -------------------------------------------
$gt = Import-Csv $GroundTruth
$gtFiles = @($gt | ForEach-Object { Split-Path $_.file -Leaf })

if ($Runs -gt 1) {
    $Reset = $true
    Write-Warning "Runs>1 : purge ES automatique entre runs. Note : la fenetre glissante du detecteur error-rate est en memoire (5 min) — des runs rapproches peuvent partager son etat."
}

function Invoke-EvalRun([int]$RunIndex, [string]$RunDir) {
    # --- Reset : purge des index ES (logs + incidents) -------------------------
    if ($Reset) {
        Write-Host "==> Reset : purge des index Elasticsearch"
        try {
            Invoke-RestMethod -Uri "$EsUrl/logs,incidents" -Method Delete -TimeoutSec 15 | Out-Null
        } catch { Write-Warning "    Purge ES impossible : $($_.Exception.Message)" }
        Start-Sleep -Seconds 2
    }

    # --- Snapshot des incidents existants (pour ne compter que les nouveaux) ---
    $beforeIds = @()
    try {
        $beforeIds = @(Invoke-RestMethod -Uri "$BaseUrl/api/incidents" -TimeoutSec 10 | ForEach-Object { $_.id })
    } catch { Write-Warning "Impossible de lister les incidents existants" }

    # --- Injection des fichiers de logs ---------------------------------------
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

    # --- Collecte incidents + metriques ---------------------------------------
    $incidents = @(Invoke-RestMethod -Uri "$BaseUrl/api/incidents" -TimeoutSec 15)
    $newIncidents = @($incidents | Where-Object { $beforeIds -notcontains $_.id })
    $incidents | ConvertTo-Json -Depth 5 | Out-File (Join-Path $RunDir "incidents.json") -Encoding utf8
    try {
        Invoke-WebRequest -Uri "$BaseUrl/actuator/prometheus" `
            -OutFile (Join-Path $RunDir "prometheus.txt") -TimeoutSec 15
    } catch { Write-Warning "Endpoint prometheus inaccessible" }

    # --- Comparaison avec le ground truth -------------------------------------
    # Un fichier est "detecte" si au moins un NOUVEL incident porte source = nom du fichier.
    $detectedSources = @($newIncidents | ForEach-Object { $_.source } | Select-Object -Unique)

    $tp = 0; $fn = 0
    $detailRows = @()
    foreach ($f in $gtFiles) {
        $hits = @($newIncidents | Where-Object { $_.source -eq $f })
        $hit = $hits.Count -gt 0
        if ($hit) { $tp++ } else { $fn++ }
        $detailRows += [pscustomobject]@{
            file = $f; expected = "INCIDENT"; detected = $(if ($hit) { "YES" } else { "NO" })
            incident_types = ($hits | ForEach-Object { $_.type }) -join "|"
            verdict = $(if ($hit) { "TP" } else { "FN" })
        }
    }
    # Incidents sur des sources non annotees = faux positifs
    foreach ($s in $detectedSources) {
        if ($gtFiles -notcontains $s) {
            $types = ($newIncidents | Where-Object { $_.source -eq $s } | ForEach-Object { $_.type }) -join "|"
            $detailRows += [pscustomobject]@{
                file = $s; expected = "none"; detected = "YES"; incident_types = $types; verdict = "FP"
            }
        }
    }
    $fp = @($detailRows | Where-Object { $_.verdict -eq "FP" }).Count

    $precision = if (($tp + $fp) -gt 0) { [math]::Round($tp / ($tp + $fp), 3) } else { 0 }
    $recall    = if (($tp + $fn) -gt 0) { [math]::Round($tp / ($tp + $fn), 3) } else { 0 }
    $f1        = if (($precision + $recall) -gt 0) { [math]::Round(2 * $precision * $recall / ($precision + $recall), 3) } else { 0 }

    # --- Ecriture des resultats du run -----------------------------------------
    $ingestRows  | Export-Csv (Join-Path $RunDir "ingestion.csv") -NoTypeInformation -Encoding utf8
    $detailRows  | Export-Csv (Join-Path $RunDir "detection.csv") -NoTypeInformation -Encoding utf8

    Write-Host ""
    Write-Host "================ RUN $RunIndex ================"
    $detailRows | Format-Table -AutoSize
    Write-Host "Nouveaux incidents : $($newIncidents.Count)  |  TP=$tp FP=$fp FN=$fn"
    Write-Host "Precision=$precision  Recall=$recall  F1=$f1"

    return [pscustomobject]@{
        run = $RunIndex; new_incidents = $newIncidents.Count
        TP = $tp; FP = $fp; FN = $fn
        precision = $precision; recall = $recall; f1 = $f1
    }
}

# --- Boucle de runs ------------------------------------------------------------
$runSummaries = @()
for ($r = 1; $r -le $Runs; $r++) {
    $runDir = if ($Runs -gt 1) { Join-Path $ResultsDir "run$r" } else { $ResultsDir }
    New-Item -ItemType Directory -Path $runDir -Force | Out-Null
    $runSummaries += Invoke-EvalRun -RunIndex $r -RunDir $runDir
}

# --- Agregation multi-runs ------------------------------------------------------
$summary = [pscustomobject]@{
    timestamp = (Get-Date -Format "o"); base_url = $BaseUrl
    runs = $Runs; files_evaluated = $gtFiles.Count
    results = $runSummaries
}
if ($Runs -gt 1) {
    $prec = @($runSummaries | ForEach-Object { $_.precision })
    $rec  = @($runSummaries | ForEach-Object { $_.recall })
    $f1s  = @($runSummaries | ForEach-Object { $_.f1 })
    $summary | Add-Member -NotePropertyName mean -NotePropertyValue ([pscustomobject]@{
        precision = [math]::Round(($prec | Measure-Object -Average).Average, 3)
        recall    = [math]::Round(($rec  | Measure-Object -Average).Average, 3)
        f1        = [math]::Round(($f1s  | Measure-Object -Average).Average, 3)
    })
    $summary | Add-Member -NotePropertyName std -NotePropertyValue ([pscustomobject]@{
        precision = [math]::Round(($prec | Measure-Object -StandardDeviation).StandardDeviation, 3)
        recall    = [math]::Round(($rec  | Measure-Object -StandardDeviation).StandardDeviation, 3)
        f1        = [math]::Round(($f1s  | Measure-Object -StandardDeviation).StandardDeviation, 3)
    })
    Write-Host ""
    Write-Host "================ AGREGAT ($Runs runs) ================"
    Write-Host ("Precision moy={0} +/- {1}  Recall moy={2} +/- {3}  F1 moy={4} +/- {5}" -f `
        $summary.mean.precision, $summary.std.precision, `
        $summary.mean.recall, $summary.std.recall, `
        $summary.mean.f1, $summary.std.f1)
}
$summary | ConvertTo-Json -Depth 5 | Out-File (Join-Path $ResultsDir "summary.json") -Encoding utf8
Write-Host "Resultats dans : $ResultsDir"
