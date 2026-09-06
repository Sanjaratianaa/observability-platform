# Observability Platform — Progress

## Dernière session : 6 septembre 2026

### Sprint 1 — Connecteur ES (fait)
- [x] Projet Spring Boot 4.1.0 initialisé (Java 21, Maven)
- [x] Connexion Elasticsearch 9.x validée (HTTPS + auth)
- [x] Config SSL dev (`ElasticsearchSslConfig`) — trust-all pour cert auto-signé
- [x] Modèle `LogEntry` (@Document, index "logs")
- [x] `LogEntryRepository` (CRUD auto via Spring Data ES)
- [x] Insert/read d'un document validé au démarrage (`ElasticSearchPingRunner`)
- [x] API REST créée (`LogIngestionController`)

### Moteur de parsing (fait)
- [x] Interface `LogParser` (`canParse` / `parse` / `getName`)
- [x] `LogParsingService` — dispatch vers le premier parser compatible
- [x] `JsonLogParser` — logs JSON → `LogEntry` (couvert par tests)
- [x] `SyslogParser` — format RFC syslog (priorité → niveau)
- [x] `ApacheLogParser` — access log (status code → niveau)
- [x] `LogParserException` pour les erreurs de parsing

### Détection d'anomalies (fait)
- [x] Interface `AnomalyDetector` + `AnomalyDetectionService` (agrège tous les détecteurs)
- [x] `ErrorRateAnomalyDetector` — fenêtre glissante 5 min, seuil 30% (`SlidingWindowCounter`)
- [x] `StackTraceAnomalyDetector` — extraction root cause + localisation
- [x] Modèle `Anomaly` (type, description, sévérité, source)
- [~] `IncidentDeduplicator` — **remplacé** par la corrélation persistée (voir Sprint 2)

### Sprint 2 — Cœur de l'Orchestrateur (fait, à tester)
- [x] Modèle `Incident` persisté (@Document, index "incidents") + `IncidentStatus` (OPEN/ACKNOWLEDGED/RESOLVED)
- [x] `IncidentRepository` — `findByFingerprintAndStatus` pour la corrélation
- [x] **Corrélation d'incidents** — `IncidentService` : fingerprint = type + source
  - nouvel incident → création + notification
  - récurrence → `occurrenceCount++`, `lastSeen`, escalade de sévérité (pas de spam)
- [x] Abstraction **`Notifier`** + **`NotificationHub`** (injection de liste, isolation des pannes)
- [x] **Hub d'alerte adaptatif** — routage par sévérité (`supports()`) + par événement (`IncidentEvent`)
- [x] `TeamsNotifier` = plug-in `Notifier` — n'alerte QUE sur `CREATED` (anti-spam)
- [x] **Intégration Jira** (`JiraNotifier`) — cycle de vie complet :
  - `CREATED` → crée un ticket (HIGH/CRITICAL only) + mémorise `jiraTicketKey`
  - `RECURRED` → commente le ticket existant (idempotent, pas de doublon)
- [x] Controller simplifié → `incidentService.handle(anomaly, log)`

### Sécurité
- [x] Secrets externalisés en variables d'env (Teams, Jira, **et mot de passe ES**)
- [ ] Régénérer le mot de passe ES (l'ancien reste dans l'historique git)

### Fichiers clés
```
backend/core/src/main/java/valueit/observability/platform/
├── PlatformApplication.java              → point d'entrée
├── ElasticSearchPingRunner.java          → test connexion + insert au démarrage
├── ElasticsearchSslConfig.java           → config SSL trust-all (dev)
├── model/LogEntry.java                   → entité log
├── repository/
│   ├── LogEntryRepository.java           → CRUD logs
│   └── IncidentRepository.java           → CRUD incidents + corrélation
├── controller/LogIngestionController.java → API REST ingestion
├── parser/                               → LogParser, LogParsingService, Json/Syslog/Apache
├── anomaly/                              → AnomalyDetector, détecteurs, SlidingWindowCounter
├── incident/                             → Incident, IncidentStatus, IncidentEvent
├── service/                              → LogParsingService, AnomalyDetectionService, IncidentService
└── notification/                         → Notifier, NotificationHub, TeamsNotifier, JiraNotifier
```

### Flux complet
```
Log brut → parse → LogEntry (ES)
        → détecteurs → Anomaly
        → IncidentService.handle()        (corrélation + persistance)
             ├─ nouveau   → save + dispatch(CREATED)
             └─ récurrence → occurrenceCount++ + dispatch(RECURRED)
        → NotificationHub → Notifier.supports()/notify()
             ├─ TeamsNotifier  (CREATED uniquement)
             └─ JiraNotifier   (HIGH/CRITICAL : crée puis commente)
```


### Sprint 2.5 — Améliorations backend (fait)
- [x] Enum `Severity` (LOW → CRITICAL) avec `max()` pour escalade
- [x] `Incident.relatedLogIds` — corrélation incident ↔ logs
- [x] `SLF4J` logging dans tous les services
- [x] `GlobalExceptionHandler` — gestion centralisée des erreurs REST
- [x] Fix `ClassCastException` (saveAll / findAll → StreamSupport)
- [x] Pagination + tri sur `GET /api/logs` (Page + Sort)
- [x] Recherche temporelle `GET /api/logs/search?from=...&to=...&level=...`
- [x] ChatOps : `ChatOpsController` + `ChatOpsService` (commandes : list, stats, ack, resolve)

### Sprint 3 — Interfaces (fait)
- [x] `WebConfig` — CORS activé pour le frontend
- [x] Frontend React (Vite + TailwindCSS) dans `frontend/dashboard/`
- [x] SPA avec sidebar navigation (React Router DOM)
- [x] **Page Dashboard** — KPI cards (logs, incidents, ouverts, résolus) + pie chart (recharts)
- [x] **Page Logs** — tableau paginé avec recherche temporelle + filtre niveau
- [x] **Page Incidents** — cartes avec actions ACK / Résoudre + filtres statut/sévérité
- [x] Proxy Vite → backend (`/api` → `http://localhost:8082`)

### Documentation API (fait)
- [x] Dépendance `springdoc-openapi-starter-webmvc-ui`
- [x] `OpenApiConfig` — métadonnées API (titre, description, version, licence)
- [x] Annotations `@Tag` + `@Operation` + `@Parameter` sur les 3 controllers
- [x] Swagger UI accessible sur `/swagger-ui.html`

### Tests unitaires — 46 tests, 0 failures (fait)
- [x] `JsonLogParserTest` (6 tests) — parsing JSON, cas limites
- [x] `ApacheLogParserTest` (6 tests) — parsing Apache, niveaux HTTP
- [x] `SyslogParserTest` (7 tests) — parsing Syslog, sévérités 0-7
- [x] `StackTraceAnomalyDetectorTest` (5 tests) — NPE, CausedBy, sévérités
- [x] `KeywordAnomalyDetectorTest` (7 tests) — OOM, deadlock, timeout, troncature
- [x] `ErrorRateAnomalyDetectorTest` (4 tests) — seuil min, taux 30%/50%+
- [x] `SeverityTest` (4 tests) — enum max(), ordinal
- [x] `IncidentServiceTest` (6 tests) — création, récurrence, ACK, resolve (Mockito)
- [x] `PlatformApplicationTests` — skip si pas d'ES (`@EnabledIfEnvironmentVariable`)

### Docker + CI/CD (fait)
- [x] `backend/core/Dockerfile` — multi-stage (JDK 21 build → JRE 21 runtime)
- [x] `frontend/dashboard/Dockerfile` — multi-stage (Node 20 build → nginx runtime)
- [x] `frontend/dashboard/nginx.conf` — SPA fallback + proxy API → backend
- [x] `infra/docker-compose.yml` — 3 services (Elasticsearch + backend + frontend) avec healthcheck
- [x] `.github/workflows/ci.yml` — pipeline CI : backend (compile+test) | frontend (build) | docker (validate)
- [x] `application.yaml` externalisé via `${ENV_VAR:default}` pour Docker

### Endpoints API (port 8082)
| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/logs/raw` | Ingérer un log brut (auto-parse + détection + notif) |
| POST | `/api/logs` | Ingérer un log structuré |
| POST | `/api/logs/bulk` | Ingérer plusieurs logs en lot |
| GET | `/api/logs?page=0&size=50` | Lister les logs (paginé) |
| GET | `/api/logs/level/{level}` | Filtrer par niveau |
| GET | `/api/logs/search?from=...&to=...&level=...` | Recherche temporelle |
| GET | `/api/incidents` | Lister les incidents (filtre ?status=&severity=) |
| GET | `/api/incidents/{id}` | Détail d'un incident |
| PUT | `/api/incidents/{id}/ack` | Acquitter un incident |
| PUT | `/api/incidents/{id}/resolve` | Résoudre un incident |
| GET | `/api/incidents/stats` | Nombre d'incidents par statut |
| POST | `/api/chatops` | Commande ChatOps (list, stats, ack, resolve) |
| GET | `/swagger-ui.html` | Documentation API interactive |

### Fichiers clés (mis à jour)
```
backend/core/src/main/java/valueit/observability/platform/
├── PlatformApplication.java
├── ElasticSearchPingRunner.java
├── ElasticsearchSslConfig.java
├── WebConfig.java                        → CORS
├── OpenApiConfig.java                    → Swagger
├── model/LogEntry.java
├── repository/{LogEntry,Incident}Repository.java
├── controller/
│   ├── LogIngestionController.java       → ingestion + recherche
│   ├── IncidentController.java           → CRUD incidents + stats
│   ├── ChatOpsController.java            → commandes ChatOps
│   └── GlobalExceptionHandler.java       → gestion erreurs
├── parser/                               → LogParser, Json/Syslog/Apache
├── anomaly/                              → AnomalyDetector, détecteurs, SlidingWindowCounter
├── incident/                             → Incident, IncidentStatus, IncidentEvent, Severity
├── service/                              → LogParsing, AnomalyDetection, Incident, ChatOps
└── notification/                         → Notifier, NotificationHub, Teams, Jira

frontend/dashboard/src/
├── App.jsx                               → SPA + sidebar + router
├── api.js                                → fonctions API (fetch/search/ack/resolve)
├── pages/
│   ├── Dashboard.jsx                     → stats + pie chart
│   ├── Logs.jsx                          → tableau paginé + recherche
│   └── Incidents.jsx                     → cartes + actions

infra/docker-compose.yml                  → ES + backend + frontend
.github/workflows/ci.yml                  → CI pipeline
```

### Notes techniques
- Variables d'env : `SPRING_ELASTICSEARCH_URIS`, `SPRING_ELASTICSEARCH_USERNAME`, `SPRING_ELASTICSEARCH_PASSWORD`, `SERVER_PORT`, `TEAMS_WEBHOOK_URL`, `JIRA_*`
- Port backend : `8082` (configurable via `SERVER_PORT`)
- Déploiement Docker : `docker compose -f infra/docker-compose.yml up --build`
- Tests : `./mvnw test` (46 tests, pas besoin d'ES)
- Swagger UI : `http://localhost:8082/swagger-ui.html`

### Prochaines étapes
- [ ] Tester le frontend + backend end-to-end
- [ ] Spring Boot Actuator + métriques Prometheus (meta-observabilité)
- [ ] README.md complet avec diagrammes d'architecture
- [ ] Merger sprint-3-interfaces → main
