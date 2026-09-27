# Teams ChatOps Demo — Observability Platform
# Simule une conversation ChatOps dans un canal Microsoft Teams.
# Envoie des commandes au ChatOps API, puis poste les reponses dans Teams
# via un webhook entrant, creant un flux conversationnel visible dans le canal.
#
# Usage:
#   .\scripts\teams_chatops_demo.ps1 -WebhookUrl "https://xxx.webhook.office.com/..."
#   .\scripts\teams_chatops_demo.ps1 -WebhookUrl $env:TEAMS_WEBHOOK_URL
#   .\scripts\teams_chatops_demo.ps1 -WebhookUrl "..." -ChatOpsUrl http://localhost:8083
#
# Le script :
#   1. Envoie la commande "help" et affiche les commandes disponibles
#   2. Envoie "list" pour lister les incidents ouverts
#   3. Envoie "stats" pour les statistiques
#   4. Acquitte le premier incident ouvert (si disponible)
#   5. Resout le premier incident acquitte (si disponible)
#
# Chaque echange est poste dans Teams comme une paire commande/reponse.

param(
    [Parameter(Mandatory=$true)]
    [string]$WebhookUrl,
    [string]$ChatOpsUrl = "http://localhost:8083",
    [string]$IncidentUrl = "http://localhost:8082",
    [int]$DelayBetweenSec = 3
)

$ErrorActionPreference = "Stop"

# --- Fonctions utilitaires ---------------------------------------------------

function Send-TeamsCard {
    param(
        [string]$Title,
        [string]$Text,
        [string]$Color = "0076D7",
        [hashtable[]]$Facts = @()
    )
    $sections = @(@{ activityTitle = $Title; text = $Text; markdown = $true })
    if ($Facts.Count -gt 0) {
        $sections[0]["facts"] = $Facts
    }
    $card = @{
        "@type"      = "MessageCard"
        "@context"   = "http://schema.org/extensions"
        themeColor   = $Color
        summary      = $Title
        sections     = $sections
    }
    $json = $card | ConvertTo-Json -Depth 10 -Compress
    try {
        Invoke-RestMethod -Uri $WebhookUrl -Method Post -Body $json `
            -ContentType "application/json; charset=utf-8" -TimeoutSec 15 | Out-Null
    } catch {
        Write-Warning "Erreur envoi Teams : $($_.Exception.Message)"
    }
}

function Send-UserCommand {
    param([string]$Command)
    Send-TeamsCard -Title "👤 Commande utilisateur" `
        -Text "``$Command``" -Color "6264A7"
    Start-Sleep -Seconds 1
}

function Send-BotResponse {
    param(
        [string]$Command,
        [string]$Response
    )
    # Formatte la reponse pour Teams (remplace les retours a la ligne)
    $formatted = $Response -replace "`r`n", "<br>" -replace "`n", "<br>"
    Send-TeamsCard -Title "🤖 Observability Bot" `
        -Text $formatted -Color "00AA00" `
        -Facts @(
            @{ name = "Commande"; value = $Command }
        )
}

function Invoke-ChatOps {
    param([string]$Command)
    try {
        $response = Invoke-RestMethod -Uri "$ChatOpsUrl/api/chatops" `
            -Method Post -Body $Command -ContentType "text/plain; charset=utf-8" -TimeoutSec 15
        return $response
    } catch {
        return "Erreur : $($_.Exception.Message)"
    }
}

# --- Verification de la sante des services -----------------------------------

Write-Host "==> Verification des services..."
$servicesOk = $true
foreach ($svc in @(@{n='chatops'; u=$ChatOpsUrl}, @{n='incident'; u=$IncidentUrl})) {
    try {
        $health = Invoke-RestMethod -Uri "$($svc.u)/actuator/health" -TimeoutSec 5
        if ($health.status -eq "UP") {
            Write-Host "    $($svc.n)-service UP"
        } else {
            Write-Warning "$($svc.n)-service status: $($health.status)"
            $servicesOk = $false
        }
    } catch {
        Write-Warning "$($svc.n)-service non disponible sur $($svc.u)"
        $servicesOk = $false
    }
}
if (-not $servicesOk) {
    Write-Error "Services non disponibles. Lancez d'abord : docker compose -f infra/docker-compose.yml up --build"
    exit 1
}

# --- Envoi de la carte d'introduction ----------------------------------------

Write-Host ""
Write-Host "==> Demarrage de la demo ChatOps dans Teams"
Write-Host "    Webhook: $($WebhookUrl.Substring(0, [Math]::Min(60, $WebhookUrl.Length)))..."
Write-Host ""

Send-TeamsCard -Title "🚀 Observability Platform — ChatOps" `
    -Text "Session de supervision interactive demarree. Tapez une commande pour interagir avec la plateforme d'observabilite." `
    -Color "6264A7" `
    -Facts @(
        @{ name = "Plateforme"; value = "Observability Platform v1.0" }
        @{ name = "Services"; value = "monitoring (8081) | incident (8082) | chatops (8083)" }
    )
Start-Sleep -Seconds $DelayBetweenSec

# --- Sequence de commandes ---------------------------------------------------

# 1. help
Write-Host "  [1/5] help"
Send-UserCommand "help"
$resp = Invoke-ChatOps "help"
Send-BotResponse "help" $resp
Start-Sleep -Seconds $DelayBetweenSec

# 2. stats
Write-Host "  [2/5] stats"
Send-UserCommand "stats"
$resp = Invoke-ChatOps "stats"
Send-BotResponse "stats" $resp
Start-Sleep -Seconds $DelayBetweenSec

# 3. list
Write-Host "  [3/5] list"
Send-UserCommand "list"
$resp = Invoke-ChatOps "list"
Send-BotResponse "list" $resp
Start-Sleep -Seconds $DelayBetweenSec

# 4. ack — premier incident ouvert
Write-Host "  [4/5] ack (premier incident ouvert)"
try {
    $incidents = @(Invoke-RestMethod -Uri "$IncidentUrl/api/incidents?status=OPEN" -TimeoutSec 10)
    if ($incidents.Count -gt 0) {
        $targetId = $incidents[0].id
        $cmd = "ack $targetId"
        Send-UserCommand $cmd
        $resp = Invoke-ChatOps $cmd
        Send-BotResponse $cmd $resp
    } else {
        Write-Host "    Aucun incident OPEN, skip ack"
        Send-TeamsCard -Title "🤖 Observability Bot" `
            -Text "Aucun incident ouvert a acquitter." -Color "FFAA00"
    }
} catch {
    Write-Warning "    Erreur listing incidents : $($_.Exception.Message)"
}
Start-Sleep -Seconds $DelayBetweenSec

# 5. resolve — premier incident ACKNOWLEDGED
Write-Host "  [5/5] resolve (premier incident acquitte)"
try {
    $incidents = @(Invoke-RestMethod -Uri "$IncidentUrl/api/incidents?status=ACKNOWLEDGED" -TimeoutSec 10)
    if ($incidents.Count -gt 0) {
        $targetId = $incidents[0].id
        $cmd = "resolve $targetId"
        Send-UserCommand $cmd
        $resp = Invoke-ChatOps $cmd
        Send-BotResponse $cmd $resp
    } else {
        Write-Host "    Aucun incident ACKNOWLEDGED, skip resolve"
        Send-TeamsCard -Title "🤖 Observability Bot" `
            -Text "Aucun incident acquitte a resoudre." -Color "FFAA00"
    }
} catch {
    Write-Warning "    Erreur listing incidents : $($_.Exception.Message)"
}
Start-Sleep -Seconds $DelayBetweenSec

# --- Carte de fin ------------------------------------------------------------
Send-TeamsCard -Title "✅ Demo ChatOps terminee" `
    -Text "Toutes les commandes ont ete executees. La plateforme d'observabilite est operationnelle." `
    -Color "00AA00" `
    -Facts @(
        @{ name = "Commandes executees"; value = "help, stats, list, ack, resolve" }
        @{ name = "Dashboard"; value = "http://localhost:8090" }
    )

Write-Host ""
Write-Host "================ DEMO TERMINEE ================"
Write-Host "Les messages ont ete postes dans votre canal Teams."
Write-Host "Vous pouvez maintenant faire des captures d'ecran du canal."
