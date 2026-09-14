Dossier data/ — jeux de données pour évaluations

Structure recommandée:

- data/sample/apache.log     => logs Apache (text)
- data/sample/json.log       => JSON-lines (one JSON object per line)
- data/sample/syslog.log     => syslog format
- data/annotations/ground_truth.csv => annotation des incidents (columns: file,timestamp,start_line,end_line,label,comments)

Remarques:
- Fournir des versions petit/medium/large pour tester scalabilité.
- Ne committer pas de données sensibles; anonymiser ou synthétiser.
- Scripts d'injection: scripts/run_evaluation.sh et scripts/run_evaluation.ps1

Exemple simple d'annotation CSV:
# file,timestamp,start_line,end_line,label,comments
sample/apache.log,2026-09-14T12:00:00,120,130,ERROR_SPIKE,"Pic d'erreurs HTTP 5xx"

Ajouter ici les jeux de données avant d'exécuter les scripts d'évaluation.
