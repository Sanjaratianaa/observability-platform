# Informations manquantes / à corriger dans le mémoire

Analyse comparative entre `MémoireITU-v3r3` et l'application réellement implémentée
(`observability-platform`). Les sections ci-dessous donnent le contenu prêt à intégrer.

---

## 📌 ÉTAT D'AVANCEMENT (25 sept. 2026 — docx vérifié après mise à jour)

### ✅ FAIT — vérifié dans le .docx

- **Légendes des 10 figures** placées
- **§5.1.1 corrigé** — déduplication par fingerprint ES
- **§1.2 reformulé** — détection reproduite par le module monitoring (outils internes en prod)
- **§7.2.3 réécrit** — ES + PostgreSQL + dédup
- **Glossaire complet** — fingerprint, Grafana, Prometheus, ChatOps, CI/CD, webhook, timeout…
- **§5.3** — mentionne les 5 vues (Dashboard, Logs, Incidents, ChatOps, Audit)
- **Figures dessinées et insérées** : Fig 3 (§6.1), Fig 4 (§6.2, validée), Fig 5 (§7.2.3),
  Fig 6 (§7.2.4, validée), Fig 7 (§7.2.4, états), Fig 8 (§7.2.4, pivotée 90°)

### 🔴 INCOHÉRENCES à corriger (le texte contredit le mécanisme réel)

1. **§7.2.4 « Détection et création automatique d'un ticket »** — dit encore
   « vérifie si un ticket correspondant existe déjà **dans Jira** » → reprendre le texte
   fingerprint ES de §5.1.1.
2. **§5.3.2** — « Jira : utilisé pour **rechercher les tickets existants** » → Jira ne sert
   qu'à créer/commenter ; la recherche de doublons est dans Elasticsearch.
3. **§2.3** — « L'API REST de Jira est utilisée pour… **vérifier si un incident similaire
   a déjà été signalé** » → même correction (dédup dans ES, Jira = création/commentaire).

### 🐛 Bugs de texte

4. **Résumé FR tronqué** — « …notifications instantanées sur Microsoft sans ouvrir les
   outils de monitoring existants » : il manque « **Teams**, et une interface
   conversationnelle… » (la version EN est complète, pas la FR).
5. **§5.1.1** — typo : « monitoring-service **ka** transmet » → « la ».

### ⬜ RESTE À FAIRE

**Nécessite l'app lancée** (`docker compose up` dans `infra/`) :

6. **Fig 1 + Fig 2** (§5.3.1) — captures Dashboard + Incidents (`http://localhost:8090`).
7. **Fig 10** (§9) — `scripts/run_evaluation.ps1` → P/R/F1 par détecteur. **Priorité max.**
8. **Fig 9** (§8.4) — script de charge (N × `POST /api/logs/bulk`) → débit + temps de réponse.

**Texte à compléter dans le .docx** :

9. **§5.3.1** — le texte dit encore « l'interface principale est le ChatOps » alors que
   §5.3 cite les 5 vues → décrire le dashboard React (React 19 + nginx proxy).
10. **§5.3.2** — ajouter le tableau des endpoints REST (contenu dans « Prompt 9 »).
11. **§7.1.2** — stack réelle : Spring Boot 4.1.0, Java 21, ES 9.0.0, PostgreSQL 16,
    React 19, Prometheus, Grafana, Maven multi-module.
12. **§6.2** — le texte ne cite que « quatre éléments » : ajouter frontend/nginx,
    PostgreSQL, Prometheus, Grafana (cohérent avec la Fig 4 à 8 conteneurs).
13. **§7.2.x** — méta-observabilité (`/actuator/prometheus` → Grafana), audit JPA,
    Swagger par service, `GlobalExceptionHandler`, CORS.
14. **§8** — tableau des 65 tests par module + CI GitHub Actions ; §8.4 est encore
    au futur (« sont prévus ») → passer au résultat quand la charge est faite.
15. **§9.3** — ajouter : auth sur les API, file d'attente/rate limiting, retry/circuit
    breaker, Kubernetes, Kibana optionnel.
16. **Résumé/Abstract** — après fix n°4 : mentionner dashboard + fingerprint + chiffres
    d'évaluation (quand dispo).
17. **Annexe** — extrait `docker-compose.yml`, payload `AnomalyReport`, exemples de logs.

> **Note** : les sections « Prompt 1-9 » plus bas servent de **référence** (contenu
> exact des figures + textes). Ne pas supprimer — nécessaires pour rédiger les
> paragraphes autour des figures déjà insérées.

---

## ⚠️ Corrections importantes (risque en soutenance)

### 1. « Architecture microservices » → ✅ **résolu par la refonte (23 sept. 2026)**

~~Le mémoire affirmait trois microservices alors que le backend était un monolithe
modulaire.~~ **L'implémentation a été refactorée en microservices** — le mémoire est
maintenant conforme à la réalité :

- **`monitoring-service`** (port 8081) — ingestion de logs (`/api/logs*`), parsing
  multi-format, détection d'anomalies, index `logs` dans Elasticsearch. Transmet
  chaque anomalie via REST (`POST /internal/anomalies`, DTO `AnomalyReport`).
- **`incident-service`** (port 8082) — corrélation/déduplication par fingerprint,
  cycle de vie des incidents, notifications Jira/Teams, audit JPA
  (`audit_logs`, `notification_records` en H2/PostgreSQL),
  index `incidents` dans Elasticsearch.
- **`chatops-service`** (port 8083) — interface conversationnelle (`/api/chatops`),
  délègue à incident-service via `IncidentApiClient` (REST).
- **`common`** — module Maven partagé : `LogEntry`, `Incident`, enums, `AnomalyReport`,
  `ElasticsearchSslConfig`.
- Le `docker-compose.yml` déploie désormais **8 conteneurs** : Elasticsearch,
  PostgreSQL, les 3 microservices, frontend, Prometheus, Grafana.
- Communication inter-services : REST synchrone (RestClient), avec isolation de panne
  (l'ingestion continue si incident-service est indisponible).

**Reste à faire dans le mémoire** : vérifier que les descriptions (ports, endpoint
interne `/internal/anomalies`, module `common`, découpage des responsabilités)
correspondent à cette implémentation.

### 2. Déduplication : ce n'est pas une recherche dans Jira → ✅ **corrigé dans le mémoire**

Le mémoire dit (§5.1.1, §7.2.4) que la plateforme « vérifie si un ticket correspondant
existe déjà dans Jira ». En réalité :

- La déduplication se fait dans **Elasticsearch** via un **fingerprint** =
  `type d'anomalie + "::" + source`. À chaque anomalie, on cherche un incident
  **OPEN ou ACKNOWLEDGED** avec le même fingerprint.
- Si trouvé → `occurrenceCount++`, `lastSeen` mis à jour, escalade de sévérité
  (`Severity.max()`), et un **commentaire** est ajouté au ticket Jira existant
  (la clé `jiraTicketKey` est stockée sur l'incident).
- Si absent → création de l'incident + ticket Jira (si HIGH/CRITICAL).

Texte de remplacement suggéré :

> Avant de créer un ticket, la plateforme calcule une empreinte (fingerprint) de
> l'anomalie à partir de son type et de sa source, puis recherche dans Elasticsearch
> un incident actif (ouvert ou acquitté) portant la même empreinte. Si un tel incident
> existe, il est mis à jour (compteur d'occurrences, date de dernière vue, escalade de
> sévérité) et le ticket Jira associé reçoit un commentaire de récurrence. Sinon, un
> nouvel incident est créé et un ticket Jira est ouvert lorsque la sévérité l'exige.

---

## Informations manquantes par section

### §5.3.1 IHM — le dashboard React n'est pas mentionné

Le mémoire présente ChatOps comme « l'interface utilisateur principale ». Il existe en
fait **un dashboard web complet** (React 19 + Vite + Tailwind CSS 4 + Recharts) avec
5 pages :

- **Dashboard** — KPI (total logs, incidents, ouverts, résolus) + graphiques de répartition
- **Logs** — tableau paginé, recherche temporelle (`from`/`to`), filtre par niveau
- **Incidents** — cartes avec badges sévérité/statut, actions Acquitter/Résoudre, filtres
- **ChatOps** — terminal interactif intégré
- **Audit** — journal d'audit + historique des notifications (filtres action/canal)

### §6 / §7 — le pipeline de détection interne n'est pas décrit

Le mémoire laisse entendre que la détection est entièrement assurée par les outils
existants. L'application implémente en fait **son propre pipeline**, réparti entre
monitoring-service (étapes 1-3) et incident-service (étapes 4-5) :

1. **Ingestion** *(monitoring-service)* — `POST /api/logs/raw` (1 log) et `POST /api/logs/raw/bulk?source=`
   (lot, 1 ligne = 1 log, retourne `BulkIngestResult` : reçus/ingérés/erreurs/anomalies)
2. **Parsing** *(monitoring-service)* — 3 parsers avec `@Order` déterministe :
   `JsonLogParser` (JSON natif, alias `service`→`source`), `ApacheLogParser`
   (access log, statut HTTP → niveau), `SyslogParser` (RFC 3164, PRI optionnel,
   inférence du niveau depuis le message)
3. **Détection** *(monitoring-service)* — 3 détecteurs :
   - `ErrorRateAnomalyDetector` — fenêtre glissante 5 min **par source**, seuil 30 %
     (HIGH) / 50 % (CRITICAL), minimum 10 logs
   - `KeywordAnomalyDetector` — mots-clés critiques (OOM, deadlock, timeout…)
   - `StackTraceAnomalyDetector` — extraction du type d'exception (insensible à la
     casse, types `*Exception` et `*Error`), root cause via `Caused by:`, localisation
     de la frame
   - Chaque anomalie est envoyée à incident-service via `POST /internal/anomalies`
     (DTO `AnomalyReport` : type, description, severity, source, logId)
4. **Corrélation** *(incident-service)* — fingerprint + statuts actifs (voir correction n°2)
5. **Notification** *(incident-service)* — `NotificationHub` dispatch asynchrone vers les `Notifier` :
   - `TeamsNotifier` — webhook entrant, MessageCard, uniquement sur `CREATED` (anti-spam)
   - `JiraNotifier` — API REST v2, ticket si HIGH/CRITICAL (y compris sur `RECURRED`
     après escalade), commentaires sur récurrence/résolution
   - Isolation des pannes : l'échec d'un notifier n'affecte pas les autres ;
     chaque envoi est persisté dans `notification_record` (succès/échec + erreur)

### §7.2.3 Modélisation des données — à préciser

**Elasticsearch** (index `logs` et `incidents`) :

- `LogEntry` : timestamp, level (INFO/WARN/ERROR/DEBUG), source, message
- `Incident` : fingerprint, type, description, severity (LOW→CRITICAL), status
  (OPEN→ACKNOWLEDGED→RESOLVED), source, occurrenceCount, firstSeen/lastSeen,
  relatedLogIds, jiraTicketKey

**PostgreSQL/H2 via JPA** (métadonnées — absent du mémoire) :

- `audit_logs` : action, entityType, entityId, details, performedBy, createdAt
- `notification_records` : channel, incidentId, eventType, externalRef, success,
  errorMessage, sentAt

### §7 — méta-observabilité (absente)

La plateforme s'auto-supervise via **Spring Boot Actuator + Micrometer → Prometheus** —
chaque microservice expose `/actuator/prometheus`, scrapé par Prometheus (conteneur dédié) :

- *incident-service* : `obs_incidents_created_total`, `obs_incidents_resolved_total`,
  `obs_notifications_sent_total`, `obs_notifications_failed_total`
- *monitoring-service* : compteurs de logs ingérés et d'anomalies détectées
- Grafana (conteneur dédié, port 3000) pour la visualisation

### §8 Tests — chiffres réels à intégrer

**65 tests** (JUnit 5 + Mockito, sans Elasticsearch requis), répartis par microservice :

| Module | Suite | Tests | Couverture |
|--------|-------|-------|------------|
| monitoring | `JsonLogParserTest` | 6 | parsing JSON, cas limites |
| monitoring | `ApacheLogParserTest` | 6 | parsing Apache, niveaux HTTP |
| monitoring | `SyslogParserTest` | 7 | PRI, inférence de niveau |
| monitoring | `StackTraceAnomalyDetectorTest` | 8 | NPE, CausedBy, *Error, casse |
| monitoring | `KeywordAnomalyDetectorTest` | 7 | OOM, deadlock, timeout |
| monitoring | `ErrorRateAnomalyDetectorTest` | 5 | seuils, isolation par source |
| monitoring | `SampleDataParsingTest`, `MonitoringApplicationTests` | 2 | parsing samples, contexte |
| incident | `IncidentServiceTest` | 9 | création, récurrence OPEN/ACK, gardes d'état |
| incident | `SeverityTest` | 4 | enum, escalade |
| incident | `AuditLogIntegrationTest`, `NotificationRecordIntegrationTest`, `IncidentApplicationTests` | 7 | audit, notifications, contexte |
| chatops | `ChatOpsServiceTest` | 4 | dispatch, help, commandes inconnues |

**Évaluation** (à citer en §8.4 ou §9) : `scripts/run_evaluation.ps1` injecte les
fichiers de `data/sample/`, compare les incidents créés à `data/annotations/ground_truth.csv`
et calcule **précision / rappel / F1** (options `-Reset` purge ES, `-Runs N` avec
moyenne ± écart-type). ⚠️ Les résultats chiffrés doivent être obtenus en exécutant
la campagne — la section « Tests de charge » est encore un placeholder.

### §2.6 / §4 — CI/CD : c'est implémenté

Le mémoire dit « le choix de l'outil CI/CD dépend de l'environnement ». En réalité :
**GitHub Actions** (`.github/workflows/ci.yml`) — 3 jobs : backend (`mvnw verify`
multi-module depuis `backend/` : compile + teste les 4 modules), frontend
(`npm ci && npm run build`), validation `docker compose config`.

### Autres éléments implémentés à mentionner

- **Swagger UI** — `springdoc-openapi` sur chaque service (`/swagger-ui.html`)
- **Gestion d'erreurs** — `GlobalExceptionHandler` dans chaque service (400/404/500 JSON)
- **Gardes d'état** — `acknowledge()` refuse les non-OPEN, `resolve()` refuse les
  déjà résolus (cohérence du cycle de vie)
- **Timeouts HTTP** — 5 s connexion / 10 s lecture sur les appels Jira/Teams et
  inter-services (`IncidentClient`, `IncidentApiClient`)
- **CORS** — `WebConfig` dans chaque service pour le frontend
- **Sécurité** — tous les secrets en variables d'env (`TEAMS_WEBHOOK_URL`, `JIRA_*`,
  `SPRING_ELASTICSEARCH_*`) ; auth Basic Jira ; SSL trust-all en dev pour ES

### Stack technique réelle (versions)

| Composant | Version |
|-----------|---------|
| Java / Spring Boot | 21 / 4.1.0 |
| Elasticsearch | 9.0.0 |
| PostgreSQL (prod) / H2 (dev) | 16 / embarqué |
| React / Vite / Tailwind | 19 / 8 / 4 |
| Node (build frontend) | 20 |
| Ports | monitoring 8081, incident 8082, chatops 8083, frontend 8090, ES 9200, Prometheus 9091, Grafana 3000 |

### API REST complète (pour l'annexe)

`/api/logs*` → monitoring-service (8081) · `/api/incidents*`, `/api/audit*`, `/api/notifications*` → incident-service (8082) · `/api/chatops` → chatops-service (8083) · `POST /internal/anomalies` → endpoint interne incident-service.

| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/logs/raw` | Ingérer un log brut |
| POST | `/api/logs/raw/bulk?source=` | Ingestion en lot |
| POST | `/api/logs` | Log structuré |
| POST | `/api/logs/bulk` | Logs structurés en lot |
| GET | `/api/logs?page=&size=` | Liste paginée |
| GET | `/api/logs/level/{level}` | Filtre par niveau |
| GET | `/api/logs/search?from=&to=&level=` | Recherche temporelle |
| GET | `/api/incidents?status=&severity=` | Liste + filtres |
| GET | `/api/incidents/{id}` | Détail |
| PUT | `/api/incidents/{id}/ack` | Acquitter |
| PUT | `/api/incidents/{id}/resolve` | Résoudre |
| GET | `/api/incidents/stats` | Comptage par statut |
| POST | `/api/chatops` | Commandes : `list`, `stats`, `ack <id>`, `resolve <id>`, `help` |
| GET | `/api/audit?action=` | Journal d'audit |
| GET | `/api/audit/incident/{id}` | Historique d'un incident |
| GET | `/api/notifications?channel=` | Historique notifications |
| GET | `/api/notifications/failed` | Notifications en échec |
| GET | `/actuator/health`, `/actuator/prometheus` | Santé + métriques |

### Écarts entre le mémoire et l'implémentation (à assumer ou corriger)

- **File d'attente / rate limiting** (§2.7) : non implémenté — remplacé par dispatch
  `@Async` + déduplication. À reformuler ou à lister en évolution future.
- **Sauvegarde/restauration** (§2.6) : non implémenté — à garder comme perspective.
- **« Générer des rapports »** (objectif §1.2) : couvert partiellement par la page
  Audit + métriques Prometheus, pas de génération de rapports à proprement parler.
- **Bibliographie** : ajouter éventuellement Spring Boot docs, Docker docs,
  springdoc-openapi.

---

## Figures pour le mémoire (contenu vérifié dans le code)

### Prompt 1 — Figure d'architecture globale

**Composants** (8 conteneurs, `infra/docker-compose.yml`) :

| Composant | Conteneur | Image / Build | Port hôte → conteneur | Rôle |
|---|---|---|---|---|
| Frontend | `obs-frontend` | build `frontend/dashboard` (React 19 + nginx) | 8090 → 80 | Dashboard web + reverse proxy nginx vers les API |
| monitoring-service | `obs-monitoring` | build `backend/monitoring-service` | 8081 → 8081 | Ingestion (`/api/logs*`), parsing multi-format, détection d'anomalies, index `logs` ES, forwarding des anomalies |
| incident-service | `obs-incident` | build `backend/incident-service` | 8082 → 8082 | Corrélation/dédup fingerprint, cycle de vie (`/api/incidents*`), notifications Jira/Teams, audit (`/api/audit*`, `/api/notifications*`), index `incidents` ES, JPA PostgreSQL (`audit_logs`, `notification_records`) |
| chatops-service | `obs-chatops` | build `backend/chatops-service` | 8083 → 8083 | `POST /api/chatops`, délègue à incident-service via `IncidentApiClient` |
| Elasticsearch | `obs-elasticsearch` | `elasticsearch:9.0.0` | 9200 → 9200 | Index `logs` + `incidents` |
| PostgreSQL | `obs-postgres` | `postgres:16-alpine` | 5432 → 5432 | Métadonnées JPA : `audit_logs`, `notification_records` |
| Prometheus | `obs-prometheus` | `prom/prometheus` | 9091 → 9090 | Scrape `/actuator/prometheus` des 3 services (15 s) |
| Grafana | `obs-grafana` | `grafana/grafana` | 3000 → 3000 | Dashboards métriques (datasource Prometheus provisionnée) |

Acteurs externes (hors compose) : navigateur utilisateur, producteurs de logs,
Jira (API REST v2), Teams (webhook entrant).

**Relations** — format `A → protocole/endpoint → B` :

Entrées utilisateur / logs :

- Navigateur → `HTTP :8090` → Frontend (nginx)
- Producteurs de logs → `HTTP POST /api/logs/raw`, `/api/logs/raw/bulk`, `/api/logs`, `/api/logs/bulk` → monitoring-service:8081 (direct ou via nginx)

Frontend → backend (proxy nginx, `frontend/dashboard/nginx.conf`) :

- Frontend nginx → `HTTP proxy /api/logs*` → monitoring-service:8081
- Frontend nginx → `HTTP proxy /api/chatops` → chatops-service:8083
- Frontend nginx → `HTTP proxy /api/*` (incidents, audit, notifications) → incident-service:8082

Inter-services :

- monitoring-service → `HTTP REST POST /internal/anomalies` (JSON `AnomalyReport`, RestClient, timeouts 5 s/10 s) → incident-service:8082
- chatops-service → `HTTP REST GET /api/incidents?status=OPEN`, `GET /api/incidents/stats`, `PUT /api/incidents/{id}/ack|resolve` → incident-service:8082

Persistance :

- monitoring-service → `HTTP REST (client ES)` → elasticsearch:9200 — index `logs`
- incident-service → `HTTP REST (client ES)` → elasticsearch:9200 — index `incidents`
- incident-service → `JDBC/PostgreSQL` → postgres:5432 — tables `audit_logs`, `notification_records`

Notifications externes :

- incident-service → `HTTPS REST API v2 (Basic auth)` → Jira — tickets + commentaires
- incident-service → `HTTPS POST webhook (MessageCard JSON)` → Teams

Méta-observabilité :

- Prometheus → `HTTP GET /actuator/prometheus` (scrape 15 s) → monitoring-service:8081, incident-service:8082, chatops-service:8083
- Grafana → `HTTP/PromQL` → prometheus:9090 (datasource `http://prometheus:9090`, access proxy)
- Navigateur → `HTTP :3000` → Grafana · `HTTP :9091` → Prometheus UI

Protocoles : HTTP/REST JSON partout en interne, JDBC vers PostgreSQL, HTTPS vers
Jira/Teams. Timeouts 5 s connexion / 10 s lecture sur les clients REST.

### Prompt 2 — Pipeline de traitement d'un log

Fonctionnement réellement implémenté, dans l'ordre :

1. **Réception** *(monitoring-service)* — `LogIngestionController` :
   `POST /api/logs/raw` (1 log) et `POST /api/logs/raw/bulk?source=` (lot
   `text/plain`, 1 ligne = 1 log, lignes vides/`#` ignorées, retourne
   `BulkIngestResult{received, ingested, parseErrors, anomaliesDetected}`).
   Note : `POST /api/logs` et `/api/logs/bulk` (structurés) sauvegardent
   directement sans parsing ni détection.
2. **Parsing** *(monitoring-service)* — `LogParsingService.parse()` itère les
   `LogParser` par `@Order`, premier `canParse()` gagne : `JsonLogParser` (1),
   `ApacheLogParser` (2), `SyslogParser` (3). Produit `LogEntry{timestamp, level,
   source, message}` → sauvegardé dans l'index ES `logs` (génère `logId`).
   Échec → `LogParserException`.
3. **Détection** *(monitoring-service)* — `AnomalyDetectionService.analyze()`
   exécute tous les `AnomalyDetector` : `ErrorRateAnomalyDetector` (fenêtre 5 min
   par source, ≥10 logs, ≥30 % → HIGH, ≥50 % → CRITICAL), `KeywordAnomalyDetector`,
   `StackTraceAnomalyDetector`. Produit 0..n `Anomaly{type, description, severity,
   detectedAt, sourceLogMessage}`.
4. **Transmission** *(monitoring → incident)* — `toReport()` mappe vers
   `AnomalyReport{type, description, severity, detectedAt, source, logId,
   sourceLogMessage}` (module `common`) ; `IncidentClient.report()` envoie
   `HTTP POST /internal/anomalies` (RestClient, timeouts 5 s/10 s ; échec loggé,
   jamais propagé — isolation de panne).
5. **Fingerprint** *(incident-service)* — `InternalAnomalyController`
   (`POST /internal/anomalies`, `@Hidden`) → `IncidentService.handle()` ;
   `fingerprint = type + "::" + source`.
6. **Recherche** *(incident-service)* —
   `IncidentRepository.findFirstByFingerprintAndStatusIn(fingerprint,
   [OPEN, ACKNOWLEDGED])` sur l'index ES `incidents` (RESOLVED ignoré).
7. **Création ou mise à jour** *(incident-service)* — trouvé → `updateRecurring()` :
   `lastSeen`, `occurrenceCount+1`, `severity = Severity.max()`, `description`,
   `relatedLogIds` → `dispatch(RECURRED)`. Absent → `createNew()` : incident OPEN,
   `occurrenceCount=1` → `metrics.incidentCreated()` → `dispatch(CREATED)` →
   `AuditService.record("INCIDENT_CREATED")` → table `audit_logs` (PostgreSQL).
8. **Notification** *(incident-service)* — `NotificationHub.dispatch()` (`@Async`)
   itère les `Notifier` avec filtre `supports()` : `TeamsNotifier` (tous, mais
   n'agit que sur `CREATED` → webhook HTTPS MessageCard) ; `JiraNotifier`
   (HIGH/CRITICAL seulement → ticket si `jiraTicketKey == null`, commentaires sur
   récurrence/résolution, REST v2 Basic auth). Chaque envoi → `NotificationRecord`
   en PostgreSQL + métriques `obs_notifications_sent/failed_total`.

Schéma suggéré :

```
Log brut ──HTTP──▶ [monitoring-service]
                     LogIngestionController → LogParsingService → LogEntry → ES «logs»
                          │
                     AnomalyDetectionService (3 détecteurs) → Anomaly
                          │
                     IncidentClient ──HTTP POST /internal/anomalies──▶ [incident-service]
                                                                       InternalAnomalyController
                                                                          │
                                                                     IncidentService.handle()
                                                                       fingerprint = type::source
                                                                          │
                                                              ES «incidents» : OPEN/ACK ?
                                                                   ┌──────┴──────┐
                                                                trouvé        absent
                                                                   │             │
                                                            updateRecurring  createNew
                                                            (count+1, sev.)  (OPEN, audit)
                                                                   └──────┬──────┘
                                                                NotificationHub (@Async)
                                                                   ┌──────┴──────┐
                                                             TeamsNotifier   JiraNotifier
                                                             (CREATED only)  (HIGH/CRITICAL)
                                                                   └──────┬──────┘
                                                                notification_record (PostgreSQL)
```

Points forts à mentionner : déduplication par fingerprint (pas de recherche Jira),
isolation de panne aux deux extrémités (ingestion survit à incident-service down ;
un notifier en échec ne bloque pas les autres).

### Prompt 3 — Architecture interne des microservices (structure Maven)

Parent : `backend/pom.xml` = `platform-parent` (packaging `pom`, Spring Boot 4.1.0,
Java 21) — modules : `common`, `monitoring-service`, `incident-service`,
`chatops-service`. `dependencyManagement` épingle la version de `common`.

#### `common` — bibliothèque partagée (jar, pas un service)

- **Rôle** : contrat inter-services — modèles, DTO, enums, config ES.
- **Classes** : `model.LogEntry` ; `incident.Incident`, `IncidentStatus`,
  `IncidentEvent`, `Severity` ; `dto.AnomalyReport` ; `ElasticsearchSslConfig`.
- **Endpoints** : aucun (pas d'application Spring Boot).
- **Dépendances** : aucune interne. Dépendants : monitoring-service et
  incident-service — **chatops-service n'en dépend pas** (il consomme les
  incidents en `JsonNode` générique).

#### `monitoring-service` (port 8081)

- **Rôle** : ingestion de logs, parsing multi-format, détection d'anomalies,
  index ES `logs`, forwarding des anomalies vers incident-service.
- **Classes principales** :
  - `controller` : `LogIngestionController`, `GlobalExceptionHandler`
  - `parser` : `LogParser` (interface), `JsonLogParser` @Order(1),
    `ApacheLogParser` @Order(2), `SyslogParser` @Order(3), `LogParserException`
  - `anomaly` : `AnomalyDetector` (interface), `Anomaly`,
    `ErrorRateAnomalyDetector` (+ `SlidingWindowCounter`),
    `KeywordAnomalyDetector`, `StackTraceAnomalyDetector`
  - `service` : `LogParsingService`, `AnomalyDetectionService`,
    `IncidentClient` (REST → incident-service)
  - `repository` : `LogEntryRepository` (Spring Data ES)
  - `metrics.PlatformMetrics`, `EsPingRunner`, `OpenApiConfig`, `WebConfig`
- **Endpoints** : `POST /api/logs/raw`, `POST /api/logs/raw/bulk?source=`,
  `POST /api/logs`, `POST /api/logs/bulk`, `GET /api/logs`, `GET /api/logs/level/{level}`,
  `GET /api/logs/search`, `/actuator/*`, `/swagger-ui.html`
- **Dépendances Maven** : `common`, `spring-boot-starter-web`,
  `spring-boot-starter-data-elasticsearch`, `actuator`, `micrometer-registry-prometheus`,
  `springdoc-openapi`. **Pas de JPA, pas de notification.**
- **Communications** : → Elasticsearch (HTTP REST, index `logs`) ;
  → incident-service (`POST /internal/anomalies`).

#### `incident-service` (port 8082)

- **Rôle** : cycle de vie des incidents, corrélation/déduplication par fingerprint,
  notifications Jira/Teams, audit — index ES `incidents` + JPA PostgreSQL.
- **Classes principales** :
  - `controller` : `IncidentController`, `InternalAnomalyController` (@Hidden),
    `AuditController`, `GlobalExceptionHandler`
  - `service` : `IncidentService` (fingerprint, dédup, gardes d'état)
  - `notification` : `Notifier` (interface), `NotificationHub` (@Async),
    `TeamsNotifier`, `JiraNotifier`
  - `metadata` : `AuditLog` + `AuditLogRepository` + `AuditService`,
    `NotificationRecord` + `NotificationRecordRepository` (JPA)
  - `repository` : `IncidentRepository` (Spring Data ES)
  - `metrics.PlatformMetrics`, `EsPingRunner`, `OpenApiConfig`, `WebConfig`
- **Endpoints** : `POST /internal/anomalies` (interne) ; `GET /api/incidents`,
  `GET /api/incidents/{id}`, `PUT /api/incidents/{id}/ack`,
  `PUT /api/incidents/{id}/resolve`, `GET /api/incidents/stats` ;
  `GET /api/audit`, `GET /api/audit/incident/{id}`, `GET /api/notifications`,
  `GET /api/notifications/failed` ; `/actuator/*`, `/swagger-ui.html`
- **Dépendances Maven** : `common`, `web`, `data-elasticsearch`, `data-jpa`,
  `postgresql`, `h2` (dev), `actuator`, `micrometer-registry-prometheus`,
  `springdoc-openapi`. **Pas de parsers ni de détecteurs.**
- **Communications** : → Elasticsearch (index `incidents`) ; → PostgreSQL (JDBC) ;
  → Jira (HTTPS REST v2, Basic auth) ; → Teams (HTTPS webhook).

#### `chatops-service` (port 8083)

- **Rôle** : interface conversationnelle — délègue tout à incident-service via REST.
- **Classes principales** :
  - `controller` : `ChatOpsController`, `GlobalExceptionHandler`
  - `service` : `ChatOpsService` (dispatch de commandes), `IncidentApiClient`
    (REST → incident-service)
  - `chatops` : `ChatCommand` (interface), `ListIncidentsCommand`, `StatsCommand`,
    `AckIncidentCommand`, `ResolveIncidentCommand`
  - `OpenApiConfig`, `WebConfig`
- **Endpoints** : `POST /api/chatops` (commandes : `list`, `stats`, `ack <id>`,
  `resolve <id>`, `help`) ; `/actuator/*`, `/swagger-ui.html`
- **Dépendances Maven** : `web`, `actuator`, `micrometer-registry-prometheus`,
  `springdoc-openapi`. **Ni `common`, ni ES, ni JPA** — client REST pur.
- **Communications** : → incident-service (`GET /api/incidents?status=OPEN`,
  `GET /api/incidents/stats`, `PUT /api/incidents/{id}/ack|resolve`).

#### Vérification de la séparation des responsabilités

- monitoring-service : aucune JPA, aucun notifier — ingestion/détection seules. ✔
- incident-service : aucun parser ni détecteur — corrélation/notifications seules. ✔
- chatops-service : aucune persistance, aucune dépendance à `common` — délégation
  REST pure (réponses lues en `JsonNode`). ✔
- `common` : uniquement modèles/DTO/enums + `ElasticsearchSslConfig` — aucun
  endpoint, aucune logique métier. ✔
- Graphe de dépendances Maven : `monitoring → common`, `incident → common`,
  `chatops → ∅` (aucune dépendance inter-service à la compilation — le couplage
  est uniquement à l'exécution, via HTTP).

### Prompt 4 — Cycle de vie d'un incident (diagramme d'états)

**États** (`common` → `IncidentStatus`) : `OPEN`, `ACKNOWLEDGED`, `RESOLVED`.
**État initial** : `OPEN` — posé dans `IncidentService.createNew()`.
**État terminal** : `RESOLVED` — aucune transition de réouverture n'existe.
**Événements de notification** (`IncidentEvent`) : `CREATED`, `RECURRED`, `RESOLVED`.

#### Transitions — format `état → action/condition → état`

- `∅ → anomalie reçue, aucun incident actif avec ce fingerprint → OPEN`
  — `InternalAnomalyController` (`POST /internal/anomalies`) →
  `IncidentService.handle()` → `createNew()` : `status=OPEN`,
  `occurrenceCount=1`, `firstSeen=lastSeen=now` → `dispatch(CREATED)` +
  `auditService.record("INCIDENT_CREATED", actor="system")`.
- `OPEN → récurrence (même fingerprint, statut OPEN/ACK) → OPEN` *(auto-boucle)*
  — `IncidentService.updateRecurring()` : `occurrenceCount+1`, `lastSeen=now`,
  `severity = Severity.max(ancienne, nouvelle)`, `description` rafraîchie,
  `logId` ajouté à `relatedLogIds` → `dispatch(RECURRED)`. Le statut ne change pas.
- `ACKNOWLEDGED → récurrence → ACKNOWLEDGED` *(auto-boucle)* — même méthode
  `updateRecurring()` ; la dédup inclut les ACKNOWLEDGED.
- `OPEN → PUT /api/incidents/{id}/ack → ACKNOWLEDGED`
  — `IncidentController.acknowledge()` → `IncidentService.acknowledge()` ;
  audit `INCIDENT_ACKNOWLEDGED` (actor="user") ; aucun événement de notification.
- `OPEN → PUT /api/incidents/{id}/resolve → RESOLVED` — résolution directe
  autorisée sans passage par ACKNOWLEDGED.
- `ACKNOWLEDGED → PUT /api/incidents/{id}/resolve → RESOLVED`
  — `IncidentController.resolve()` → `IncidentService.resolve()` :
  `metrics.incidentResolved()` → `dispatch(RESOLVED)` (commentaire Jira) →
  audit `INCIDENT_RESOLVED` (actor="user").
- `RESOLVED → nouvelle anomalie même fingerprint → ∅ (nouvel incident OPEN)`
  — la requête `findFirstByFingerprintAndStatusIn(fingerprint, [OPEN,
  ACKNOWLEDGED])` exclut RESOLVED → `createNew()` crée un **nouvel** incident
  (pas de réouverture).

#### Gardes (conditions bloquantes)

- `acknowledge()` : `status != OPEN` → `IllegalArgumentException` → HTTP 400
  (`GlobalExceptionHandler`). On ne peut pas acquitter un ACKNOWLEDGED ni un RESOLVED.
- `resolve()` : `status == RESOLVED` → `IllegalArgumentException` → HTTP 400.
- `id` inexistant → `Optional.empty()` → HTTP 404.
- Récurrence : seuls OPEN/ACKNOWLEDGED participent à la déduplication.

#### Escalade de sévérité

- `Severity` : `LOW < MEDIUM < HIGH < CRITICAL` (ordre ordinal), `max()` retourne
  le plus élevé — la sévérité **ne peut que monter** sur récurrence.
- Conséquence implémentée : si l'escalade atteint HIGH/CRITICAL,
  `JiraNotifier.supports()` devient vrai → sur `RECURRED`, si `jiraTicketKey ==
  null`, un ticket Jira est créé (commentaire du code : « la sévérité a pu être
  escaladée après la création »). `TeamsNotifier` reste silencieux sur RECURRED
  (anti-spam, CREATED uniquement).

#### Schéma d'états suggéré

```
              anomalie (fingerprint inconnu)
                    │
                    ▼
        ┌───────────────────────┐
   ┌──▶ │         OPEN          │ ──ack──▶ ┌───────────────┐
   │    │  recurrence: count+1, │          │ ACKNOWLEDGED  │
   │    │  severity=max (boucle)│          │ (boucle récur.)│
   │    └──────────┬────────────┘          └──────┬────────┘
   │             │ resolve                       │ resolve
   │             ▼                               ▼
   │        ┌─────────────────────────────────────┐
   │        │             RESOLVED                │  (terminal)
   │        └─────────────────────────────────────┘
   │             │ nouvelle anomalie même fingerprint
   └─────────────┘ → crée un NOUVEL incident OPEN (pas de réouverture)

Gardes : ack refuse non-OPEN (400) · resolve refuse RESOLVED (400) · id absent → 404
```

### Prompt 5 — Corrélation / déduplication des incidents

Mécanisme implémenté dans `IncidentService` (incident-service), déclenché par
`InternalAnomalyController` (`POST /internal/anomalies`).

#### Construction du fingerprint

- **Méthode** : `IncidentService.buildFingerprint(AnomalyReport)` —
  `fingerprint = report.type() + "::" + report.source()`.
- **Données utilisées** : uniquement le **type d'anomalie** (ex.
  `STACK_TRACE_EXCEPTION`, `ERROR_RATE`, `KEYWORD`) et la **source** du log
  (ex. nom du service/fichier). Ni la description ni la sévérité n'entrent
  dans le fingerprint — deux anomalies du même type sur la même source
  corrèlent toujours, même si le message diffère.

#### Recherche de l'incident existant

- **Méthode** : `IncidentRepository.findFirstByFingerprintAndStatusIn(
  fingerprint, ACTIVE_STATUSES)` — requête Spring Data Elasticsearch sur
  l'index **`incidents`**.
- **Statuts pris en compte** : `ACTIVE_STATUSES = [OPEN, ACKNOWLEDGED]`
  (constante de `IncidentService`). Un incident RESOLVED est **exclu** →
  une récurrence après résolution crée un nouvel incident.

#### Si un incident correspondant existe → `updateRecurring()`

- `lastSeen = Instant.now()`
- `occurrenceCount + 1`
- `severity = Severity.max(ancienne, report.severity())` — escalade
  monotone, jamais de déclassement (`LOW < MEDIUM < HIGH < CRITICAL`)
- `description` remplacée par celle du dernier rapport
- `logId` ajouté à `relatedLogIds` (traçabilité log ↔ incident)
- Sauvegarde ES → `NotificationHub.dispatch(incident, RECURRED)`
- **Le statut ne change pas** (OPEN reste OPEN, ACK reste ACK).

#### Si aucun incident → `createNew()`

- Nouvel `Incident` : `fingerprint`, `type`, `severity`, `status=OPEN`,
  `source`, `description`, `firstSeen=lastSeen=now`, `occurrenceCount=1`,
  `relatedLogIds=[logId]` → sauvegarde ES → `metrics.incidentCreated()` →
  `dispatch(CREATED)` → `AuditService.record("INCIDENT_CREATED")` (PostgreSQL).

#### Rôle de Jira lors d'une récurrence (`JiraNotifier.notify()`)

- `supports()` : uniquement si sévérité HIGH ou CRITICAL.
- `jiraTicketKey != null` → **commentaire** sur le ticket existant :
  `POST {baseUrl}/rest/api/2/issue/{key}/comment` avec le corps
  `🔁 Récurrence — occurrence n°{occurrenceCount} (dernière vue : {lastSeen})`.
- `jiraTicketKey == null` (sévérité escaladée après création) →
  **création du ticket** `POST /rest/api/2/issue` (issuetype Bug, résumé
  `[SEVERITY] type sur source`), la clé retournée est persistée sur
  l'incident (`incidentRepository.save`).
- `TeamsNotifier` : silencieux sur RECURRED (n'agit que sur CREATED).
- Chaque tentative → `NotificationRecord` en PostgreSQL (succès/échec).

#### Diagramme de séquence suggéré

```
monitoring-service          incident-service              Elasticsearch      Jira/Teams
      │                          │                             │                │
      │  POST /internal/anomalies│                             │                │
      │  (AnomalyReport)         │                             │                │
      ├─────────────────────────▶│                             │                │
      │                          │ fingerprint = type::source  │                │
      │                          │ findByFingerprintAndStatusIn│                │
      │                          ├────────────────────────────▶│                │
      │                          │◀── incident actif ou ∅ ─────┤                │
      │                          │                             │                │
      │              ┌───────────┴────────────┐                │                │
      │              │ trouvé                 │ absent         │                │
      │              ▼                        ▼                │                │
      │        updateRecurring           createNew             │                │
      │        count+1, lastSeen,        OPEN, count=1,        │                │
      │        severity=max              audit_log             │                │
      │              │                        │                │                │
      │              └──────────┬─────────────┘                │                │
      │                         ▼                              │                │
      │              NotificationHub.dispatch (@Async)         │                │
      │                         │                              │                │
      │              RECURRED: commentaire Jira ───────────────┼───────────────▶│
      │              (ou création si escalade HIGH/CRITICAL)   │                │
      │              CREATED: Teams + ticket Jira ─────────────┼───────────────▶│
      │                         │                              │                │
      │                         └──▶ notification_record (PostgreSQL)           │
```

### Prompt 6 — Modèle de données

Deux stockages : **Elasticsearch** (documents, index `logs` + `incidents`) et
**PostgreSQL/H2 via JPA** (tables `audit_logs`, `notification_records`).

#### Documents Elasticsearch

**`LogEntry`** — `@Document(indexName = "logs")` (module `common`) :

| Champ | Type ES | Rôle |
|---|---|---|
| `id` | `@Id` (String, généré par ES) | identifiant du log |
| `timestamp` | Date | date du log (posée par le parser ou le client) |
| `level` | Keyword | INFO / WARN / ERROR / DEBUG |
| `source` | Keyword | origine du log (`@JsonAlias("service")` accepté en entrée) |
| `message` | Text | contenu brut |

**`Incident`** — `@Document(indexName = "incidents")` (module `common`) :

| Champ | Type ES | Rôle |
|---|---|---|
| `id` | `@Id` (String, généré par ES) | identifiant de l'incident |
| `fingerprint` | Keyword | `type::source` — clé de déduplication |
| `type` | Keyword | type d'anomalie (`STACK_TRACE_EXCEPTION`, `ERROR_RATE`, `KEYWORD`…) |
| `severity` | Keyword (enum) | LOW / MEDIUM / HIGH / CRITICAL |
| `status` | Keyword (enum) | OPEN / ACKNOWLEDGED / RESOLVED |
| `source` | Keyword | source à l'origine de l'incident |
| `description` | Text | description de la dernière anomalie |
| `firstSeen` / `lastSeen` | Date | première / dernière occurrence |
| `occurrenceCount` | Integer | nombre de récurrences |
| `jiraTicketKey` | Keyword | clé du ticket Jira (ex. `OPS-123`) — lien incident ↔ Jira |
| `relatedLogIds` | Keyword (liste) | ids des `LogEntry` ayant déclenché l'incident — lien incident ↔ logs |

#### Entités JPA (PostgreSQL prod / H2 dev — incident-service)

**`AuditLog`** — `@Table(name = "audit_logs")` :

| Colonne | Rôle |
|---|---|
| `id` | Long, `@GeneratedValue(IDENTITY)` |
| `action` | INCIDENT_CREATED / INCIDENT_ACKNOWLEDGED / INCIDENT_RESOLVED / NOTIFICATION_SENT |
| `entity_type` | INCIDENT / LOG / NOTIFICATION |
| `entity_id` | id de l'entité concernée (ex. id d'incident ES) |
| `details` | texte libre (description de l'anomalie à la création) |
| `performed_by` | `system` / `user` / `chatops` |
| `created_at` | Instant, auto via `@PrePersist` |

**`NotificationRecord`** — `@Table(name = "notification_records")` :

| Colonne | Rôle |
|---|---|
| `id` | Long, `@GeneratedValue(IDENTITY)` |
| `channel` | TEAMS / JIRA (dérivé du nom du notifier) |
| `incident_id` | id de l'incident ES concerné |
| `event_type` | CREATED / RECURRED / RESOLVED |
| `success` | booléen |
| `error_message` | message d'erreur si échec |
| `external_ref` | clé du ticket Jira / référence externe |
| `sent_at` | Instant, auto via `@PrePersist` |

#### Relations entre les données

- `Incident.relatedLogIds` → liste d'`id` de `LogEntry` (ES `logs`) — **lien
  incident → logs déclencheurs** (relation logique, pas de FK : deux index ES).
- `Incident.jiraTicketKey` → clé du ticket Jira externe — **lien incident → Jira**.
- `Incident.fingerprint` = `type::source` — relie toutes les occurrences d'une
  même anomalie au même incident actif.
- `NotificationRecord.incidentId` → `Incident.id` — **lien notification → incident**.
- `NotificationRecord.externalRef` → référence externe (ticket Jira).
- `AuditLog.entityType` + `entityId` → référence polymorphe vers un incident
  (`INCIDENT` + id ES), un log ou une notification.
- `AnomalyReport.logId` (DTO de transit) → alimente `relatedLogIds`.

#### Version simplifiée pour le mémoire (M2)

```
┌─────────────────────┐          ┌─────────────────────────┐
│   LogEntry (ES)     │          │    Incident (ES)        │
│─────────────────────│          │─────────────────────────│
│ id (PK)             │◀─────────│ relatedLogIds [ids]     │
│ timestamp           │  1..n    │ id (PK)                 │
│ level               │          │ fingerprint (type::src) │
│ source              │          │ type, severity, status  │
│ message             │          │ source, description     │
└─────────────────────┘          │ firstSeen, lastSeen     │
                                 │ occurrenceCount         │
                                 │ jiraTicketKey ──────────┼──▶ Ticket Jira (externe)
                                 └───────────┬─────────────┘
                                             │ id
                    ┌────────────────────────┼────────────────────────┐
                    ▼                        │                        ▼
        ┌───────────────────────┐            │         ┌────────────────────────┐
        │ NotificationRecord    │            │         │ AuditLog (JPA)         │
        │ (JPA)                 │            │         │────────────────────────│
        │───────────────────────│            │         │ id (PK)                │
        │ id (PK)               │            │         │ action                 │
        │ channel (TEAMS/JIRA)  │            │         │ entityType + entityId ─┤ (→ Incident.id)
        │ incidentId ───────────┼────────────┘         │ details                │
        │ eventType             │                      │ performedBy            │
        │ success, errorMessage │                      │ createdAt              │
        │ externalRef (Jira key)│                      └────────────────────────┘
        │ sentAt                │
        └───────────────────────┘
```

À retenir pour la figure : **ES = données métier** (logs, incidents), **JPA =
métadonnées** (audit, notifications) ; les liens sont logiques (ids stockés),
jamais des clés étrangères — cohérent avec une architecture microservices où
chaque store est indépendant.

### Prompt 7 — Architecture de déploiement (Docker)

Fichier : `infra/docker-compose.yml` — **8 conteneurs**, un réseau bridge par
défaut (résolution DNS par nom de service), 3 volumes nommés.

#### Conteneurs

| Conteneur | Service compose | Image / Build | Port hôte → conteneur | depends_on | Volume |
|---|---|---|---|---|---|
| `obs-elasticsearch` | elasticsearch | `docker.elastic.co/elasticsearch/elasticsearch:9.0.0` | 9200 → 9200 | — | `esdata` |
| `obs-postgres` | postgres | `postgres:16-alpine` | 5432 → 5432 | — | `pgdata` |
| `obs-monitoring` | monitoring-service | build multi-stage : `eclipse-temurin:21-jdk` → `21-jre` | 8081 → 8081 | elasticsearch (healthy), incident-service | — |
| `obs-incident` | incident-service | build multi-stage : `eclipse-temurin:21-jdk` → `21-jre` | 8082 → 8082 | elasticsearch (healthy), postgres (healthy) | — |
| `obs-chatops` | chatops-service | build multi-stage : `eclipse-temurin:21-jdk` → `21-jre` | 8083 → 8083 | incident-service | — |
| `obs-frontend` | frontend | build multi-stage : `node:20-alpine` → `nginx:alpine` | 8090 → 80 | monitoring, incident, chatops | — |
| `obs-prometheus` | prometheus | `prom/prometheus:latest` | 9091 → 9090 | monitoring, incident, chatops | `prometheus.yml` monté |
| `obs-grafana` | grafana | `grafana/grafana:latest` | 3000 → 3000 | prometheus | `grafanadata` + provisioning monté |

Healthchecks : Elasticsearch (`/_cluster/health` green/yellow) et PostgreSQL
(`pg_isready`) — les services Spring attendent `service_healthy`.

#### Communications — format `Conteneur → protocole/port → Conteneur`

Entrées externes :

- Navigateur → `HTTP :8090` → `obs-frontend`
- Producteurs de logs → `HTTP :8081 /api/logs/*` → `obs-monitoring`
- Navigateur → `HTTP :3000` → `obs-grafana` · `HTTP :9091` → `obs-prometheus`

Routage frontend (nginx, `frontend/dashboard/nginx.conf`) :

- `obs-frontend` → `HTTP :8081 /api/logs*` → `obs-monitoring`
- `obs-frontend` → `HTTP :8083 /api/chatops` → `obs-chatops`
- `obs-frontend` → `HTTP :8082 /api/*` → `obs-incident`

Inter-services :

- `obs-monitoring` → `HTTP :8082 POST /internal/anomalies` → `obs-incident`
- `obs-chatops` → `HTTP :8082 /api/incidents*` → `obs-incident`

Persistance :

- `obs-monitoring` → `HTTP :9200` → `obs-elasticsearch` (index `logs`)
- `obs-incident` → `HTTP :9200` → `obs-elasticsearch` (index `incidents`)
- `obs-incident` → `JDBC :5432` → `obs-postgres` (`audit_logs`, `notification_records`)

Sorties externes (hors réseau compose) :

- `obs-incident` → `HTTPS` → Jira (`/rest/api/2/issue*`, Basic auth)
- `obs-incident` → `HTTPS` → Teams (webhook entrant)

Méta-observabilité :

- `obs-prometheus` → `HTTP :8081|8082|8083 /actuator/prometheus` (pull, 15 s) → les 3 services
- `obs-grafana` → `HTTP :9090` → `obs-prometheus` (datasource provisionnée)

#### Ordre de démarrage (graphe `depends_on`)

```
elasticsearch ─┬─▶ monitoring-service ─┐
               └─▶ incident-service ───┼─▶ frontend
postgres ────────▶ incident-service ───┴─▶ chatops-service
3 services ──────────────────────────────▶ prometheus ─▶ grafana
```

#### Schéma de déploiement suggéré

```
                    ┌─────────────────────────────────────────────┐
   Navigateur ─────▶│  obs-frontend (nginx :80, exposé :8090)     │
                    └──┬──────────────┬───────────────┬───────────┘
              /api/logs│      /api/*  │    /api/chatops│
                       ▼              ▼               ▼
              ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
   logs ─────▶│obs-monitoring│ │ obs-incident │◀│ obs-chatops  │
   :8081      │    :8081     │ │    :8082     │ │    :8083     │
              └──┬────────┬──┘ └──┬───┬───┬───┘ └──────────────┘
                 │        │       │   │   │
        ES :9200 │        │POST   │   │   │ JDBC :5432
                 ▼        │/internal│  │   ▼
              ┌──────────┐│/anomalies│  │ ┌────────────┐
              │obs-elastic│◀──────────┘  │ │obs-postgres│
              │  :9200   │◀──────────────┘ │  :5432     │
              └──────────┘   ES :9200      └────────────┘
                                   │
              obs-incident ──HTTPS──▶ Jira / Teams (externes)
                                   │
   ┌─────────────┐   scrape 15s    │
   │obs-prometheus│◀── /actuator/prometheus ── 3 services (:8081-83)
   │   :9090     │◀── obs-grafana :3000 (PromQL)
   └─────────────┘
```

### Prompt 8 — Figures techniques recommandées pour le mémoire

Liste triée par pertinence. Objectif M2 : 6-8 figures techniques maximum —
chaque figure doit démontrer quelque chose qu'un texte ne peut pas montrer.

#### À inclure (forte valeur)

**Figure 1 — Architecture de déploiement de la plateforme**

- **Section** : §5 (conception) ou §7 (implémentation) — une seule fois.
- **Contenu** : les 8 conteneurs avec ports exposés, flux HTTP internes,
  sorties HTTPS vers Jira/Teams, scraping Prometheus. Fusionner les contenus
  des Prompts 1 et 7 en **une seule figure** (deux figures quasi identiques
  = redondance).
- **Source** : `infra/docker-compose.yml`, `frontend/dashboard/nginx.conf`,
  `infra/prometheus.yml`, Dockerfiles.
- **Format** : diagramme (draw.io / PlantUML deployment).
- **Valeur** : essentielle — c'est LA figure qui prouve l'architecture
  microservices réelle.

**Figure 2 — Pipeline de traitement d'un log**

- **Section** : §6 ou §7 (fonctionnement interne).
- **Contenu** : chaîne `Log brut → LogIngestionController → LogParsingService
  (3 parsers @Order) → LogEntry → ES → AnomalyDetectionService (3 détecteurs)
  → AnomalyReport → POST /internal/anomalies`. Voir Prompt 2.
- **Source** : `LogIngestionController`, `LogParsingService`,
  `AnomalyDetectionService`, `IncidentClient`.
- **Format** : diagramme de flux horizontal.
- **Valeur** : forte — montre le pipeline interne que les outils existants
  ne fournissent pas (c'est votre contribution).

**Figure 3 — Diagramme d'états du cycle de vie d'un incident**

- **Section** : §7.2.4 (gestion des incidents).
- **Contenu** : OPEN → ACKNOWLEDGED → RESOLVED, auto-boucles de récurrence,
  gardes (400 sur transitions invalides), RESOLVED terminal → nouvel incident.
  Voir Prompt 4.
- **Source** : `IncidentStatus`, `IncidentService.acknowledge()/resolve()/
  updateRecurring()`.
- **Format** : diagramme d'états UML.
- **Valeur** : forte — figure classique, facile à défendre en soutenance.

**Figure 4 — Diagramme de séquence : corrélation/déduplication**

- **Section** : §7.2.4 (à côté de la figure 3) — ou §5.1.1 pour corriger
  l'affirmation « recherche dans Jira ».
- **Contenu** : `POST /internal/anomalies` → fingerprint `type::source` →
  requête ES `[OPEN, ACKNOWLEDGED]` → branchement updateRecurring/createNew →
  NotificationHub → Jira/Teams → `notification_records`. Voir Prompt 5.
- **Source** : `IncidentService.handle()`, `IncidentRepository`,
  `NotificationHub`, `JiraNotifier`.
- **Format** : diagramme de séquence UML.
- **Valeur** : forte — c'est le mécanisme le plus original du projet ;
  corrige en même temps l'erreur du mémoire sur la dédup Jira.

**Figure 5 — Modèle de données**

- **Section** : §7.2.3 (modélisation des données).
- **Contenu** : 2 documents ES (`LogEntry`, `Incident`) + 2 tables JPA
  (`audit_logs`, `notification_records`) + liens logiques (`relatedLogIds`,
  `jiraTicketKey`, `incidentId`, `entityId`). Voir Prompt 6.
- **Source** : classes `LogEntry`, `Incident`, `AuditLog`,
  `NotificationRecord`.
- **Format** : diagramme entité-association adapté (pas un vrai MCD —
  préciser « liens logiques, pas de FK »).
- **Valeur** : moyenne-forte — nécessaire si §7.2.3 existe ; montre la
  séparation données métier (ES) / métadonnées (JPA).

**Figure 6 — Captures du dashboard React**

- **Section** : §5.3.1 (IHM).
- **Contenu** : 2 captures maximum — page Dashboard (KPI + graphiques) et
  page Incidents (badges, actions ack/resolve). Éventuellement ChatOps.
- **Source** : application lancée (`docker compose up`, `http://localhost:8090`).
- **Format** : captures d'écran.
- **Valeur** : forte — le dashboard n'est pas mentionné dans le mémoire ;
  une capture le rend concret immédiatement.

**Figure 7 — Résultats de l'évaluation (précision/rappel/F1)**

- **Section** : §8.4 ou §9 (résultats).
- **Contenu** : tableau ou bar chart des métriques par détecteur / global,
  éventuellement moyenne ± écart-type sur N runs.
- **Source** : sortie de `scripts/run_evaluation.ps1` + `data/annotations/
  ground_truth.csv`. ⚠️ Nécessite d'exécuter la campagne d'abord.
- **Format** : tableau ou graphique.
- **Valeur** : forte — un mémoire M2 sans résultats chiffrés est faible ;
  c'est la figure qui manque le plus actuellement.

#### Optionnelles (seulement si la section existe)

- **Capture Grafana** (§7 méta-observabilité) — une capture du dashboard
  `observability.json` si vous parlez de l'auto-supervision. Sinon, inutile.
- **Architecture interne des modules Maven** (Prompt 3) — seulement si une
  section « conception détaillée » existe ; sinon redondante avec la figure 1.
- **Capture Swagger UI** — faible valeur, une ligne de texte suffit.

#### À éviter (redondant ou trop faible)

- **Diagramme de classes complet** — trop détaillé pour un M2, personne ne
  le lit ; les figures 2-5 couvrent déjà la conception.
- **Figure CI/CD** — le pipeline GitHub Actions se décrit en 3 lignes de
  texte ; une figure n'apporte rien.
- **Diagramme de séquence ChatOps** — le flux est mince (commande → REST →
  réponse) ; une phrase suffit.
- **Graphe de dépendances Maven** — une phrase (« monitoring et incident
  dépendent de common, chatops est autonome ») suffit.
- **Captures de toutes les pages du dashboard** — 2 captures suffisent ;
  au-delà c'est du remplissage.

### Prompt 9 — Placement des figures dans le sommaire réel du mémoire

Le mémoire ne contient actuellement que **2 figures** (liste des figures, p. iv) :
Figure 1 « Architecture logicielle et organisation interne des microservices »
(§6.1, p. 22) et Figure 2 « Architecture technique de la plateforme » (§6.2,
p. 23). Voici quoi mettre dans chaque section et où placer les nouvelles figures.

#### Figures déjà prévues — à remplir

- **§6.1 Architecture logicielle** → Figure 1 : architecture interne des
  microservices — modules Maven, classes principales, endpoints, dépendances
  (`monitoring → common`, `incident → common`, `chatops autonome`).
  Contenu prêt : **Prompt 3**.
- **§6.2 Architecture technique** → Figure 2 : architecture de déploiement —
  8 conteneurs, ports, flux HTTP/JDBC/HTTPS, scraping Prometheus.
  Contenu prêt : **Prompts 1 + 7 fusionnés**.

#### Figures à ajouter — par section

- **§5.3.1 IHM** → 2 captures d'écran : page Dashboard (KPI + graphiques) et
  page Incidents (badges sévérité/statut, boutons ack/resolve). La section
  existe mais le dashboard n'y est pas décrit — à rédiger aussi.
- **§7.2.3 Modélisation des données** → figure modèle de données : 2 documents
  ES + 2 tables JPA + liens logiques. Contenu prêt : **Prompt 6**.
- **§7.2.4 Réalisation des cas d'utilisation** → 3 figures :
  1. pipeline de traitement d'un log (**Prompt 2**) — couvre le cas
     « détection et création automatique » (§5.1.1) ;
  2. diagramme d'états OPEN → ACKNOWLEDGED → RESOLVED (**Prompt 4**) ;
  3. diagramme de séquence corrélation/déduplication (**Prompt 5**) —
     à placer ici ou en §5.1.1 pour corriger l'affirmation « recherche Jira ».
- **§7.2.5 Composants et leur déploiement** → optionnel : graphe `depends_on`
  ou extrait annoté du `docker-compose.yml`. Une figure suffit si §6.2 a déjà
  le schéma complet — sinon redondant.
- **§8.1/8.2 Tests** → **tableau** (pas figure) : les 65 tests par module et
  suite. Contenu prêt : section §8 du présent document.
- **§8.4 Tests de charge** → tableau/graphique de résultats — ⚠️ à générer
  (script de charge à écrire, ex. N requêtes bulk + mesure du débit).
- **§9 Évaluation** (ou §8.3) → tableau/graphique précision/rappel/F1 —
  ⚠️ à générer via `scripts/run_evaluation.ps1` + `ground_truth.csv`.

#### Contenu textuel à corriger/ajouter (sans figure)

- **§5.1.1** — remplacer « recherche dans Jira » par déduplication par
  fingerprint dans Elasticsearch (voir correction n°2 de ce document).
- **§5.3.1** — décrire le dashboard React (5 pages : Dashboard, Logs,
  Incidents, ChatOps, Audit).
- **§5.2.x** — fiabilité : isolation de panne (ingestion survit à
  incident-service down), timeouts 5 s/10 s, gardes d'état 400.
- **§7.1.2** — stack réelle : Spring Boot 4.1.0, Java 21, ES 9.0.0,
  PostgreSQL 16, React 19, Prometheus + Grafana.
- **§7.2.x** — ajouter : méta-observabilité (Actuator → Prometheus → Grafana),
  audit JPA (`audit_logs`, `notification_records`), Swagger par service,
  `GlobalExceptionHandler`, CORS.
- **§8** — CI GitHub Actions (3 jobs) + 65 tests.
- **§9.3** — perspectives : auth sur les API, file d'attente/rate limiting,
  retry/circuit breaker, Kubernetes, Kibana si déployé.

#### Récapitulatif — nombre de figures final

| Section | Figure | Statut |
|---|---|---|
| §6.1 | Architecture interne microservices | existante, à remplir (Prompt 3) |
| §6.2 | Architecture de déploiement | existante, à remplir (Prompts 1+7) |
| §5.3.1 | 2 captures dashboard | à faire (captures) |
| §7.2.3 | Modèle de données | à faire (Prompt 6) |
| §7.2.4 | Pipeline log + états + séquence dédup | à faire (Prompts 2, 4, 5) |
| §8.4 | Résultats charge | à générer |
| §9 | Résultats évaluation P/R/F1 | à générer |

Total : **~8 figures + 2 tableaux** — cohérent pour un M2 sans surcharge.
