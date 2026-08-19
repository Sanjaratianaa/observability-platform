# Observability Platform — Progress

## Dernière session : 19 août 2026

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

### Endpoints API (port 8081)
| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/logs/raw` | Ingérer un log brut (auto-parse + détection + notif) |
| POST | `/api/logs` | Ingérer un log (JSON structuré) |
| POST | `/api/logs/bulk` | Ingérer plusieurs logs |
| GET | `/api/logs` | Lister tous les logs |
| GET | `/api/logs/level/{level}` | Filtrer par niveau |

### Prochaines étapes
1. **Tester le pipeline end-to-end** (à la maison) — `POST /api/logs/raw` avec stack trace / burst d'erreurs ; vérifier création `Incident`, logs `[Teams]`/`[Jira]`, montée de `occurrenceCount` sans re-spam.
2. **Tests unitaires** — `IncidentService` (nouveau vs récurrence, escalade sévérité), `NotificationHub` (routage + isolation), parsers Syslog/Apache, détecteurs.
3. **Ré-notification sur escalade** — re-notifier Teams si la sévérité monte (nouvel `IncidentEvent.ESCALATED`).
4. **Résolution d'incidents** — passer un incident en `RESOLVED` (auto quand le taux redescend, ou manuel → prépare le ChatOps Sprint 3).
5. **Sprint 3** — interface ChatOps, dashboard React, (mobile Kotlin optionnel).

### Notes techniques
- Variables d'env : `ELASTIC_PASSWORD` (requis), `TEAMS_WEBHOOK_URL`, `JIRA_BASE_URL`/`JIRA_EMAIL`/`JIRA_API_TOKEN`/`JIRA_PROJECT_KEY` (canaux auto-désactivés si "not-configured").
- App sur le port `8081` (`application.yaml`).
- Jira Cloud : Basic Auth `email:api-token` (Base64), endpoints `/rest/api/2/issue` (création) et `/rest/api/2/issue/{key}/comment` (MAJ).
- Build/test nécessitent le réseau (parent POM Spring Boot 4.1.0) → à faire à la maison.
- Pour relancer proprement : `taskkill /F /IM java.exe` avant de Run.

### Ce qui peut être codé SANS Elasticsearch / SANS réseau
- Logique de parsing (Java pur)
- Logique de détection d'anomalies (algorithmes purs)
- Logique de corrélation / orchestration (`IncidentService`, `NotificationHub`)
- (Écriture des tests possible ; l'exécution nécessite le build)
