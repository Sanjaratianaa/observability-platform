Plan d'expériences — Observability Platform

1. Objectif

Évaluer la capacité des détecteurs d'anomalies du projet à repérer des incidents dans des flux de logs (apache, json, syslog). Mesurer précision, rappel, F1, latence de détection et coût (CPU/RAM).

2. Hypothèses

- Les détecteurs fournis (keyword, sliding window, stack trace, error-rate) donnent des comportements distincts selon scénarios.
- On peut simuler trois scénarios: normal, pic d'erreurs, faux-positifs.

3. Jeux de données

Structure recommandée (répertoire data/):
- data/sample/apache.log  (logs Apache synthétiques ou réels anonymisés)
- data/sample/json.log    (JSON lines)
- data/sample/syslog.log  (syslog format)
- data/annotations/ground_truth.csv  (format: file,timestamp,start_line,end_line,label,comments)

Fournir au moins 3 variantes par scénario (petit/medium/large).

4. Métriques

- Détection (par événement d'incident): précision, rappel, F1
- Score au niveau des fenêtres temporelles (si applicable): AUC
- Latence: temps entre ingestion et alerte
- Performances: CPU%, RAM MB, TPS ingested
- Robustesse: taux de faux positifs/negatifs

5. Baselines

- Seuil simple (ex: n erreurs / minute)
- Sliding-window counter (déjà présent)
- Keyword match

6. Protocole expérimental

Pour chaque dataset/scénario:
- Démarrer infra: docker compose up --build (infra/docker-compose.yml)
- Lancer backend en mode test si besoin (./mvnw spring-boot:run)
- Injecter logs via endpoint d'ingestion ou docker exec vers un utilitaire d'injection
- Mesurer metrics (temps, alerts) et collecter logs d'alerte
- Répéter 3 fois et calculer moyenne ± écart-type

7. Reproductibilité

- Fournir scripts/scripts/run_evaluation.sh (et .ps1) qui: démarrent la stack, injectent logs, collectent métriques et produisent un rapport CSV/JSON dans results/
- Versionner jeux de données et paramétrages

8. Livrables attendus

- data/ avec jeux de données et annotations
- scripts/run_evaluation.* exécutable
- results/ avec CSV/JSON de métriques et figures
- rapport/section expérimentale (figures, tableaux, discussion)

9. Estimation temporelle (fordable)

- Préparer jeux de données minimal: 1–2 jours
- Implémenter scripts d'évaluation: 1–2 jours
- Exécuter expériences initiales + analyser: 2–4 jours

10. Commandes d'exemple

# lancer stack (depuis infra/)
cd infra
docker compose up --build -d

# vérifier backend
curl -sS http://localhost:8082/actuator/health
curl -sS http://localhost:8082/actuator/prometheus | head -n 20

# injection (endpoint réel : POST /api/logs/raw/bulk?source=<nom>)
curl -X POST "http://localhost:8082/api/logs/raw/bulk?source=apache.log" -H 'Content-Type: text/plain' --data-binary @data/sample/apache.log

# évaluation complète (injection + métriques + comparaison ground truth)
.\scripts\run_evaluation.ps1 -Reset          # 1 run avec purge ES
.\scripts\run_evaluation.ps1 -Runs 3         # 3 répétitions, moyenne ± écart-type


---

Mettre à jour ce document si l'API d'ingestion diffère. Créer ensuite scripts d'exécution automatisés.
