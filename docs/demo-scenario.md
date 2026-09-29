# Scénario de Démo Vidéo + Captures d'écran

## Pré-requis (à faire AVANT l'enregistrement)

1. **Lancer Docker** : `docker compose up -d` dans `infra/` → attendre ~5-7 min que tout soit UP
2. **Vérifier** : `docker ps` → 8 containers UP
3. **Ouvrir le navigateur** avec ces onglets prêts :
   - `http://localhost:8090` (frontend)
   - `http://localhost:9091` (Prometheus)
   - `http://localhost:3000` (Grafana, admin/admin)
   - `http://localhost:8085/swagger-ui.html` (Swagger monitoring)
   - `http://localhost:8082/swagger-ui.html` (Swagger incident)

---

## Scénario Démo (~5 min de vidéo)

### Séquence 1 — Infrastructure (30s)
📸 **Captures à prendre :**
- Terminal avec `docker ps` montrant les 8 containers UP
- Grafana dashboard (même vide, ça montre que c'est branché)

### Séquence 2 — Dashboard vide → avec données (1 min)
1. Ouvrir `http://localhost:8090` → **📸 Dashboard page**
2. Ouvrir un terminal PowerShell et injecter des logs :
```powershell
# Log normal
Invoke-RestMethod -Uri "http://localhost:8085/api/logs/raw?source=demo" -Method Post -Body '{"timestamp":"2026-09-29T10:00:00Z","level":"INFO","service":"demo","message":"Application started successfully"}' -ContentType "text/plain; charset=utf-8"

# Log avec erreur (anomalie keyword)
Invoke-RestMethod -Uri "http://localhost:8085/api/logs/raw?source=demo" -Method Post -Body '{"timestamp":"2026-09-29T10:01:00Z","level":"ERROR","service":"demo","message":"connection timeout to database server"}' -ContentType "text/plain; charset=utf-8"

# Log avec stack trace (anomalie stack trace)
Invoke-RestMethod -Uri "http://localhost:8085/api/logs/raw?source=demo" -Method Post -Body '{"timestamp":"2026-09-29T10:02:00Z","level":"ERROR","service":"demo","message":"java.lang.NullPointerException at com.example.Service.process(Service.java:42)"}' -ContentType "text/plain; charset=utf-8"
```
3. Rafraîchir le Dashboard → les KPI changent → **📸 Dashboard avec données**

### Séquence 3 — Pages Logs et Incidents (1 min)
1. Cliquer sur "Logs" → **📸 Page Logs** avec les 3 logs visibles
2. Cliquer sur "Incidents" → **📸 Page Incidents** avec les incidents créés automatiquement
3. Montrer les badges de sévérité (MEDIUM/HIGH) et les boutons Acquitter/Résoudre

### Séquence 4 — Cycle de vie d'un incident (1 min)
1. Sur la page Incidents, cliquer **Acquitter** sur un incident
2. **📸 Incident acquitté** (badge ACKNOWLEDGED)
3. Cliquer **Résoudre** sur le même incident
4. **📸 Incident résolu** (badge RESOLVED)

### Séquence 5 — ChatOps (30s)
1. Cliquer sur "ChatOps" dans le menu
2. Taper `help` → voir les commandes disponibles → **📸 ChatOps help**
3. Taper `list` → voir les incidents
4. Taper `stats` → voir les statistiques

### Séquence 6 — Audit (30s)
1. Cliquer sur "Audit" → **📸 Page Audit** montrant les actions (CREATED, ACKNOWLEDGED, RESOLVED)
2. Voir l'onglet Notifications → **📸 Historique des notifications**

### Séquence 7 — Swagger API (30s)
1. Montrer `http://localhost:8085/swagger-ui.html` → **📸 Swagger monitoring**
2. Montrer `http://localhost:8082/swagger-ui.html` → **📸 Swagger incident**

### Séquence 8 — Métriques (30s)
1. Montrer `http://localhost:9091` → Prometheus → **📸 Prometheus UI**
2. Montrer `http://localhost:3000` → Grafana dashboard → **📸 Grafana**

---

## Résumé des captures nécessaires (14 captures)

| # | Capture | URL/Source |
|---|---------|-----------|
| 1 | `docker ps` — 8 containers UP | Terminal |
| 2 | Dashboard (avec KPI) | `localhost:8090` |
| 3 | Page Logs | `localhost:8090` → Logs |
| 4 | Page Incidents (avec badges) | `localhost:8090` → Incidents |
| 5 | Incident acquitté | `localhost:8090` → Incidents |
| 6 | Incident résolu | `localhost:8090` → Incidents |
| 7 | ChatOps terminal | `localhost:8090` → ChatOps |
| 8 | Page Audit (actions) | `localhost:8090` → Audit |
| 9 | Audit — Notifications | `localhost:8090` → Audit |
| 10 | Swagger monitoring-service | `localhost:8085/swagger-ui.html` |
| 11 | Swagger incident-service | `localhost:8082/swagger-ui.html` |
| 12 | Prometheus UI | `localhost:9091` |
| 13 | Grafana dashboard | `localhost:3000` |
| 14 | Terminal — injection de logs | Terminal PowerShell |

---

## Astuce pour gagner du temps

**NE PAS éteindre Docker avant la capture.**
Si tu dois redémarrer le laptop, lance Docker dès le boot, puis `docker compose up -d` dans `infra/`. ES met ~5 min à devenir healthy, les services Spring Boot ~3-5 min après.

**Ordre optimal :**
1. Lance Docker en arrière-plan
2. Pendant que ça démarre, prépare les commandes dans un fichier `.ps1`
3. Dès que c'est UP, fais toutes les captures en une seule session (~10 min max)
