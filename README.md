# Observability Platform

> Plateforme d'observabilité centralisée — ingestion de logs, détection d'anomalies, gestion d'incidents et notifications automatisées.

**Projet de stage M2 MBDS** — Développé pour [Value IT](https://valueit.mg)

---

## Table des matières

- [Architecture](#architecture)
- [Stack technique](#stack-technique)
- [Prérequis](#prérequis)
- [Démarrage rapide](#démarrage-rapide)
- [API Endpoints](#api-endpoints)
- [Pipeline de traitement](#pipeline-de-traitement)
- [Frontend Dashboard](#frontend-dashboard)
- [ChatOps](#chatops)
- [Tests](#tests)
- [Évaluation](#évaluation)
- [CI/CD](#cicd)
- [Structure du projet](#structure-du-projet)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Sources de logs                          │
│              (applications, serveurs, services)                 │
└──────────────────────────┬──────────────────────────────────────┘
                           │ POST /api/logs/raw[/bulk]
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Moteur de Parsing                           │
│         JsonLogParser │ SyslogParser │ ApacheLogParser          │
└──────────────────────────┬──────────────────────────────────────┘
                           │ LogEntry
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Détection d'anomalies                          │
│  ErrorRateDetector │ KeywordDetector │ StackTraceDetector       │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Anomaly
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    IncidentService                              │
│        Corrélation (fingerprint) │ Déduplication               │
│            Escalade de sévérité │ Persistance ES               │
└──────────────┬──────────────────────────┬───────────────────────┘
               │                          │
               ▼                          ▼
┌──────────────────────┐    ┌─────────────────────────────────────┐
│    NotificationHub   │    │         AuditService (JPA)          │
│  ┌────────────────┐  │    │     audit_log + notification_record │
│  │ TeamsNotifier   │  │    └─────────────────────────────────────┘
│  │ JiraNotifier    │  │
│  └────────────────┘  │
└──────────────────────┘
```

---

## Stack technique

| Couche | Technologie |
|--------|-------------|
| **Backend** | Java 21, Spring Boot 4.1.0, Maven |
| **Stockage logs/incidents** | Elasticsearch 9.0.0 |
| **Métadonnées (audit, notifications)** | JPA — H2 (dev) / PostgreSQL 16 (Docker) |
| **Frontend** | React 19, Vite 8, Tailwind CSS 4, Recharts, Lucide |
| **Documentation API** | springdoc-openapi (Swagger UI) |
| **Métriques** | Spring Boot Actuator + Micrometer → Prometheus |
| **CI/CD** | GitHub Actions |
| **Conteneurisation** | Docker, Docker Compose |

---

## Prérequis

- **Java 21** (JDK)
- **Node.js 20+** (pour le frontend)
- **Docker & Docker Compose** (pour l'infrastructure)
- **Maven** (inclus via `mvnw`)

---

## Démarrage rapide

### Option 1 : Docker Compose (recommandé)

Lance l'ensemble de la stack (Elasticsearch, PostgreSQL, backend, frontend, Prometheus) :

```bash
docker compose -f infra/docker-compose.yml up --build
```

| Service | URL |
|---------|-----|
| Frontend (Dashboard) | http://localhost:8090 |
| Backend API | http://localhost:8082 |
| Swagger UI | http://localhost:8082/swagger-ui.html |
| Elasticsearch | http://localhost:9200 |
| Prometheus | http://localhost:9091 |

### Option 2 : Développement local

**1. Démarrer Elasticsearch :**

```bash
docker compose -f infra/docker-compose.yml up elasticsearch -d
```

**2. Démarrer le backend :**

```bash
cd backend/core
./mvnw spring-boot:run
```

Le backend démarre sur le port `8082` avec H2 en mémoire (pas besoin de PostgreSQL en dev).

**3. Démarrer le frontend :**

```bash
cd frontend/dashboard
npm install
npm run dev
```

Le frontend Vite démarre sur `http://localhost:5173` avec proxy vers le backend.

---

## API Endpoints

Port par défaut : **8082**

### Ingestion de logs

| Méthode | URL | Description |
|---------|-----|-------------|
| `POST` | `/api/logs/raw` | Ingérer un log brut (auto-parse + détection) |
| `POST` | `/api/logs/raw/bulk` | Ingérer des logs en lot (`text/plain`, 1 ligne = 1 log) |
| `POST` | `/api/logs` | Ingérer un log structuré (JSON) |
| `POST` | `/api/logs/bulk` | Ingérer plusieurs logs structurés |

### Consultation de logs

| Méthode | URL | Description |
|---------|-----|-------------|
| `GET` | `/api/logs?page=0&size=50` | Lister les logs (paginé, tri par timestamp desc) |
| `GET` | `/api/logs/level/{level}` | Filtrer par niveau (ERROR, WARN, INFO, DEBUG) |
| `GET` | `/api/logs/search?from=...&to=...&level=...` | Recherche temporelle |

### Gestion des incidents

| Méthode | URL | Description |
|---------|-----|-------------|
| `GET` | `/api/incidents` | Lister les incidents (filtre `?status=` `?severity=`) |
| `GET` | `/api/incidents/{id}` | Détail d'un incident |
| `PUT` | `/api/incidents/{id}/ack` | Acquitter (OPEN → ACKNOWLEDGED) |
| `PUT` | `/api/incidents/{id}/resolve` | Résoudre (→ RESOLVED) |
| `GET` | `/api/incidents/stats` | Statistiques par statut |

### ChatOps & Monitoring

| Méthode | URL | Description |
|---------|-----|-------------|
| `POST` | `/api/chatops` | Commande ChatOps (`list`, `stats`, `ack <id>`, `resolve <id>`) |
| `GET` | `/actuator/health` | Health check |
| `GET` | `/actuator/prometheus` | Métriques Prometheus |
| `GET` | `/swagger-ui.html` | Documentation interactive |

---

## Pipeline de traitement

```
Log brut (texte) ──→ LogParsingService
                        ├─ JsonLogParser     (logs JSON)
                        ├─ SyslogParser      (RFC 3164, <PRI> optionnel)
                        └─ ApacheLogParser   (access log Apache)
                              │
                              ▼ LogEntry (sauvegardé dans ES)
                     AnomalyDetectionService
                        ├─ ErrorRateAnomalyDetector   (fenêtre glissante 5min, seuil 30%)
                        ├─ KeywordAnomalyDetector      (OOM, deadlock, timeout, etc.)
                        └─ StackTraceAnomalyDetector   (extraction root cause)
                              │
                              ▼ Anomaly
                     IncidentService.handle()
                        ├─ Fingerprint = type::source (déduplication)
                        ├─ Nouvel incident → OPEN + notification
                        └─ Récurrence → occurrenceCount++ + escalade sévérité
                              │
                              ▼
                     NotificationHub (@Async)
                        ├─ TeamsNotifier   (webhook, CREATED uniquement)
                        └─ JiraNotifier    (HIGH/CRITICAL: crée ticket, commente récurrences)
```

### Sévérités

`LOW` → `MEDIUM` → `HIGH` → `CRITICAL` (escalade automatique via `Severity.max()`)

### Cycle de vie des incidents

`OPEN` → `ACKNOWLEDGED` → `RESOLVED`

---

## Frontend Dashboard

SPA React avec 5 pages :

- **Dashboard** — KPI cards (total logs, incidents, ouverts, résolus) + graphiques de répartition
- **Logs** — Tableau paginé avec recherche temporelle et filtre par niveau
- **Incidents** — Cartes avec badges sévérité/statut, actions ACK et Résoudre, filtres
- **ChatOps** — Terminal interactif pour les commandes `list`, `stats`, `ack`, `resolve`
- **Audit** — Journal d'audit et historique des notifications (filtres par action/canal)

---

## ChatOps

Interface conversationnelle via `POST /api/chatops` :

| Commande | Description |
|----------|-------------|
| `list` | Liste les incidents ouverts |
| `stats` | Statistiques par statut |
| `ack <id>` | Acquitter un incident |
| `resolve <id>` | Résoudre un incident |
| `help` | Afficher les commandes disponibles |

---

## Tests

60 tests unitaires (pas besoin d'Elasticsearch) :

```bash
cd backend/core
./mvnw test
```

| Suite de tests | Tests |
|----------------|-------|
| `JsonLogParserTest` | 6 — parsing JSON, cas limites |
| `ApacheLogParserTest` | 6 — parsing Apache, niveaux HTTP |
| `SyslogParserTest` | 7 — parsing Syslog, sévérités 0-7 |
| `StackTraceAnomalyDetectorTest` | 8 — NPE, CausedBy, *Error, casse libre, sévérités |
| `KeywordAnomalyDetectorTest` | 7 — OOM, deadlock, timeout |
| `ErrorRateAnomalyDetectorTest` | 5 — seuil, taux 30%/50%+, isolation par source |
| `SeverityTest` | 4 — enum max(), ordinal |
| `IncidentServiceTest` | 9 — création, récurrence (OPEN/ACK), ACK, resolve, gardes d'état |
| `AuditLogIntegrationTest` | Tests d'intégration audit |
| `NotificationRecordIntegrationTest` | Tests d'intégration notifications |
| `PlatformApplicationTests`, `SampleDataParsingTest` | Contexte Spring, parsing des fichiers sample |

---

## Évaluation

Campagne d'évaluation automatisée — injection de logs samples, comparaison avec un ground truth annoté :

```powershell
# Windows (PowerShell)
.\scripts\run_evaluation.ps1

# Linux / macOS
./scripts/run_evaluation.sh
```

**Données :** `data/sample/*.log` (Apache, JSON, Syslog)
**Annotations :** `data/annotations/ground_truth.csv`
**Résultats :** `results/eval-<timestamp>/` (summary.json, detection.csv, ingestion.csv, prometheus.txt)

Métriques calculées : **Précision**, **Rappel**, **F1-score**

---

## CI/CD

Pipeline GitHub Actions (`.github/workflows/ci.yml`) :

1. **Backend** — Compile + tests (`mvnw verify`)
2. **Frontend** — Build (`npm ci && npm run build`)
3. **Docker** — Validation du compose (`docker compose config`)

Déclenché sur push/PR vers `main` et branches `sprint-*`.

---

## Structure du projet

```
observability-platform/
├── backend/core/                          # Backend Spring Boot
│   ├── src/main/java/.../platform/
│   │   ├── PlatformApplication.java       # Point d'entrée
│   │   ├── controller/                    # REST controllers (Logs, Incidents, ChatOps, Metadata)
│   │   ├── service/                       # Logique métier (Parsing, Detection, Incidents, ChatOps)
│   │   ├── parser/                        # Parsers (JSON, Syslog, Apache)
│   │   ├── anomaly/                       # Détecteurs (ErrorRate, Keyword, StackTrace)
│   │   ├── incident/                      # Modèle Incident + enums
│   │   ├── notification/                  # NotificationHub + Notifiers (Teams, Jira)
│   │   ├── metadata/                      # Audit + NotificationRecord (JPA)
│   │   ├── metrics/                       # PlatformMetrics (Prometheus counters)
│   │   ├── model/                         # LogEntry (ES document)
│   │   └── repository/                    # Spring Data repositories
│   ├── src/main/resources/
│   │   └── application.yaml               # Configuration externalisée
│   ├── src/test/                           # 60 tests unitaires
│   ├── Dockerfile                          # Multi-stage (JDK build → JRE runtime)
│   └── pom.xml
├── frontend/dashboard/                     # Frontend React
│   ├── src/
│   │   ├── App.jsx                        # SPA + sidebar + router
│   │   ├── api.js                         # Client API
│   │   └── pages/                         # Dashboard, Logs, Incidents, ChatOps, Audit
│   ├── Dockerfile                          # Multi-stage (Node build → nginx)
│   ├── nginx.conf                          # SPA fallback + API proxy
│   └── package.json
├── infra/
│   ├── docker-compose.yml                 # ES + PostgreSQL + Backend + Frontend + Prometheus
│   └── prometheus.yml                     # Config scraping
├── data/
│   ├── sample/                            # Fichiers de logs pour évaluation
│   └── annotations/ground_truth.csv       # Vérité terrain
├── scripts/
│   ├── run_evaluation.ps1                 # Script d'évaluation (Windows)
│   └── run_evaluation.sh                  # Script d'évaluation (Linux/macOS)
├── docs/                                  # Documentation technique
├── .github/workflows/ci.yml              # Pipeline CI
└── README.md
```

---

## Variables d'environnement

| Variable | Défaut | Description |
|----------|--------|-------------|
| `SPRING_ELASTICSEARCH_URIS` | `http://localhost:9200` | URL Elasticsearch |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:mem:obsdb` | URL base de données JPA |
| `SERVER_PORT` | `8082` | Port du backend |
| `TEAMS_WEBHOOK_URL` | (placeholder) | Webhook Microsoft Teams |
| `JIRA_BASE_URL` | (placeholder) | URL instance Jira |
| `JIRA_EMAIL` | — | Email compte Jira |
| `JIRA_API_TOKEN` | — | Token API Jira |
| `JIRA_PROJECT_KEY` | `OPS` | Clé projet Jira |

---

## Licence

MIT