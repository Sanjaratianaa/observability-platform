import { useEffect, useState } from 'react';
import { fetchIncidents, acknowledgeIncident, resolveIncident } from '../api';
import { CheckCircle, Eye, RefreshCw } from 'lucide-react';

const SEVERITY_COLORS = {
  CRITICAL: 'bg-red-500/20 text-red-400 border-red-500/30',
  HIGH: 'bg-orange-500/20 text-orange-400 border-orange-500/30',
  MEDIUM: 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30',
  LOW: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
};

const STATUS_COLORS = {
  OPEN: 'bg-red-500/20 text-red-400',
  ACKNOWLEDGED: 'bg-amber-500/20 text-amber-400',
  RESOLVED: 'bg-emerald-500/20 text-emerald-400',
};

export default function Incidents() {
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState('');
  const [filterSeverity, setFilterSeverity] = useState('');

  const load = () => {
    setLoading(true);
    fetchIncidents(filterStatus || null, filterSeverity || null)
      .then((data) => {
        setIncidents(Array.isArray(data) ? data : []);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  };

  useEffect(() => { load(); }, [filterStatus, filterSeverity]);

  const handleAck = async (id) => {
    await acknowledgeIncident(id);
    load();
  };

  const handleResolve = async (id) => {
    await resolveIncident(id);
    load();
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold text-white">Incidents</h2>
        <button onClick={load}
          className="text-slate-400 hover:text-white p-2 rounded-lg hover:bg-slate-700 transition-colors">
          <RefreshCw size={16} />
        </button>
      </div>

      {/* Filters */}
      <div className="flex gap-3">
        <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}
          className="bg-slate-800 border border-slate-700 text-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-emerald-500">
          <option value="">Tous les statuts</option>
          <option value="OPEN">Ouverts</option>
          <option value="ACKNOWLEDGED">Acquittés</option>
          <option value="RESOLVED">Résolus</option>
        </select>
        <select value={filterSeverity} onChange={(e) => setFilterSeverity(e.target.value)}
          className="bg-slate-800 border border-slate-700 text-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-emerald-500">
          <option value="">Toutes sévérités</option>
          <option value="CRITICAL">Critical</option>
          <option value="HIGH">High</option>
          <option value="MEDIUM">Medium</option>
          <option value="LOW">Low</option>
        </select>
      </div>

      {/* Cards */}
      {loading ? (
        <p className="text-slate-400 text-sm">Chargement...</p>
      ) : incidents.length === 0 ? (
        <p className="text-slate-500 text-sm">Aucun incident</p>
      ) : (
        <div className="space-y-3">
          {incidents.map((inc) => (
            <div key={inc.id} className={`bg-slate-800 border rounded-xl p-4 ${SEVERITY_COLORS[inc.severity]?.split(' ')[2] || 'border-slate-700'}`}>
              <div className="flex items-start justify-between">
                <div className="space-y-1 flex-1">
                  <div className="flex items-center gap-2">
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${SEVERITY_COLORS[inc.severity] || ''}`}>
                      {inc.severity}
                    </span>
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${STATUS_COLORS[inc.status] || ''}`}>
                      {inc.status}
                    </span>
                    <span className="text-xs text-slate-500">#{inc.id?.slice(0, 8)}</span>
                  </div>
                  <h3 className="text-white font-medium">{inc.type}</h3>
                  <p className="text-sm text-slate-400">{inc.description}</p>
                  <div className="flex gap-4 text-xs text-slate-500 mt-1">
                    <span>Source: <span className="text-slate-300 font-mono">{inc.source}</span></span>
                    <span>Occurrences: <span className="text-slate-300">{inc.occurrenceCount}</span></span>
                    <span>Dernière vue: {inc.lastSeen ? new Date(inc.lastSeen).toLocaleString('fr-FR') : '-'}</span>
                    {inc.relatedLogIds?.length > 0 && (
                      <span>Logs liés: <span className="text-slate-300">{inc.relatedLogIds.length}</span></span>
                    )}
                  </div>
                </div>

                {/* Actions */}
                <div className="flex gap-2 ml-4">
                  {inc.status === 'OPEN' && (
                    <button onClick={() => handleAck(inc.id)}
                      className="flex items-center gap-1 px-3 py-1.5 bg-amber-600/20 text-amber-400 rounded-lg text-xs hover:bg-amber-600/30 transition-colors">
                      <Eye size={14} /> ACK
                    </button>
                  )}
                  {(inc.status === 'OPEN' || inc.status === 'ACKNOWLEDGED') && (
                    <button onClick={() => handleResolve(inc.id)}
                      className="flex items-center gap-1 px-3 py-1.5 bg-emerald-600/20 text-emerald-400 rounded-lg text-xs hover:bg-emerald-600/30 transition-colors">
                      <CheckCircle size={14} /> Résoudre
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
