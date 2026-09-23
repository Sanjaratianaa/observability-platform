import { useEffect, useState } from 'react';
import { fetchIncidentStats, fetchIncidents, fetchLogs } from '../api';
import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, Tooltip, Cell } from 'recharts';

const PANEL = { background: '#16181e', border: '1px solid #1e2028' };
const SEV_DOT = { CRITICAL: '#ef4444', HIGH: '#f97316', MEDIUM: '#eab308', LOW: '#3b82f6' };
const STATUS_COLOR = { OPEN: '#ef4444', ACKNOWLEDGED: '#f59e0b', RESOLVED: '#22c55e' };

export default function Dashboard() {
  const [stats, setStats] = useState({});
  const [incidents, setIncidents] = useState([]);
  const [logCount, setLogCount] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      fetchIncidentStats(),
      fetchIncidents(),
      fetchLogs(0, 1),
    ]).then(([s, inc, logs]) => {
      setStats(s);
      setIncidents(inc);
      setLogCount(logs.totalElements || 0);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  if (loading) return <p style={{ color: '#5a5f6b' }} className="text-sm p-4">Chargement...</p>;

  const total = Object.values(stats).reduce((a, b) => a + b, 0);
  const open = stats.OPEN || 0;
  const ack = stats.ACKNOWLEDGED || 0;
  const resolved = stats.RESOLVED || 0;

  const barData = [
    { name: 'Ouverts', value: open, color: '#ef4444' },
    { name: 'Acquittés', value: ack, color: '#f59e0b' },
    { name: 'Résolus', value: resolved, color: '#22c55e' },
  ];

  const sevCounts = {};
  incidents.forEach(i => { sevCounts[i.severity] = (sevCounts[i.severity] || 0) + 1; });

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-end justify-between">
        <div>
          <h2 className="text-lg font-semibold text-white">Dashboard</h2>
          <p className="text-xs mt-0.5" style={{ color: '#5a5f6b' }}>Vue d'ensemble du système de monitoring</p>
        </div>
        <div className="text-xs font-mono" style={{ color: '#5a5f6b' }}>
          {new Date().toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })}
        </div>
      </div>

      {/* KPIs — compact row */}
      <div className="grid grid-cols-5 gap-3">
        <Metric label="Logs ingérés" value={logCount} accent="#3b82f6" />
        <Metric label="Incidents" value={total} accent="#8b5cf6" />
        <Metric label="Ouverts" value={open} accent="#ef4444" />
        <Metric label="Acquittés" value={ack} accent="#f59e0b" />
        <Metric label="Résolus" value={resolved} accent="#22c55e" />
      </div>

      <div className="grid grid-cols-3 gap-3">
        {/* Bar chart */}
        <div className="col-span-1 rounded-md p-4" style={PANEL}>
          <h3 className="text-xs font-medium mb-3" style={{ color: '#6b7080' }}>Incidents par statut</h3>
          {total > 0 ? (
            <ResponsiveContainer width="100%" height={180}>
              <BarChart data={barData} barSize={28}>
                <XAxis dataKey="name" tick={{ fontSize: 11, fill: '#5a5f6b' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 11, fill: '#5a5f6b' }} axisLine={false} tickLine={false} allowDecimals={false} />
                <Tooltip
                  contentStyle={{ background: '#1e2028', border: '1px solid #2a2d35', borderRadius: 4, fontSize: 12 }}
                  cursor={{ fill: 'rgba(255,255,255,0.03)' }}
                />
                <Bar dataKey="value" radius={[3, 3, 0, 0]}>
                  {barData.map((entry, i) => <Cell key={i} fill={entry.color} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <p className="text-xs py-10 text-center" style={{ color: '#3a3d45' }}>Aucune donnée</p>
          )}
        </div>

        {/* Severity breakdown */}
        <div className="col-span-1 rounded-md p-4" style={PANEL}>
          <h3 className="text-xs font-medium mb-3" style={{ color: '#6b7080' }}>Répartition par sévérité</h3>
          <div className="space-y-3">
            {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map(sev => {
              const count = sevCounts[sev] || 0;
              const pct = total > 0 ? Math.round((count / total) * 100) : 0;
              return (
                <div key={sev}>
                  <div className="flex items-center justify-between mb-1">
                    <div className="flex items-center gap-2">
                      <span className="w-2 h-2 rounded-full" style={{ background: SEV_DOT[sev] }} />
                      <span className="text-xs text-white">{sev}</span>
                    </div>
                    <span className="text-xs font-mono" style={{ color: '#5a5f6b' }}>{count} ({pct}%)</span>
                  </div>
                  <div className="h-1.5 rounded-full" style={{ background: '#1e2028' }}>
                    <div className="h-full rounded-full transition-all" style={{ width: `${pct}%`, background: SEV_DOT[sev], opacity: 0.8 }} />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Recent activity */}
        <div className="col-span-1 rounded-md p-4" style={PANEL}>
          <h3 className="text-xs font-medium mb-3" style={{ color: '#6b7080' }}>Derniers incidents</h3>
          {incidents.length === 0 ? (
            <p className="text-xs" style={{ color: '#3a3d45' }}>Aucun incident</p>
          ) : (
            <div className="space-y-0">
              {incidents.slice(0, 6).map((inc, idx) => (
                <div key={inc.id}
                  className="flex items-center gap-2.5 py-2"
                  style={{ borderTop: idx > 0 ? '1px solid #1e2028' : 'none' }}>
                  <span className="w-1.5 h-1.5 rounded-full flex-shrink-0" style={{ background: SEV_DOT[inc.severity] }} />
                  <div className="flex-1 min-w-0">
                    <div className="text-xs text-white truncate">{inc.type.replace(/_/g, ' ')}</div>
                    <div className="text-[10px] font-mono truncate" style={{ color: '#5a5f6b' }}>{inc.source}</div>
                  </div>
                  <StatusPill status={inc.status} />
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Incident table — denser view */}
      {incidents.length > 0 && (
        <div className="rounded-md overflow-hidden" style={PANEL}>
          <div className="px-4 py-3 border-b" style={{ borderColor: '#1e2028' }}>
            <h3 className="text-xs font-medium" style={{ color: '#6b7080' }}>Tous les incidents actifs</h3>
          </div>
          <table className="w-full text-xs">
            <thead>
              <tr style={{ background: '#13151a' }}>
                <th className="text-left px-4 py-2 font-medium" style={{ color: '#5a5f6b' }}>Sévérité</th>
                <th className="text-left px-4 py-2 font-medium" style={{ color: '#5a5f6b' }}>Type</th>
                <th className="text-left px-4 py-2 font-medium" style={{ color: '#5a5f6b' }}>Source</th>
                <th className="text-left px-4 py-2 font-medium" style={{ color: '#5a5f6b' }}>Statut</th>
                <th className="text-right px-4 py-2 font-medium" style={{ color: '#5a5f6b' }}>Occurrences</th>
              </tr>
            </thead>
            <tbody>
              {incidents.map((inc, idx) => (
                <tr key={inc.id} className="hover:bg-white/[0.02] transition-colors"
                  style={{ borderTop: idx > 0 ? '1px solid #1e2028' : 'none' }}>
                  <td className="px-4 py-2.5">
                    <div className="flex items-center gap-2">
                      <span className="w-2 h-2 rounded-full" style={{ background: SEV_DOT[inc.severity] }} />
                      <span className="text-white">{inc.severity}</span>
                    </div>
                  </td>
                  <td className="px-4 py-2.5 text-white font-mono">{inc.type}</td>
                  <td className="px-4 py-2.5 font-mono" style={{ color: '#6b7080' }}>{inc.source}</td>
                  <td className="px-4 py-2.5"><StatusPill status={inc.status} /></td>
                  <td className="px-4 py-2.5 text-right font-mono text-white">{inc.occurrenceCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function Metric({ label, value, accent }) {
  return (
    <div className="rounded-md p-3" style={PANEL}>
      <div className="text-[10px] uppercase tracking-wider mb-1" style={{ color: '#5a5f6b' }}>{label}</div>
      <div className="text-xl font-semibold font-mono" style={{ color: accent }}>{value}</div>
    </div>
  );
}

function StatusPill({ status }) {
  return (
    <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium"
      style={{ background: STATUS_COLOR[status] + '18', color: STATUS_COLOR[status] }}>
      <span className="w-1 h-1 rounded-full" style={{ background: STATUS_COLOR[status] }} />
      {status}
    </span>
  );
}
