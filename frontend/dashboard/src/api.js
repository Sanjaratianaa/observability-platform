const BASE = '/api';

export async function fetchLogs(page = 0, size = 50) {
  const res = await fetch(`${BASE}/logs?page=${page}&size=${size}`);
  return res.json();
}

export async function fetchLogsByLevel(level) {
  const res = await fetch(`${BASE}/logs/level/${level}`);
  return res.json();
}

export async function searchLogs(from, to, level) {
  let url = `${BASE}/logs/search?from=${from}&to=${to}`;
  if (level) url += `&level=${level}`;
  const res = await fetch(url);
  return res.json();
}

export async function fetchIncidents(status, severity) {
  let url = `${BASE}/incidents`;
  const params = new URLSearchParams();
  if (status) params.set('status', status);
  if (severity) params.set('severity', severity);
  const qs = params.toString();
  if (qs) url += `?${qs}`;
  const res = await fetch(url);
  return res.json();
}

export async function fetchIncidentStats() {
  const res = await fetch(`${BASE}/incidents/stats`);
  return res.json();
}

export async function acknowledgeIncident(id) {
  const res = await fetch(`${BASE}/incidents/${id}/ack`, { method: 'PUT' });
  return res.json();
}

export async function resolveIncident(id) {
  const res = await fetch(`${BASE}/incidents/${id}/resolve`, { method: 'PUT' });
  return res.json();
}
