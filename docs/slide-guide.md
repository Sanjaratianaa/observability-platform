# Guide de création des slides de soutenance

> **2 présentations à préparer :**
> 1. **Slides de soutenance** (ce fichier) — ~20 slides, présentation du projet
> 2. **Slides de démo live** (`docs/slide-demo-live.md`) — ~17 slides, guide la démo en direct le jour J
>
> Les deux peuvent être créées au bureau sans Docker. Seules les captures
> d'écran (placeholders) seront ajoutées à la maison.

Tu peux faire ce travail au bureau **sans Docker** — il faut juste le contenu du projet
(ton repo sur OneDrive) et un outil de slides (PowerPoint/Google Slides).

---

## Structure recommandée (~15-20 slides)

### Slide 1 — Page de titre
- **Titre** : Plateforme d'observabilité pour la supervision proactive des systèmes distribués
- Ton nom, MBDS M2, année, logo ValueIT
- Nom du tuteur/encadrant

### Slide 2 — Contexte & Problématique
- ValueIT : crawling distribué, millions de logs/jour
- Problème : détection manuelle, incidents manqués, temps de réaction long
- Question : comment automatiser la détection, la corrélation et la notification ?

### Slide 3 — Objectifs
- Pipeline automatisé : ingestion → détection → incident → notification
- Déduplication intelligente (pas de doublons)
- Notifications Jira + Teams en temps réel
- Dashboard web + interface ChatOps
- Méta-observabilité (la plateforme se supervise elle-même)

### Slide 4 — Architecture globale
- Schéma des 8 conteneurs Docker (tu peux le dessiner dans PowerPoint)
- Copie le diagramme ASCII de `docs/thesis-infos-manquantes.md` (Prompt 1) comme base
- Composants : Frontend, 3 microservices, ES, PostgreSQL, Prometheus, Grafana
- Flèches : HTTP REST entre les services

### Slide 5 — Stack technique
| Composant | Technologie |
|-----------|-------------|
| Backend | Java 21, Spring Boot 4.1.0, Maven multi-module |
| Stockage | Elasticsearch 9.0.0 (logs + incidents) |
| Métadonnées | PostgreSQL 16 (audit, notifications) |
| Frontend | React 19, Vite, Tailwind CSS 4, Recharts |
| Monitoring | Prometheus + Grafana |
| CI/CD | GitHub Actions |
| Déploiement | Docker Compose (8 conteneurs) |

### Slide 6 — Pipeline de traitement (flux d'un log)
- Schéma vertical simplifié :
  1. Log brut → Parsing (JSON/Syslog/Apache)
  2. → Détection (3 algorithmes)
  3. → Fingerprint + déduplication ES
  4. → Création/mise à jour incident
  5. → Notification Jira + Teams
- Reprendre le schéma ASCII de `thesis-infos-manquantes.md` (Prompt 2)

### Slide 7 — Détection d'anomalies (3 algorithmes)
| Détecteur | Méthode | Seuil |
|-----------|---------|-------|
| ErrorRate | Fenêtre glissante 5 min/source | ≥30% → HIGH, ≥50% → CRITICAL |
| Keyword | Mots-clés (OOM, deadlock, timeout) | Présence → MEDIUM |
| StackTrace | Regex exception Java | Présence → HIGH |

### Slide 8 — Corrélation & Déduplication
- Fingerprint = `type + "::" + source`
- Recherche ES : incident OPEN ou ACKNOWLEDGED avec même fingerprint
- Trouvé → mise à jour (count++, escalade sévérité)
- Absent → création d'un nouvel incident
- **Pas de recherche dans Jira** — tout dans Elasticsearch

### Slide 9 — Notifications (Jira + Teams)
- Jira : création ticket (HIGH/CRITICAL), commentaires sur récurrence
- Teams : webhook Adaptive Card sur création
- Isolation de panne : échec d'un notifier n'affecte pas les autres
- Historique : chaque envoi persisté en PostgreSQL
- 📸 *[Capture Teams/Jira à insérer cet après-midi]*

### Slide 10 — Interface Web (Dashboard)
- 5 pages : Dashboard, Logs, Incidents, ChatOps, Audit
- 📸 *[Capture Dashboard à insérer]*
- 📸 *[Capture Incidents à insérer]*

### Slide 11 — Interface ChatOps
- Commandes : `help`, `list`, `stats`, `ack <id>`, `resolve <id>`
- Terminal intégré dans le frontend
- Délègue à incident-service via REST
- 📸 *[Capture ChatOps à insérer]*

### Slide 12 — Cycle de vie d'un incident
- Diagramme d'états : OPEN → ACKNOWLEDGED → RESOLVED
- Actions : création auto, acquittement manuel, résolution manuelle
- Audit trail complet en PostgreSQL
- Reprendre le contenu de `thesis-infos-manquantes.md` (Prompt 4)

### Slide 13 — Méta-observabilité
- Chaque service expose `/actuator/prometheus`
- Prometheus scrape toutes les 15s
- Grafana pour la visualisation
- Métriques custom : `obs_incidents_created_total`, `obs_notifications_sent_total`...
- 📸 *[Capture Prometheus/Grafana à insérer]*

### Slide 14 — Tests unitaires
- 65 tests JUnit 5 + Mockito
- Répartition par module (voir tableau dans thesis-infos-manquantes §8)
- CI/CD : GitHub Actions (build backend + frontend + Docker)
- 📸 *[Capture `mvnw verify` à insérer]*

### Slide 15 — Évaluation (Précision/Rappel/F1)
- Script `run_evaluation.ps1` : injection → comparaison ground truth
- 📸 *[Résultats P/R/F1 à insérer cet après-midi]*
- Interprétation des résultats

### Slide 16 — Tests de charge
- Script `load_test.ps1` : N batches × M logs
- Métriques : débit (logs/s), latence p95
- 📸 *[Résultats à insérer]*

### Slide 17 — Démonstration (slide de transition)
- "Démonstration en direct" ou lien vers la vidéo
- Points à montrer : injection → détection → incident → notification → résolution

### Slide 18 — Difficultés & Solutions
- ES 9.0.0 : démarrage lent, healthcheck adapté
- Quoting JSON sous Windows (curl.exe vs Invoke-RestMethod)
- Isolation de panne : monitoring continue si incident-service down
- Outbox pattern pour fiabilité

### Slide 19 — Perspectives
- File d'attente dédiée (Kafka/RabbitMQ)
- Rate limiting / circuit breaker
- Application mobile Android (Kotlin)
- Machine learning pour la détection
- Sauvegarde/restauration des données

### Slide 20 — Conclusion
- Plateforme fonctionnelle, déployable en un `docker compose up`
- Pipeline complet : ingestion → détection → corrélation → notification
- 3 microservices bien séparés + dashboard web
- Résultats d'évaluation (P/R/F1)
- Merci / Questions ?

---

## Ce que tu peux faire AU BUREAU (sans Docker) :

1. **Créer toutes les slides** avec le texte ci-dessus
2. **Dessiner les schémas** d'architecture et de pipeline (PowerPoint shapes)
3. **Laisser des placeholders** pour les captures (cadres vides avec légende)
4. **Relire/corriger le mémoire** (les points listés dans thesis-infos-manquantes)
5. **Préparer le script de démo** (relire demo-scenario.md)
6. **Rédiger les notes de présentation** pour chaque slide

## Ce que tu fais CET APRÈS-MIDI (avec Docker) :

1. Lancer Docker, attendre que tout soit UP
2. Suivre `demo-scenario.md` pour prendre les 14 captures
3. Insérer les captures dans les slides
4. Enregistrer la vidéo de démo si besoin
5. Lancer `run_evaluation.ps1 -Reset -Runs 3` et capturer les résultats
6. Lancer `load_test.ps1` et capturer les résultats
