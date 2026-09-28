# Plan de soutenance — Observability Platform

Ce document propose une structure de présentation claire, courte et crédible pour la soutenance.

## 1. Objectif de la présentation

En 10 à 15 minutes, il faut faire passer trois idées :

- **le besoin métier est réel** ;
- **la solution développée est cohérente techniquement** ;
- **le projet apporte un gain concret d’automatisation et d’observabilité**.

## 2. Structure conseillée des slides

## Slide 1 — Titre

### Titre conseillé

**Plateforme ChatOps de monitoring pour infrastructure de crawling distribuée**

### Sous-titre

- Nom
- Master 2 MBDS
- Entreprise : ValueIT
- Année : 2026

### À dire

- contexte stage ;
- sujet du projet ;
- objectif global de la soutenance.

## Slide 2 — Contexte métier

### Contenu

- ValueIT collecte des données e-commerce à grande échelle ;
- l’infrastructure de crawling produit beaucoup d’événements et d’incidents ;
- l’équipe Data Monitoring supervise cette infrastructure.

### À dire

- les anomalies existent déjà et sont détectées ;
- le problème principal se situe dans le **traitement après détection**.

## Slide 3 — Problématique

### Titre conseillé

**Problématique et limites de l’existant**

### Contenu

- création manuelle des tickets Jira ;
- absence de notification automatique ;
- consultation manuelle des outils de monitoring ;
- risque de doublons et perte de temps.

### Formule simple à dire

> Comment automatiser le traitement des incidents détectés afin de réduire les opérations manuelles et améliorer la réactivité des équipes ?

## Slide 4 — Objectifs du projet

### Contenu

- automatiser la création des incidents ;
- éviter les doublons ;
- notifier les équipes sur Teams ;
- permettre la consultation via ChatOps ;
- proposer une interface web de supervision.

### Astuce visuelle

Faire un bloc `Avant` / `Après`.

## Slide 5 — Solution proposée

### Titre conseillé

**Vue d’ensemble de la solution**

### Contenu

- `monitoring-service`
- `incident-service`
- `chatops-service`
- `frontend dashboard`
- Elasticsearch, PostgreSQL, Prometheus, Grafana

### À afficher

- idéalement ton schéma d’architecture globale

### À dire

- architecture microservices ;
- séparation des responsabilités ;
- intégration avec les outils existants, sans les remplacer.

## Slide 6 — Pipeline de traitement

### Titre conseillé

**Du log à l’incident**

### Flux à afficher

- ingestion du log ;
- parsing ;
- détection d’anomalie ;
- création d’un `AnomalyReport` ;
- corrélation dans `incident-service` ;
- notification et ticketing.

### Message important à dire

> La valeur ajoutée ne se limite pas à détecter, mais à transformer automatiquement un événement technique en incident exploitable.

## Slide 7 — Déduplication et logique métier

### Titre conseillé

**Gestion des incidents et déduplication**

### Contenu

- fingerprint = `type::source` ;
- recherche d’incident actif ;
- mise à jour si récurrence ;
- création sinon ;
- cycle de vie `OPEN -> ACKNOWLEDGED -> RESOLVED`.

### À dire

- insister que la déduplication se fait dans Elasticsearch, pas dans Jira ;
- c’est un point fort important pour le jury.

## Slide 8 — Interfaces utilisateur

### Contenu

- dashboard React ;
- page incidents ;
- page logs ;
- terminal ChatOps ;
- audit.

### Si tu as peu de temps

Montre seulement :

- une capture dashboard ;
- une capture incidents ;
- une capture ChatOps si possible.

### Message à dire

- la plateforme propose à la fois une interface graphique et une interface conversationnelle.

## Slide 9 — Démonstration fonctionnelle

### Contenu

Un scénario simple :

- injection d’un log d’erreur ;
- incident créé ;
- notification envoyée ;
- commande ChatOps `list` ou `stats`.

### À dire

- soit tu fais une vraie démo ;
- soit tu fais une démo statique par captures et schémas.

## Slide 10 — Stack technique

### Contenu

- Java 21 / Spring Boot ;
- Maven multi-module ;
- Elasticsearch ;
- PostgreSQL ;
- React / Vite ;
- Docker Compose ;
- Prometheus / Grafana.

### À dire

- justifier brièvement les choix ;
- montrer la cohérence avec l’environnement existant.

## Slide 11 — Tests et évaluation

### Contenu

- tests unitaires par module ;
- scripts d’évaluation ;
- métriques : précision, rappel, F1 ;
- éventuellement résultats de charge.

### Important

Ne mets des chiffres que si tu les as réellement obtenus.

### Sinon

Utilise une formulation prudente :

- campagne d’évaluation automatisée préparée ;
- protocole reproductible ;
- résultats exploitables via `run_evaluation.ps1`.

## Slide 12 — Résultats et apports

### Contenu

- réduction des tâches manuelles ;
- meilleure réactivité ;
- centralisation du traitement d’incident ;
- meilleure traçabilité ;
- architecture prête à évoluer.

### Formulation utile

> Le projet apporte une chaîne d’automatisation complète entre la détection et la prise en charge d’un incident.

## Slide 13 — Limites et perspectives

### Contenu

- sécurisation/authentification des APIs ;
- rate limiting ou message broker dédié ;
- circuit breaker ;
- déploiement Kubernetes ;
- enrichissement des dashboards ;
- industrialisation de l’évaluation.

### À dire

- montrer que tu connais les limites réelles ;
- rester honnête et crédible.

## Slide 14 — Conclusion

### Contenu

- rappel de la problématique ;
- synthèse de la solution ;
- bénéfices pour l’entreprise ;
- ouverture.

### Phrase de fin possible

> Ce travail m’a permis de concevoir une plateforme d’observabilité orientée automatisation des incidents, en combinant intégration métier, architecture microservices et outils modernes de supervision.

## Slide 15 — Questions

Simple et propre.

## 3. Version courte si le temps est serré

Si tu dois faire une soutenance plus courte, garde seulement :

- Slide 1 — Titre
- Slide 2 — Contexte et problème
- Slide 3 — Objectifs
- Slide 4 — Architecture globale
- Slide 5 — Pipeline de traitement
- Slide 6 — Déduplication / incidents
- Slide 7 — Interfaces
- Slide 8 — Stack technique
- Slide 9 — Résultats / apports
- Slide 10 — Limites / perspectives / conclusion

## 4. Visuels à préparer en priorité

Si tu n’as pas encore toutes les captures, prépare d’abord :

- **capture du dashboard**
- **capture de la page incidents**
- **schéma d’architecture globale**
- **schéma pipeline d’un log vers un incident**
- **diagramme d’états incident**

## 5. Conseils de forme

- une idée principale par slide ;
- peu de texte ;
- police grande ;
- préférer schémas, flux, captures et mots-clés ;
- éviter les paragraphes longs ;
- garder une cohérence visuelle avec ton slide de licence si tu réutilises son template.

## 6. Storyline conseillée

L’ordre narratif le plus convaincant est :

- **voici le problème métier** ;
- **voici pourquoi l’existant n’est pas suffisant** ;
- **voici l’architecture que j’ai conçue** ;
- **voici comment un incident est traité automatiquement** ;
- **voici ce que l’utilisateur voit** ;
- **voici les apports et les limites**.

## 7. Ce qu’il faut absolument éviter devant le jury

- dire que Jira fait la déduplication ;
- promettre des fonctionnalités non démontrées ;
- surcharger les slides de texte ;
- passer trop de temps sur des détails de code ;
- parler d’outils non présents dans l’application comme s’ils étaient intégrés.

## 8. Script oral très court par grande partie

### Contexte

> L’entreprise dispose déjà d’outils de détection, mais le traitement des incidents reste largement manuel.

### Problème

> Le temps est perdu après la détection : ticketing, notification, consultation des informations et gestion des doublons.

### Solution

> J’ai développé une plateforme microservices qui automatise cette chaîne, depuis l’ingestion d’un log jusqu’à la création ou la mise à jour d’un incident.

### Valeur ajoutée

> L’intérêt principal est de transformer une détection technique en action opérationnelle exploitable.

### Conclusion

> Le projet améliore la réactivité, la traçabilité et la centralisation du traitement des incidents tout en restant cohérent avec l’écosystème technique existant.
