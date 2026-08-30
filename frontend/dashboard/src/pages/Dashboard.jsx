import { useEffect, useState } from 'react';
import { fetchIncidentStats, fetchIncidents, fetchLogs } from '../api';
import { AlertTriangle, FileText, ShieldCheck, ShieldAlert } from 'lucide-react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';

const COLORS = { OPEN: '#ef4444', ACKNOWLEDGED: '#f59e0b', RESOLVED: '#22c55e' };

export default function Dashboard() {
  const [stats, setStats] = useState({});
  const [recentIncidents, setRecentIncidents] = useState([]);
  const [logCount, setLogCount] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      fetchIncidentStats(),
      fetchIncidents(),
      fetchLogs(0, 1),
    ]).then(([s, inc, logs]) => {
      setStats(s);
      setRecentIncidents(inc.slice(0, 5));
      setLogCount(logs.totalElements || 0);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  if (loading) return <p className="text-slate-400">Chargement...</p>;

  const totalIncidents = Object.values(stats).reduce((a, b) => a + b, 0);
  const pieData = Object.entries(stats)
    .filter(([, v]) => v > 0)
    .map(([name, value]) => ({ name, value }));

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-white">Dashboard</h2>

      {/* KPI Cards */}
      <div className="grid grid-cols-4 gap-4">
        <Card icon={<FileText />} label="Total Logs" value={logCount} color="text-blue-400" />
        <Card icon={<AlertTriangle />} label="Incidents" value={totalIncidents} color="text-red-400" />
        <Card icon={<ShieldAlert />} label="Ouverts" value={stats.OPEN || 0} color="text-amber-400" />
        <Card icon={<ShieldCheck />} label="Résolus" value={stats.RESOLVED || 0} color="text-emerald-400" />
      </div>

      <div className="grid grid-cols-2 gap-6">
        {/* Pie chart */}
        <div className="bg-slate-800 rounded-xl p-4 border border-slate-700">
          <h3 className="text-sm font-medium text-slate-400 mb-2">Répartition incidents</h3>
          {pieData.length > 0 ? (
            <ResponsiveContainer width="100%" height={200}>
              <PieChart>
                <Pie data={pieData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label>
                  {pieData.map((entry) => (
                    <Cell key={entry.name} fill={COLORS[entry.name] || '#64748b'} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <p className="text-slate-500 text-sm text-center py-10">Aucun incident</p>
          )}
        </div>

        {/* Recent incidents */}
        <div className="bg-slate-800 rounded-xl p-4 border border-slate-700">
          <h3 className="text-sm font-medium text-slate-400 mb-3">Derniers incidents</h3>
          {recentIncidents.length === 0 ? (
            <p className="text-slate-500 text-sm">Aucun incident</p>
          ) : (
            <ul className="space-y-2">
              {recentIncidents.map((inc) => (
                <li key={inc.id} className="flex items-center justify-between text-sm bg-slate-700/50 px-3 py-2 rounded-lg">
                  <div className="flex items-center gap-2">
                    <SeverityBadge severity={inc.severity} />
                    <span className="text-slate-200 truncate max-w-[220px]">{inc.type}</span>
                  </div>
                  <StatusBadge status={inc.status} />
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}

function Card({ icon, label, value, color }) {
  return (
    <div className="bg-slate-800 border border-slate-700 rounded-xl p-4 flex items-center gap-4">
      <div className={`${color}`}>{icon}</div>
      <div>
        <p className="text-2xl font-bold text-white">{value}</p>
        <p className="text-xs text-slate-400">{label}</p>
      </div>
    </div>
  );
}

function SeverityBadge({ severity }) {
  const colors = {
    CRITICAL: 'bg-red-500/20 text-red-400',
    HIGH: 'bg-orange-500/20 text-orange-400',
    MEDIUM: 'bg-yellow-500/20 text-yellow-400',
    LOW: 'bg-blue-500/20 text-blue-400',
  };
  return <span className={`px-2 py-0.5 rounded text-xs font-medium ${colors[severity] || 'bg-slate-600 text-slate-300'}`}>{severity}</span>;
}

function StatusBadge({ status }) {
  const colors = {
    OPEN: 'bg-red-500/20 text-red-400',
    ACKNOWLEDGED: 'bg-amber-500/20 text-amber-400',
    RESOLVED: 'bg-emerald-500/20 text-emerald-400',
  };
  return <span className={`px-2 py-0.5 rounded text-xs font-medium ${colors[status] || 'bg-slate-600'}`}>{status}</span>;
}
