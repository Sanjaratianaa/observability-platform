#!/usr/bin/env bash
set -euo pipefail

# Script d'évaluation minimal (bash)
# Usage: bash scripts/run_evaluation.sh

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "1) Démarrage de la stack via infra/docker-compose.yml"
cd infra
docker compose up --build -d
sleep 8

echo "2) Vérifications rapides"
curl -sS http://localhost:8082/actuator/health || echo "Backend non accessible — vérifier logs"
curl -sS http://localhost:8082/actuator/prometheus | head -n 10 || echo "Prometheus non accessible"

echo "3) Injection de logs (exemple). Adapter l'URL d'ingestion si nécessaire"
# Exemple (décommenter et adapter si backend expose /ingest)
# curl -X POST "http://localhost:8082/ingest" -H 'Content-Type: text/plain' --data-binary @data/sample/apache.log

echo "4) Collecte métriques basiques"
mkdir -p results
DATE_TAG=$(date +%Y%m%d-%H%M%S)
# Récupérer /actuator/prometheus
curl -sS http://localhost:8082/actuator/prometheus > results/prometheus-$DATE_TAG.txt || true

echo "5) Stop optionnel (à commenter pour laisser tourner)"
# docker compose down

echo "Evaluation terminée — résultats dans results/"
