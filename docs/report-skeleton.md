Titre provisoire

Observability Platform — Détection d'anomalies sur flux de logs

Résumé

Brève synthèse des objectifs, méthodes, contributions et principaux résultats attendus (à remplir après expérimentation).

1. Contexte et problématique

- Présentation du besoin d'observabilité et détection d'anomalies dans les systèmes distribués.
- Limitations des approches existantes (seuils simples, règles statiques) et place d'approches hybrides.

2. Objectifs

- Développer et évaluer une plateforme de détection d'anomalies sur logs (multi-format).
- Proposer une méthodologie reproductible pour évaluer précision, latence et coût.
- Comparer détecteurs heuristiques (keyword, sliding-window) et méthodes baselines.

3. Contributions attendues

- Implémentation modulaire de parsers et détecteurs (réutilisables).
- Jeux de données synthétiques/annotés et scripts d'évaluation reproductibles.
- Analyse expérimentale: précision/recall/F1, latence, consommation ressource.

4. Architecture et composants

- Description du backend (Spring Boot), indexation (Elasticsearch), stockage métadonnées (Postgres), frontend (React) et monitoring (Prometheus).
- Flux: ingestion -> parsing -> détection -> notification/chatops -> stockage/visualisation.

5. Méthodologie expérimentale

- Jeux de données: formats (apache/json/syslog), scénarios (normal, pic d'erreurs, faux positifs).
- Protocoles: injection contrôlée, répétitions (n=3), collecte métriques via Prometheus et logs d'alerte.
- Baselines: seuils simples, sliding-window, keyword matching.

6. Métriques d'évaluation

- Détection: précision, rappel, F1 (au niveau d'événement), AUC si applicable.
- Opérationnelles: latence de détection, throughput, CPU/RAM.
- Robustesse: taux faux positifs/negatifs et variance entre runs.

7. Expériences planifiées

- Expérience 1: Sensibilité aux pics d'erreurs (varier amplitude et durée).
- Expérience 2: Robustesse face à bruit et faux-positifs (ajout de logs bruyants).
- Expérience 3: Scalabilité (taille de dataset, taux d'ingestion).

8. Analyse et visualisations

- Tableaux comparatifs, courbes PR, latence cumulative, utilisation ressource.
- Discussion sur compromis précision/latence et limites méthodologiques.

9. Plan de travail et calendrier (exemple 8–12 semaines)

- Semaines 1–2: jeux de données et scripts d'évaluation.
- Semaines 3–5: exécution expériences initiales et itérations.
- Semaines 6–8: analyses approfondies, rédaction des résultats.
- Semaines 9–12: rédaction finale, slides et préparation soutenance.

10. Livrables

- Code et scripts reproductibles (repo), jeux de données et rapports (PDF/Markdown), notebook d'analyse et présentation.

11. Annexes

- Instructions de reproduction (CD/CI), listing des endpoints, format d'annotations.

Références

- (Ajouter articles et sources bibliographiques pertinentes)
