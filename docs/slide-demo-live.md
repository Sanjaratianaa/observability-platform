# Slides de Démo Live — Soutenance

Ce document décrit les slides à préparer pour la **démonstration en direct** le jour de la soutenance.
Tu peux créer ces slides au bureau (PowerPoint/Google Slides) — les captures viendront après.

---

## Slide 1 — Titre

> **Démonstration — Plateforme d'Observabilité**
>
> Supervision proactive des systèmes distribués
>
> [Ton nom] — MBDS M2 — 2026

---

## Slide 2 — Architecture déployée

> **8 conteneurs Docker Compose**

Schéma simplifié (à dessiner dans PowerPoint) :

```
┌─────────────┐     ┌──────────────────┐     ┌──────────────────┐
│  Frontend   │────▶│ monitoring-svc   │────▶│  incident-svc    │
│  React/Nginx│     │ (parse, detect)  │     │ (correl, notif)  │
│   :8090     │     │     :8085        │     │     :8082        │
└─────────────┘     └───────┬──────────┘     └──┬───────┬───────┘
                            │                   │       │
┌─────────────┐     ┌───────▼──────────┐   ┌────▼──┐ ┌──▼─────┐
│ chatops-svc │     │  Elasticsearch   │   │ Jira  │ │ Teams  │
│    :8083    │     │     :9200        │   └───────┘ └────────┘
└─────────────┘     └──────────────────┘
                    ┌──────────────────┐     ┌──────────────────┐
                    │   PostgreSQL     │     │ Prometheus:9091  │
                    │     :5432        │     │ Grafana:3000     │
                    └──────────────────┘     └──────────────────┘
```

**Parler** : "Voici notre infrastructure complète lancée en un seul `docker compose up`. 
3 microservices Java/Spring Boot, un frontend React, Elasticsearch, PostgreSQL, 
Prometheus et Grafana."

---

## Slide 3 — Vérification de l'infrastructure

> **Étape 1 : Tous les services sont opérationnels**

📸 *Placeholder : capture `docker ps` montrant les 8 containers UP*

**Action live** : Montrer le terminal avec `docker ps`

**Parler** : "Les 8 conteneurs sont démarrés et healthy. L'Elasticsearch est en yellow, 
ce qui est normal pour un nœud unique."

---

## Slide 4 — Dashboard (état initial)

> **Étape 2 : Interface web — Vue d'ensemble**

📸 *Placeholder : capture Dashboard http://localhost:8090*

**Action live** : Ouvrir le navigateur → `http://localhost:8090`

**Parler** : "Notre dashboard React affiche en temps réel les KPI : nombre total de logs, 
incidents créés, ouverts et résolus, avec des graphiques de répartition."

---

## Slide 5 — Injection d'un log anomal

> **Étape 3 : Simulation d'une anomalie**

Commande à exécuter en live :

```powershell
# Log avec stack trace Java
Invoke-RestMethod -Uri "http://localhost:8085/api/logs/raw?source=demo-live" `
  -Method Post -ContentType "text/plain; charset=utf-8" `
  -Body '{"timestamp":"2026-09-29T10:00:00Z","level":"ERROR","service":"demo-live","message":"java.lang.NullPointerException at com.example.Service.process(Service.java:42)"}'
```

**Parler** : "J'envoie un log contenant une stack trace Java. Le monitoring-service va 
le parser, détecter l'anomalie, et transmettre un rapport à l'incident-service."

---

## Slide 6 — Pipeline automatique

> **Étape 4 : Détection → Incident → Notification**

Schéma du flux (flèches animées si possible) :

```
Log ERROR  →  Parse JSON  →  StackTraceDetector  →  AnomalyReport
                                                         │
                                                    fingerprint
                                                  "STACK_TRACE::demo-live"
                                                         │
                                                  Incident OPEN créé
                                                         │
                                               ┌─────────┴─────────┐
                                          Teams webhook       Jira ticket
                                          (Adaptive Card)     (si HIGH+)
```

**Parler** : "En quelques millisecondes : parsing → détection par le StackTraceDetector → 
calcul du fingerprint → création de l'incident → notifications automatiques."

---

## Slide 7 — Page Logs

> **Étape 5 : Consultation des logs ingérés**

📸 *Placeholder : capture page Logs*

**Action live** : Cliquer sur "Logs" dans le menu

**Parler** : "La page Logs montre tous les logs ingérés avec recherche temporelle 
et filtrage par niveau (INFO, WARN, ERROR)."

---

## Slide 8 — Page Incidents

> **Étape 6 : L'incident a été créé automatiquement**

📸 *Placeholder : capture page Incidents avec l'incident créé*

**Action live** : Cliquer sur "Incidents" → montrer l'incident OPEN avec badge HIGH

**Parler** : "L'incident a été créé automatiquement avec le type STACK_TRACE_EXCEPTION, 
la sévérité HIGH, et le statut OPEN. Le fingerprint empêche les doublons."

---

## Slide 9 — Déduplication en action

> **Étape 7 : Ré-injection du même log → pas de doublon**

**Action live** : Ré-envoyer le même log (même commande que slide 5), puis rafraîchir Incidents

```powershell
# Même log → même fingerprint → même incident mis à jour
Invoke-RestMethod -Uri "http://localhost:8085/api/logs/raw?source=demo-live" `
  -Method Post -ContentType "text/plain; charset=utf-8" `
  -Body '{"timestamp":"2026-09-29T10:05:00Z","level":"ERROR","service":"demo-live","message":"java.lang.NullPointerException at com.example.Service.process(Service.java:42)"}'
```

**Parler** : "J'envoie la même erreur. Pas de nouvel incident : le compteur d'occurrences 
passe à 2. C'est la déduplication par fingerprint dans Elasticsearch."

---

## Slide 10 — Cycle de vie : Acquitter

> **Étape 8 : Gestion de l'incident — Acquittement**

📸 *Placeholder : capture incident ACKNOWLEDGED*

**Action live** : Cliquer sur le bouton "Acquitter" sur l'incident

**Parler** : "L'opérateur acquitte l'incident pour signaler qu'il est en cours de traitement. 
Le statut passe de OPEN à ACKNOWLEDGED. L'action est tracée dans l'audit."

---

## Slide 11 — Cycle de vie : Résoudre

> **Étape 9 : Résolution de l'incident**

📸 *Placeholder : capture incident RESOLVED*

**Action live** : Cliquer sur le bouton "Résoudre"

**Parler** : "L'incident est résolu. Un commentaire est automatiquement ajouté au ticket Jira. 
Si une nouvelle occurrence arrive, un nouvel incident sera créé — pas de réouverture."

---

## Slide 12 — ChatOps

> **Étape 10 : Interface conversationnelle**

📸 *Placeholder : capture page ChatOps*

**Action live** : Cliquer sur "ChatOps", taper :
- `help` → commandes disponibles
- `stats` → comptage par statut
- `list` → incidents ouverts

**Parler** : "L'interface ChatOps permet de superviser sans quitter le terminal. 
Les commandes list, stats, ack et resolve couvrent le cycle complet."

---

## Slide 13 — Audit & Traçabilité

> **Étape 11 : Journal d'audit complet**

📸 *Placeholder : capture page Audit*

**Action live** : Cliquer sur "Audit"

**Parler** : "Chaque action est tracée : création, acquittement, résolution. 
L'onglet Notifications montre l'historique des envois vers Jira et Teams, 
y compris les erreurs éventuelles."

---

## Slide 14 — Métriques & Observabilité

> **Étape 12 : La plateforme se supervise elle-même**

📸 *Placeholder : captures Prometheus + Grafana*

**Action live** : 
- Ouvrir `http://localhost:9091` → taper `obs_incidents` dans la barre
- Ouvrir `http://localhost:3000` → dashboard Grafana

**Parler** : "Chaque microservice expose des métriques Prometheus. Grafana les visualise. 
La plateforme d'observabilité est elle-même observable — c'est la méta-observabilité."

---

## Slide 15 — Swagger API

> **Étape 13 : Documentation API auto-générée**

📸 *Placeholder : capture Swagger UI*

**Action live** : Ouvrir `http://localhost:8085/swagger-ui.html`

**Parler** : "Chaque service expose une documentation Swagger auto-générée via springdoc-openapi. 
Tous les endpoints sont testables directement depuis l'interface."

---

## Slide 16 — Résultats d'évaluation

> **Précision / Rappel / F1**

📸 *Placeholder : résultats run_evaluation.ps1*

**Parler** : "Notre campagne d'évaluation injecte des logs annotés, compare les incidents 
détectés au ground truth, et calcule précision, rappel et F1-score par détecteur."

---

## Slide 17 — Fin de démo

> **Récapitulatif du flux démontré**
>
> ✅ Injection de logs → Parsing automatique → Détection d'anomalies
> ✅ Création d'incident → Déduplication par fingerprint
> ✅ Notifications Jira + Teams → Audit trail complet
> ✅ Interface web (5 pages) + ChatOps + API documentée
> ✅ Méta-observabilité (Prometheus + Grafana)
>
> **Tout est déployable en une commande : `docker compose up -d`**

---

## Commandes à préparer dans un fichier (copier-coller le jour J)

Crée un fichier `demo-commands.ps1` sur le Bureau pour le jour J :

```powershell
# === DEMO SOUTENANCE ===
# 1. Vérifier l'infra
docker ps --format "table {{.Names}}\t{{.Status}}"

# 2. Log normal
Invoke-RestMethod -Uri "http://localhost:8085/api/logs/raw?source=demo-live" -Method Post -ContentType "text/plain; charset=utf-8" -Body '{"timestamp":"2026-09-29T10:00:00Z","level":"INFO","service":"demo-live","message":"Application started successfully"}'

# 3. Log avec stack trace (crée un incident)
Invoke-RestMethod -Uri "http://localhost:8085/api/logs/raw?source=demo-live" -Method Post -ContentType "text/plain; charset=utf-8" -Body '{"timestamp":"2026-09-29T10:01:00Z","level":"ERROR","service":"demo-live","message":"java.lang.NullPointerException at com.example.Service.process(Service.java:42)"}'

# 4. Log avec timeout (crée un 2e incident)
Invoke-RestMethod -Uri "http://localhost:8085/api/logs/raw?source=demo-live" -Method Post -ContentType "text/plain; charset=utf-8" -Body '{"timestamp":"2026-09-29T10:02:00Z","level":"ERROR","service":"demo-live","message":"connection timeout to database server after 30000ms"}'

# 5. Ré-injection stack trace (déduplication)
Invoke-RestMethod -Uri "http://localhost:8085/api/logs/raw?source=demo-live" -Method Post -ContentType "text/plain; charset=utf-8" -Body '{"timestamp":"2026-09-29T10:05:00Z","level":"ERROR","service":"demo-live","message":"java.lang.NullPointerException at com.example.Service.process(Service.java:99)"}'

# 6. ChatOps depuis le terminal (optionnel)
Invoke-RestMethod -Uri "http://localhost:8083/api/chatops" -Method Post -Body "stats" -ContentType "text/plain; charset=utf-8"
```

## Timing estimé

| Slide | Durée | Cumulé |
|-------|-------|--------|
| 1-2 Titre + Archi | 30s | 0:30 |
| 3 Docker ps | 20s | 0:50 |
| 4 Dashboard | 20s | 1:10 |
| 5-6 Injection + Pipeline | 40s | 1:50 |
| 7 Logs | 20s | 2:10 |
| 8 Incidents | 30s | 2:40 |
| 9 Déduplication | 30s | 3:10 |
| 10-11 Ack + Resolve | 30s | 3:40 |
| 12 ChatOps | 30s | 4:10 |
| 13 Audit | 20s | 4:30 |
| 14-15 Métriques + Swagger | 30s | 5:00 |
| 16 Évaluation | 20s | 5:20 |
| 17 Récap | 10s | 5:30 |

**Total : ~5min30** — parfait pour une démo de soutenance.
