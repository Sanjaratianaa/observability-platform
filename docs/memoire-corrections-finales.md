# Corrections finales du mémoire — Observability Platform

Ce document sert de guide pratique pour finaliser le mémoire `MémoireITU-v3r3_Last_Version.docx` à partir de l'implémentation réelle du projet `observability-platform`.

## 1. Ce qui semble déjà bien aligné

À partir de `thesis-extracted-v2.txt`, plusieurs corrections importantes paraissent déjà intégrées :

- l'approche **microservices** est assumée dans la structure générale ;
- la **liste des figures** est enrichie jusqu'à la figure 10 ;
- la section **5.3.1** parle désormais de l'**interface utilisateur web** ;
- la section **1.2** mentionne que, **dans le prototype, la plateforme intègre un module de monitoring qui reproduit le rôle de détection** ;
- le glossaire a été enrichi avec `fingerprint`, `Grafana`, `Prometheus`.

## 2. Corrections prioritaires avant dépôt

## 2.1 Résumé français — à corriger en priorité

Le résumé français est encore incohérent avec la version anglaise.

### Problème détecté

Le passage actuel se termine par :

> l’envoi de notifications instantanées sur Microsoft sans ouvrir les outils de monitoring existants.

Il manque :

- `Teams` ;
- l'idée de **l'interface conversationnelle** ;
- la formulation est incomplète par rapport à l'abstract.

### Texte conseillé

> Au sein de ValueIT, l’infrastructure de crawling assure quotidiennement la collecte des données e-commerce sur des milliers de sites web. Le suivi de cette infrastructure est actuellement assuré par l’équipe Data Monitoring, qui s’appuie sur des outils internes de détection d’anomalies permettant d’identifier rapidement les incidents de crawl. Cependant, le traitement qui suit cette détection — création manuelle des tickets Jira, absence de notification automatique des équipes et nécessité de consulter activement les outils de monitoring — reste aujourd’hui une charge de travail manuelle et chronophage. C’est pourquoi nous avons développé une plateforme ChatOps capable d’automatiser cette supervision à travers la création automatique de tickets Jira avec déduplication des incidents similaires, l’envoi de notifications instantanées sur Microsoft Teams et une interface conversationnelle permettant de consulter l’état de l’infrastructure sans ouvrir les outils de monitoring existants.

## 2.2 Section 2.3 — Jira ne fait pas la déduplication

Dans `thesis-extracted-v2.txt`, la section `2.3 ChatOps et interopérabilité des APIs` contient encore l'idée suivante :

> l’API REST de Jira ... est utilisée ... pour vérifier si un incident similaire a déjà été signalé.

### Pourquoi c’est faux

Dans l’application réelle :

- la **déduplication** se fait dans **Elasticsearch** ;
- l’empreinte est un **fingerprint** construit à partir de `type + "::" + source` ;
- Jira sert à **créer** ou **commenter** un ticket, pas à rechercher les doublons.

### Texte de remplacement conseillé

> L’API REST de Jira est utilisée dans le projet pour créer et mettre à jour les tickets liés aux incidents. En revanche, la vérification de similarité ne repose pas sur Jira : la plateforme calcule une empreinte d’incident (fingerprint) à partir du type d’anomalie et de sa source, puis recherche dans Elasticsearch si un incident actif portant cette même empreinte existe déjà.

## 2.3 Sections 5.1.1, 5.3.2 et 7.2.4 — harmoniser le mécanisme réel

Ces sections doivent toutes raconter le **même mécanisme**.

### Version correcte à conserver partout

- détection d’une anomalie ;
- construction d’un `AnomalyReport` ;
- calcul du fingerprint `type::source` ;
- recherche d’un incident `OPEN` ou `ACKNOWLEDGED` portant cette empreinte ;
- si trouvé : mise à jour de l’incident (`occurrenceCount`, `lastSeen`, sévérité) ;
- sinon : création d’un nouvel incident ;
- création ou commentaire Jira selon l’événement et la sévérité ;
- notification Teams à la création de l’incident.

### Paragraphe prêt à coller

> Avant toute création de ticket, la plateforme calcule une empreinte de l’incident à partir du type d’anomalie et de sa source. Cette empreinte, appelée fingerprint, est utilisée pour rechercher dans Elasticsearch un incident actif déjà existant. Si un incident portant la même empreinte est trouvé, il est mis à jour avec une nouvelle occurrence, une date de dernière observation actualisée et, si nécessaire, une sévérité plus élevée. Sinon, un nouvel incident est créé. Jira intervient ensuite pour l’ouverture d’un ticket ou l’ajout de commentaires de récurrence ou de résolution ; il ne constitue donc pas le mécanisme principal de déduplication.

## 2.4 Section 2.6 — CI/CD à reformuler

Le mémoire ancien parlait d'un pipeline « prévu » ou d'un choix dépendant de l'environnement.

### Version réelle

Le projet dispose déjà d'une base de **CI/CD via GitHub Actions** :

- build backend Maven multi-module ;
- build frontend ;
- validation Docker Compose.

### Texte conseillé

> L’intégration continue du projet repose sur GitHub Actions. Le pipeline automatise la compilation et l’exécution des tests du backend Maven multi-module, la construction du frontend React ainsi que la validation de la configuration Docker Compose. Cette chaîne permet de détecter rapidement les erreurs d’intégration et de sécuriser l’évolution du projet.

## 2.5 Section 2.7 — attention à ne pas promettre ce qui n’est pas implémenté

Le texte extrait parle encore de **file d’attente** ou de **rate limiting prévu**.

### Ce qu’il faut dire à la place

Dans l’implémentation actuelle, on peut dire :

- appels asynchrones ;
- isolation de panne ;
- outbox pour les anomalies côté monitoring ;
- retries côté transmission inter-service.

### Formulation recommandée

> Pour absorber les pics d’activité, la plateforme repose actuellement sur un découplage entre ingestion, détection et traitement des incidents. La transmission des anomalies vers le service d’incidents s’appuie sur un mécanisme de persistance temporaire de type outbox ainsi que sur des tentatives de réémission en cas d’échec. Des mécanismes plus avancés de limitation de débit ou de file d’attente dédiée pourront constituer une évolution future.

## 2.6 Section 8 — bien séparer ce qui est fait et ce qui reste à illustrer

Le mémoire doit distinguer :

- **tests réellement implémentés** ;
- **évaluation expérimentale réellement exécutée** ;
- **captures/figures encore à produire**.

### À écrire clairement

- les tests unitaires par module sont réalisés ;
- les scripts d’évaluation existent ;
- les résultats chiffrés finaux doivent venir d’une exécution effective si tu veux les afficher.

### Formulation prudente

> Le projet comprend un ensemble de tests unitaires répartis sur les différents microservices, couvrant notamment les parsers de logs, les détecteurs d’anomalies, la logique de gestion des incidents et le dispatch des commandes ChatOps. En complément, une campagne d’évaluation automatisée a été préparée à l’aide de scripts d’injection et de comparaison avec une vérité terrain. Les métriques de précision, rappel et F1 peuvent ainsi être calculées sur des jeux de données annotés.

## 2.7 Section 9.3 — bonnes perspectives à conserver

Les perspectives les plus crédibles par rapport au projet actuel sont :

- sécurisation des APIs ;
- limitation de débit / file de messages dédiée ;
- circuit breaker / résilience inter-services ;
- déploiement Kubernetes ;
- Kibana ou visualisations supplémentaires ;
- authentification utilisateur côté frontend et APIs.

## 3. Ce que tu peux garder tel quel

Tu peux globalement conserver :

- l’introduction ;
- la motivation métier ;
- le choix de Spring Boot ;
- le choix des microservices ;
- la présence d’Elasticsearch, Docker, Prometheus, Grafana ;
- la structure générale du mémoire ;
- les sections décrivant l’existant métier ValueIT.

## 4. Ce qu’il faut éviter dans la version finale

Évite absolument ces formulations :

- « Jira vérifie si un incident similaire existe déjà » ;
- « la plateforme remplace les outils de monitoring existants » ;
- « la file d’attente/rate limiting est implémentée » si ce n’est pas démontré ;
- « l’interface principale est uniquement ChatOps » alors que tu as aussi un dashboard web ;
- des chiffres d’évaluation non obtenus réellement.

## 5. Ordre de priorité conseillé

Si tu manques de temps, modifie dans cet ordre :

- **1. Résumé français**
- **2. Section 2.3**
- **3. Sections 5.1.1 / 5.3.2 / 7.2.4**
- **4. Section 2.7**
- **5. Section 2.6**
- **6. Section 8 / 9 si tu ajoutes les résultats expérimentaux**

## 6. Message de soutenance à retenir

La bonne histoire à raconter est la suivante :

> Les outils internes détectent déjà les anomalies, mais les actions qui suivent restent manuelles. Le projet apporte une couche d’automatisation : corrélation d’incidents, déduplication par fingerprint, création ou mise à jour de tickets Jira, notifications Teams, consultation via dashboard et ChatOps, le tout dans une architecture microservices observable et conteneurisée.
