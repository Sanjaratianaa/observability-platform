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

### Fichiers clés (obsolète — voir section Refonte microservices)
```
backend/core/  → supprimé, remplacé par backend/{common,monitoring-service,incident-service,chatops-service}
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

### Endpoints API (par service)
| Service | Port | Endpoints |
|---------|------|-----------|
| monitoring-service | 8081 | `POST /api/logs/raw[/bulk]`, `POST /api/logs[/bulk]`, `GET /api/logs[?page&size]`, `GET /api/logs/level/{level}`, `GET /api/logs/search` |
| incident-service | 8082 | `GET /api/incidents[?status&severity]`, `GET /api/incidents/{id}`, `PUT .../ack`, `PUT .../resolve`, `GET /api/incidents/stats`, `GET /api/audit*`, `GET /api/notifications*`, `POST /internal/anomalies` (interne) |
| chatops-service | 8083 | `POST /api/chatops` (list, stats, ack, resolve) |
| tous | — | `/actuator/health`, `/actuator/prometheus`, `/swagger-ui.html` |

### Fichiers clés (microservices)
```
backend/                                  → parent pom.xml + mvnw (multi-module)
├── common/                               → LogEntry, Incident, enums, AnomalyReport, ElasticsearchSslConfig
├── monitoring-service/ (8081)            → ingestion, parsers, détecteurs, IncidentClient (REST)
├── incident-service/  (8082)             → IncidentService, notifiers, audit JPA, /internal/anomalies
└── chatops-service/   (8083)             → ChatOpsService, commandes, IncidentApiClient (REST)

frontend/dashboard/src/ (inchangé)
infra/docker-compose.yml                  → ES + PostgreSQL + 3 services + frontend + Prometheus
.github/workflows/ci.yml                  → CI : mvnw verify depuis backend/ (multi-module)
```

### Notes techniques
- Variables d'env : `SPRING_ELASTICSEARCH_URIS`, `INCIDENT_SERVICE_URL`, `SPRING_DATASOURCE_URL`, `SERVER_PORT`, `TEAMS_WEBHOOK_URL`, `JIRA_*`
- Ports : monitoring `8081`, incident `8082`, chatops `8083`
- Déploiement Docker : `docker compose -f infra/docker-compose.yml up --build`
- Tests : `cd backend && ./mvnw test` (répartis par module)
- Swagger UI : `http://localhost:808{1,2,3}/swagger-ui.html` (par service)

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

### Session 23 sept. 2026 (suite) — Refonte microservices (fait)
- [x] **Maven multi-module** — `backend/pom.xml` parent (Spring Boot 4.1.0, Java 21) + `mvnw` déplacé à la racine de `backend/` ; modules : `common`, `monitoring-service`, `incident-service`, `chatops-service`
- [x] **`common`** — `LogEntry`, `Incident`, `IncidentStatus`, `IncidentEvent`, `Severity`, `AnomalyReport` (record DTO inter-services), `ElasticsearchSslConfig`
- [x] **`monitoring-service` (8081)** — ingestion (`/api/logs*`), parsers, détecteurs, `IncidentClient` → `POST /internal/anomalies` (isolation de panne : log l'erreur sans bloquer l'ingestion)
- [x] **`incident-service` (8082)** — `IncidentService.handle(AnomalyReport)`, `InternalAnomalyController` (`@Hidden`), notifiers, audit JPA, métriques incidents/notifications
- [x] **`chatops-service` (8083)** — commandes réécrites sur `IncidentApiClient` (RestClient → `/api/incidents`), plus d'accès direct aux repositories
- [x] **Infra** — `docker-compose.yml` : 3 services backend (build context `../backend`, Dockerfile par module) ; `prometheus.yml` : 3 targets ; `nginx.conf` + `vite.config.js` : routage `/api/logs`→8081, `/api/chatops`→8083, reste→8082 ; `ci.yml` : build depuis `backend/` ; `run_evaluation.ps1` : `-MonitoringUrl`/`-IncidentUrl` ; `api-tests.http` : ports corrigés
- [x] **Tests** — répartis par module (parsers/détecteurs → monitoring, IncidentService/Severity/JPA → incident, ChatOpsService → chatops) ; `IncidentServiceTest` adapté à `AnomalyReport`
- [x] **Nettoyage** — `backend/core` et `backend/orchestrator` supprimés ; README/progress resynchronisés
- [ ] Vérifier `mvnw verify` sur une machine avec accès Maven Central (bloqué ici par le miroir HTTP forge.workit.fr)

### Prochaines étapes
- [ ] Tester le frontend + backend end-to-end
- [x] Spring Boot Actuator + métriques Prometheus (meta-observabilité)
- [x] README.md complet avec diagrammes d'architecture
- [ ] Merger sprint-3-interfaces → main
