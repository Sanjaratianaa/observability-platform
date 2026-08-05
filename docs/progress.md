# Observability Platform — Progress

## Dernière session : 5 août 2026

### Ce qui est fait (Sprint 1 — Connecteur ES)
- [x] Projet Spring Boot 4.1.0 initialisé (Java 21, Maven)
- [x] Connexion Elasticsearch 9.5.0 validée (HTTPS + auth)
- [x] Config SSL dev (`ElasticsearchSslConfig`) — trust-all pour cert auto-signé
- [x] Modèle `LogEntry` (@Document, index "logs")
- [x] `LogEntryRepository` (CRUD auto via Spring Data ES)
- [x] Insert/read d'un document validé au démarrage
- [x] API REST créée (`LogIngestionController`) — pas encore testée (port occupé)

### Fichiers clés
```
backend/core/src/main/java/valueit/observability/platform/
├── PlatformApplication.java          → point d'entrée
├── ElasticSearchPingRunner.java      → test connexion + insert au démarrage
├── ElasticsearchSslConfig.java       → config SSL trust-all (dev)
├── model/LogEntry.java               → entité log (id, timestamp, level, source, message)
├── repository/LogEntryRepository.java → CRUD ES auto
└── controller/LogIngestionController.java → API REST ingestion
```

### Endpoints API (port 8081)
| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/logs` | Ingérer un log (JSON) |
| POST | `/api/logs/bulk` | Ingérer plusieurs logs |
| GET | `/api/logs` | Lister tous les logs |
| GET | `/api/logs/level/{level}` | Filtrer par niveau |

### Prochaines étapes
1. **Tester l'API REST** — tuer les processus Java, relancer, tester avec curl/Postman
2. **Moteur de parsing** (~2 soirs) — parser des logs hétérogènes (JSON, syslog, Apache) → normaliser en `LogEntry`
3. **Détection d'anomalies** (~2-3 soirs) — pattern matching (taux erreurs, mots-clés critiques, seuils)

### Notes techniques
- Elasticsearch tourne en local : `https://localhost:9200` (user: elastic)
- L'app écoute sur le port `8081` (configuré dans application.yaml)
- JPA/PostgreSQL commentés dans pom.xml (prévu Sprint 2)
- Pour relancer proprement : `taskkill /F /IM java.exe` avant de Run

### Ce qui peut être codé SANS Elasticsearch (au travail)
- Le moteur de parsing (logique pure Java, pas besoin de connexion ES)
- Les classes de service/DTO
- Les tests unitaires
- La logique de détection d'anomalies (algorithmes purs)
