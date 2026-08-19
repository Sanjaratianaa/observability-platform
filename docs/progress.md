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
- [x] `IncidentDeduplicator` — cooldown 15 min par empreinte d'incident
- [x] Modèle `Anomaly` (type, description, sévérité, source)

### Notifications (fait)
- [x] `TeamsNotifier` — carte MessageCard via webhook (self-guard si non configuré)
- [x] **Branché dans `LogIngestionController#ingestRaw`** — chaque nouvel incident dédupliqué déclenche `teamsNotifier.send(...)`

### Fichiers clés
```
backend/core/src/main/java/valueit/observability/platform/
├── PlatformApplication.java              → point d'entrée
├── ElasticSearchPingRunner.java          → test connexion + insert au démarrage
├── ElasticsearchSslConfig.java           → config SSL trust-all (dev)
├── model/LogEntry.java                   → entité log
├── repository/LogEntryRepository.java    → CRUD ES auto
├── controller/LogIngestionController.java → API REST ingestion + notif
├── parser/                               → LogParser, LogParsingService, Json/Syslog/Apache
├── anomaly/                              → AnomalyDetector, détecteurs, SlidingWindowCounter, IncidentDeduplicator
├── service/                             → LogParsingService, AnomalyDetectionService
└── notification/TeamsNotifier.java       → webhook Teams
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
1. **Tests unitaires** — Syslog/Apache parsers + détecteurs d'anomalies (à faire à la maison, build requis)
2. **Vérifier la notif Teams end-to-end** — `POST /api/logs/raw` avec stack trace / burst d'erreurs
3. **Intégration Jira** — créer un ticket sur nouvel incident (TODO restant dans le controller)
4. **Nettoyer les secrets** — sortir le mot de passe ES de `application.yaml` (var d'env)

### Notes techniques
- Elasticsearch en local : `https://localhost:9200` (user: elastic)
- App sur le port `8081` (`application.yaml`)
- Teams webhook via env `TEAMS_WEBHOOK_URL` (placeholder "not-configured" par défaut → notif ignorée)
- JPA/PostgreSQL commentés dans pom.xml (prévu Sprint 2)
- Build/test nécessitent le réseau (parent POM Spring Boot 4.1.0) → à faire à la maison
- Pour relancer proprement : `taskkill /F /IM java.exe` avant de Run

### Ce qui peut être codé SANS Elasticsearch / SANS réseau
- Logique de parsing (Java pur)
- Logique de détection d'anomalies (algorithmes purs)
- Classes de service / DTO
- (Écriture des tests possible ; l'exécution nécessite le build)
