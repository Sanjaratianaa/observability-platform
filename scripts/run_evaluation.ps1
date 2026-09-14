# Script PowerShell d'évaluation minimal
# Usage: .\scripts\run_evaluation.ps1

$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $RepoRoot

Write-Host "1) Démarrage de la stack via infra/docker-compose.yml"
Set-Location (Join-Path $RepoRoot 'infra')
docker compose up --build -d
Start-Sleep -Seconds 8

Write-Host "2) Vérifications rapides"
try { Invoke-RestMethod -Uri http://localhost:8082/actuator/health | Write-Host } catch { Write-Host "Backend non accessible" }
try { Invoke-RestMethod -Uri http://localhost:8082/actuator/prometheus -ErrorAction Stop | Select-Object -First 20 } catch { Write-Host "Prometheus non accessible" }

Write-Host "3) Injection de logs (exemple). Adapter l'URL d'ingestion si nécessaire"
# Exemple (décommenter et adapter si backend expose /ingest)
# Invoke-RestMethod -Uri 'http://localhost:8082/ingest' -Method Post -InFile (Join-Path $RepoRoot 'data\sample\apache.log') -ContentType 'text/plain'

$results = Join-Path $RepoRoot 'results'
New-Item -ItemType Directory -Path $results -Force | Out-Null
$tag = Get-Date -Format "yyyyMMdd-HHmmss"
try { Invoke-WebRequest -Uri http://localhost:8082/actuator/prometheus -OutFile (Join-Path $results "prometheus-$tag.txt") } catch { Write-Host "Impossible de récupérer prometheus" }

Write-Host "Evaluation terminée. Résultats: $results"
