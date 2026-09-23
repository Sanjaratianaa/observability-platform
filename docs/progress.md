# Observability Platform — Progress

## Dernière session : 23 septembre 2026

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
- [x] `IncidentRepository` — `findFirstByFingerprintAndStatusIn` (OPEN + ACKNOWLEDGED) pour la corrélation
- [x] **Corrélation d'incidents** — `IncidentService` : fingerprint = type + source
  - nouvel incident → création + notification
  - récurrence → `occurrenceCount++`, `lastSeen`, escalade de sévérité (pas de spam)
- [x] Abstraction **`Notifier`** + **`NotificationHub`** (injection de liste, isolation des pannes)
- [x] **Hub d'alerte adaptatif** — routage par sévérité (`supports()`) + par événement (`IncidentEvent`)
- [x] `TeamsNotifier` = plug-in `Notifier` — n'alerte QUE sur `CREATED` (anti-spam)
- [x] **Intégration Jira** (`JiraNotifier`) — cycle de vie complet :
  - `CREATED` → crée un ticket (HIGH/CRITICAL only) + mémorise `jiraTicketKey`
  - `RECURRED` → commente le ticket existant, ou le crée si la sévérité a escaladé à HIGH/CRITICAL
  - `RESOLVED` → commente le ticket existant
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

### Tests unitaires — 60 tests (fait)
- [x] `JsonLogParserTest` (6 tests) — parsing JSON, cas limites
- [x] `ApacheLogParserTest` (6 tests) — parsing Apache, niveaux HTTP
- [x] `SyslogParserTest` (7 tests) — parsing Syslog, sévérités 0-7
- [x] `StackTraceAnomalyDetectorTest` (8 tests) — NPE, CausedBy, *Error, casse libre, sévérités
- [x] `KeywordAnomalyDetectorTest` (7 tests) — OOM, deadlock, timeout, troncature
- [x] `ErrorRateAnomalyDetectorTest` (5 tests) — seuil min, taux 30%/50%+, isolation par source
- [x] `SeverityTest` (4 tests) — enum max(), ordinal
- [x] `IncidentServiceTest` (9 tests) — création, récurrence OPEN/ACK, ACK, resolve, gardes d'état (Mockito)
- [x] `PlatformApplicationTests` — skip si pas d'ES (`@EnabledIfEnvironmentVariable`)
- [x] `AuditLogIntegrationTest`, `NotificationRecordIntegrationTest`, `SampleDataParsingTest`

### Docker + CI/CD (fait)
- [x] `backend/core/Dockerfile` — multi-stage (JDK 21 build → JRE 21 runtime)
- [x] `frontend/dashboard/Dockerfile` — multi-stage (Node 20 build → nginx runtime)
- [x] `frontend/dashboard/nginx.conf` — SPA fallback + proxy API → backend
- [x] `infra/docker-compose.yml` — 5 services (Elasticsearch + PostgreSQL + backend + frontend + Prometheus) avec healthcheck
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
| GET | `/api/audit?action=` | Journal d'audit (filtre par action) |
| GET | `/api/audit/incident/{id}` | Historique d'audit d'un incident |
| GET | `/api/notifications?channel=` | Historique des notifications |
| GET | `/api/notifications/failed` | Notifications en échec |
| GET | `/actuator/prometheus` | Métriques Prometheus |
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
│   ├── AuditController.java              → audit + notifications (ex-MetadataController fusionné)
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
│   ├── Incidents.jsx                     → cartes + actions
│   ├── ChatOps.jsx                       → terminal interactif
│   └── Audit.jsx                         → journal d'audit + notifications

infra/docker-compose.yml                  → ES + PostgreSQL + backend + frontend + Prometheus
.github/workflows/ci.yml                  → CI pipeline
```

### Notes techniques
- Variables d'env : `SPRING_ELASTICSEARCH_URIS`, `SPRING_ELASTICSEARCH_USERNAME`, `SPRING_ELASTICSEARCH_PASSWORD`, `SERVER_PORT`, `TEAMS_WEBHOOK_URL`, `JIRA_*`
- Port backend : `8082` (configurable via `SERVER_PORT`)
- Déploiement Docker : `docker compose -f infra/docker-compose.yml up --build`
- Tests : `./mvnw test` (60 tests, pas besoin d'ES)
- Swagger UI : `http://localhost:8082/swagger-ui.html`

### Sprint 4 — Évaluation (en cours)
- [x] `POST /api/logs/raw/bulk` — ingestion en lot (text/plain, 1 ligne = 1 log, ignore lignes vides/`#`, `?source=` force la source, retourne `BulkIngestResult`)
- [x] Fix `SyslogParser` — préfixe `<PRI>` optionnel (RFC 3164 sans priorité) + inférence du niveau depuis le message
- [x] `@JsonAlias("service")` sur `LogEntry.source` — les logs JSON avec champ `service` remplissent `source`
- [x] `scripts/run_evaluation.ps1` / `.sh` — injection des samples, collecte incidents + métriques Prometheus, comparaison `ground_truth.csv` → précision/rappel/F1 dans `results/eval-<ts>/` ; `-Reset` purge les index ES, `-Runs N` répétitions avec moyenne ± écart-type
- [ ] Exécuter la campagne d'évaluation complète (3 répétitions, moyenne ± écart-type)

### Session 23 sept. 2026 — Revue de code + correctifs (fait)
- [x] **P0** — `NotificationHub` persiste les `NotificationRecord` (succès + échec) et incrémente les compteurs `PlatformMetrics` ; `IncidentService` incrémente `incidentCreated`/`incidentResolved` ; niveau `WARN` normalisé (parsers ↔ filtre frontend) ; fix binding `datetime-local` → ISO dans `searchLogs`
- [x] **P1** — corrélation des récurrences sur incidents OPEN **et** ACKNOWLEDGED ; ticket Jira créé sur `RECURRED` si escalade HIGH/CRITICAL ; `StackTraceAnomalyDetector` insensible à la casse + types `*Error` ; `ErrorRateAnomalyDetector` fenêtre glissante **par source** ; gardes d'état sur `acknowledge()`/`resolve()` (400 via `GlobalExceptionHandler`, message propre en ChatOps)
- [x] **P2** — `run_evaluation.ps1` : `-Reset` (purge ES) + `-Runs N` (résultats par run + agrégat moyenne/écart-type) ; `detection.csv` enrichi des types d'incidents ; doc `plan-experiences.md` corrigée (endpoint `/api/logs/raw/bulk`)
- [x] **P3** — `MetadataController` fusionné dans `AuditController` puis supprimé ; `@Order` sur les parsers (JSON → Apache → Syslog) ; timeouts `RestClient` (5s/10s) + payloads JSON construits via Jackson dans les notifiers ; README/progress.md resynchronisés

### Prochaines étapes
- [ ] Tester le frontend + backend end-to-end
- [x] Spring Boot Actuator + métriques Prometheus (meta-observabilité)
- [ ] README.md complet avec diagrammes d'architecture
- [ ] Merger sprint-3-interfaces → main
