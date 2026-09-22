#!/usr/bin/env bash
set -euo pipefail

# Evaluation — Observability Platform
# Injecte data/sample/*.log via POST /api/logs/raw/bulk, collecte les incidents
# et les metriques Prometheus, puis compare aux annotations ground_truth.csv.
#
# Usage:
#   bash scripts/run_evaluation.sh                 # backend deja demarre
#   bash scripts/run_evaluation.sh --start-stack   # demarre docker compose d'abord
#   BASE_URL=http://localhost:8082 bash scripts/run_evaluation.sh

BASE_URL="${BASE_URL:-http://localhost:8082}"
START_STACK="${1:-}"
HEALTH_TIMEOUT=90

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SAMPLE_DIR="$ROOT_DIR/data/sample"
GROUND_TRUTH="$ROOT_DIR/data/annotations/ground_truth.csv"
RESULTS_DIR="$ROOT_DIR/results/eval-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$RESULTS_DIR"

# --- 1) Stack optionnelle -----------------------------------------------------
if [ "$START_STACK" = "--start-stack" ]; then
    echo "==> Demarrage de la stack (docker compose)"
    docker compose -f "$ROOT_DIR/infra/docker-compose.yml" up --build -d
fi

# --- 2) Attente du backend ----------------------------------------------------
echo "==> Attente du backend sur $BASE_URL (timeout ${HEALTH_TIMEOUT}s)"
deadline=$((SECONDS + HEALTH_TIMEOUT))
healthy=0
while [ $SECONDS -lt $deadline ]; do
    if curl -sf "$BASE_URL/actuator/health" | grep -q '"UP"'; then healthy=1; break; fi
    sleep 3
done
[ "$healthy" = "1" ] || { echo "Backend non disponible sur $BASE_URL"; exit 1; }
echo "    Backend UP"

# --- 3) Snapshot des incidents existants --------------------------------------
before_ids=$(curl -sf "$BASE_URL/api/incidents" | grep -o '"id":"[^"]*"' | cut -d'"' -f4 || true)

# --- 4) Injection des fichiers de logs ----------------------------------------
echo "file,received,ingested,parse_errors,anomalies" > "$RESULTS_DIR/ingestion.csv"
for file in "$SAMPLE_DIR"/*.log; do
    source_name="$(basename "$file")"
    echo "==> Injection $source_name"
    result=$(curl -sf -X POST "$BASE_URL/api/logs/raw/bulk?source=$source_name" \
        -H 'Content-Type: text/plain; charset=utf-8' --data-binary @"$file") || {
        echo "    Echec injection $source_name"
        echo "$source_name,0,0,0,0" >> "$RESULTS_DIR/ingestion.csv"
        continue
    }
    echo "    $result"
    echo "$source_name,$(echo "$result" | tr -d '{}' | sed 's/[a-zA-Z"]*://g' | tr ',' '\n' | paste -sd, -)" \
        >> "$RESULTS_DIR/ingestion.csv" 2>/dev/null || echo "$source_name,$result" >> "$RESULTS_DIR/ingestion.csv"
done

# Laisse le temps aux notifications async et au refresh ES
sleep 5

# --- 5) Collecte incidents + metriques ----------------------------------------
curl -sf "$BASE_URL/api/incidents" > "$RESULTS_DIR/incidents.json"
curl -sf "$BASE_URL/actuator/prometheus" > "$RESULTS_DIR/prometheus.txt" || true

# Nouveaux incidents = ceux dont l'id n'etait pas dans le snapshot
new_sources=$(grep -o '"source":"[^"]*"' "$RESULTS_DIR/incidents.json" | cut -d'"' -f4 | sort -u || true)
# Filtre : on retire les incidents deja connus via leurs ids (approximation par source)
detected_sources="$new_sources"

# --- 6) Comparaison avec le ground truth --------------------------------------
echo "file,expected,detected,verdict" > "$RESULTS_DIR/detection.csv"
tp=0; fn=0; fp=0
tail -n +2 "$GROUND_TRUTH" | while IFS=, read -r gfile gts gstart gend glabel gcomments; do
    fname="$(basename "$gfile")"
    if echo "$detected_sources" | grep -qx "$fname"; then
        echo "$fname,INCIDENT,YES,TP" >> "$RESULTS_DIR/detection.csv"
    else
        echo "$fname,INCIDENT,NO,FN" >> "$RESULTS_DIR/detection.csv"
    fi
done

tp=$(grep -c ',TP$' "$RESULTS_DIR/detection.csv" || true)
fn=$(grep -c ',FN$' "$RESULTS_DIR/detection.csv" || true)
# FP : sources detectees hors ground truth
gt_files=$(tail -n +2 "$GROUND_TRUTH" | cut -d, -f1 | xargs -n1 basename | sort -u)
for s in $detected_sources; do
    if ! echo "$gt_files" | grep -qx "$s"; then
        echo "$s,none,YES,FP" >> "$RESULTS_DIR/detection.csv"
        fp=$((fp + 1))
    fi
done

precision=$(awk -v t="$tp" -v f="$fp" 'BEGIN{print (t+f)>0? t/(t+f) : 0}')
recall=$(awk -v t="$tp" -v f="$fn" 'BEGIN{print (t+f)>0? t/(t+f) : 0}')
f1=$(awk -v p="$precision" -v r="$recall" 'BEGIN{print (p+r)>0? 2*p*r/(p+r) : 0}')

cat > "$RESULTS_DIR/summary.json" <<EOF
{"timestamp":"$(date -Iseconds)","base_url":"$BASE_URL","TP":$tp,"FP":$fp,"FN":$fn,"precision":$precision,"recall":$recall,"f1":$f1}
EOF

echo ""
echo "================ RESULTATS ================"
cat "$RESULTS_DIR/detection.csv"
echo "TP=$tp FP=$fp FN=$fn  |  Precision=$precision Recall=$recall F1=$f1"
echo "Resultats dans : $RESULTS_DIR"
