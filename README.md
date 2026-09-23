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

Architecture **microservices** — trois services Spring Boot indépendants communiquant par REST :

```
┌─────────────────────────────────────────────────────────────────┐
│                        Sources de logs                          │
│              (applications, serveurs, services)                 │
└──────────────────────────┬──────────────────────────────────────┘
                           │ POST /api/logs/raw[/bulk]
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│              monitoring-service  (port 8081)                    │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Moteur de Parsing                                        │  │
│  │  JsonLogParser │ SyslogParser │ ApacheLogParser           │  │
│  └──────────────────────────┬────────────────────────────────┘  │
│                             ▼ LogEntry → Elasticsearch          │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Détection d'anomalies                                    │  │
│  │  ErrorRateDetector │ KeywordDetector │ StackTraceDetector │  │
│  └──────────────────────────┬────────────────────────────────┘  │
└─────────────────────────────┼───────────────────────────────────┘
                              │ AnomalyReport (REST)
                              ▼ POST /internal/anomalies
┌─────────────────────────────────────────────────────────────────┐
│               incident-service  (port 8082)                     │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  IncidentService : corrélation (fingerprint),             │  │
│  │  déduplication, escalade de sévérité → Elasticsearch      │  │
│  └──────────────┬──────────────────────────┬─────────────────┘  │
│                 ▼                          ▼                    │
│  ┌──────────────────────┐    ┌───────────────────────────────┐  │
│  │   NotificationHub    │    │      AuditService (JPA)       │  │
│  │  TeamsNotifier       │    │  audit_log + notif_record     │  │
│  │  JiraNotifier        │    │  → H2 / PostgreSQL            │  │
│  └──────────────────────┘    └───────────────────────────────┘  │
└─────────────────────────────▲───────────────────────────────────┘
                              │ REST /api/incidents
┌─────────────────────────────┴───────────────────────────────────┐
│               chatops-service  (port 8083)                      │
│   POST /api/chatops — commandes list, stats, ack, resolve       │
│   déléguées à incident-service via IncidentApiClient            │
└─────────────────────────────────────────────────────────────────┘
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
| monitoring-service | http://localhost:8081 |
| incident-service | http://localhost:8082 |
| chatops-service | http://localhost:8083 |
| Swagger UI | http://localhost:8081/swagger-ui.html (par service) |
| Elasticsearch | http://localhost:9200 |
| Prometheus | http://localhost:9091 |

### Option 2 : Développement local

**1. Démarrer Elasticsearch :**

```bash
docker compose -f infra/docker-compose.yml up elasticsearch -d
```

**2. Démarrer les services backend** (depuis `backend/`, un terminal par service) :

```bash
./mvnw spring-boot:run -pl monitoring-service   # port 8081
./mvnw spring-boot:run -pl incident-service     # port 8082 (H2 en mémoire en dev)
./mvnw spring-boot:run -pl chatops-service      # port 8083
```

**3. Démarrer le frontend :**

```bash
cd frontend/dashboard
npm install
npm run dev
```

Le frontend Vite démarre sur `http://localhost:5173` avec proxy vers le backend.

---

## API Endpoints

### monitoring-service — port **8081** (logs)

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

### incident-service — port **8082** (incidents, audit, notifications)

| Méthode | URL | Description |
|---------|-----|-------------|
| `GET` | `/api/incidents` | Lister les incidents (filtre `?status=` `?severity=`) |
| `GET` | `/api/incidents/{id}` | Détail d'un incident |
| `PUT` | `/api/incidents/{id}/ack` | Acquitter (OPEN → ACKNOWLEDGED) |
| `PUT` | `/api/incidents/{id}/resolve` | Résoudre (→ RESOLVED) |
| `GET` | `/api/incidents/stats` | Statistiques par statut |

### chatops-service — port **8083** & endpoints transverses

| Méthode | URL | Description |
|---------|-----|-------------|
| `POST` | `/api/chatops` | Commande ChatOps (`list`, `stats`, `ack <id>`, `resolve <id>`) |
| `GET` | `/actuator/health` | Health check (chaque service) |
| `GET` | `/actuator/prometheus` | Métriques Prometheus (chaque service) |
| `GET` | `/swagger-ui.html` | Documentation interactive (chaque service) |

> Endpoint interne : `POST /internal/anomalies` sur incident-service (appelé par monitoring-service, masqué dans Swagger).

---

## Pipeline de traitement

```
monitoring-service (8081)
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
                                ▼ AnomalyReport
                       IncidentClient ──REST──→ POST /internal/anomalies
─────────────────────────────────────────────────────────────────
incident-service (8082)
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

Tests répartis par module (unitaires, pas besoin d'Elasticsearch) :

```bash
cd backend
./mvnw test
```

| Module | Suites de tests |
|--------|-----------------|
| `monitoring-service` | `JsonLogParserTest`, `ApacheLogParserTest`, `SyslogParserTest`, `StackTraceAnomalyDetectorTest`, `KeywordAnomalyDetectorTest`, `ErrorRateAnomalyDetectorTest`, `SampleDataParsingTest` |
| `incident-service` | `IncidentServiceTest` (création, récurrence, ACK, resolve, gardes d'état), `SeverityTest`, `AuditLogIntegrationTest`, `NotificationRecordIntegrationTest` |
| `chatops-service` | `ChatOpsServiceTest` (dispatch, help, commandes inconnues) |

Les tests `@SpringBootTest` sont ignorés par défaut — activer avec `INTEGRATION_TESTS=true`.

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
├── backend/                                # Maven multi-module (parent pom.xml + mvnw)
│   ├── common/                             # Contrats partagés : LogEntry, Incident, enums,
│   │                                       #   AnomalyReport (DTO inter-services), ElasticsearchSslConfig
│   ├── monitoring-service/                 # Port 8081 — ingestion, parsing, détection d'anomalies
│   │   ├── src/main/java/.../platform/
│   │   │   ├── MonitoringApplication.java
│   │   │   ├── controller/                # LogIngestionController, GlobalExceptionHandler
│   │   │   ├── service/                   # LogParsingService, AnomalyDetectionService, IncidentClient
│   │   │   ├── parser/                    # Parsers (JSON, Syslog, Apache)
│   │   │   ├── anomaly/                   # Détecteurs (ErrorRate, Keyword, StackTrace)
│   │   │   ├── metrics/                   # PlatformMetrics (logs, anomalies)
│   │   │   └── repository/                # LogEntryRepository (ES)
│   │   └── Dockerfile
│   ├── incident-service/                   # Port 8082 — incidents, dédup, notifications, audit
│   │   ├── src/main/java/.../platform/
│   │   │   ├── IncidentApplication.java
│   │   │   ├── controller/                # IncidentController, InternalAnomalyController, AuditController
│   │   │   ├── service/                   # IncidentService (corrélation fingerprint)
│   │   │   ├── notification/              # NotificationHub + TeamsNotifier + JiraNotifier
│   │   │   ├── metadata/                  # AuditLog + NotificationRecord (JPA)
│   │   │   ├── metrics/                   # PlatformMetrics (incidents, notifications)
│   │   │   └── repository/                # IncidentRepository (ES)
│   │   └── Dockerfile
│   ├── chatops-service/                    # Port 8083 — interface conversationnelle
│   │   ├── src/main/java/.../platform/
│   │   │   ├── ChatOpsApplication.java
│   │   │   ├── controller/                # ChatOpsController
│   │   │   ├── service/                   # ChatOpsService, IncidentApiClient (REST)
│   │   │   └── chatops/                   # Commandes : list, stats, ack, resolve
│   │   └── Dockerfile
│   └── api-tests.http                      # Requêtes de test (ports 8081/8082/8083)
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

| Variable | Défaut | Service | Description |
|----------|--------|---------|-------------|
| `SPRING_ELASTICSEARCH_URIS` | `http://localhost:9200` | monitoring, incident | URL Elasticsearch |
| `INCIDENT_SERVICE_URL` | `http://localhost:8082` | monitoring, chatops | URL de incident-service |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:mem:obsdb` | incident | URL base de données JPA |
| `SERVER_PORT` | `8081`/`8082`/`8083` | tous | Port du service |
| `TEAMS_WEBHOOK_URL` | (placeholder) | incident | Webhook Microsoft Teams |
| `JIRA_BASE_URL` | (placeholder) | incident | URL instance Jira |
| `JIRA_EMAIL` | — | incident | Email compte Jira |
| `JIRA_API_TOKEN` | — | incident | Token API Jira |
| `JIRA_PROJECT_KEY` | `OPS` | incident | Clé projet Jira |

---

## Licence

MIT