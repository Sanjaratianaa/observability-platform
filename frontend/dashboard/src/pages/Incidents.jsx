import { useEffect, useState } from 'react';
import { fetchIncidents, acknowledgeIncident, resolveIncident } from '../api';
import { CheckCircle, Eye, RefreshCw } from 'lucide-react';

const PANEL = { background: '#16181e', border: '1px solid #1e2028' };
const INPUT = { background: '#1e2028', border: '1px solid #2a2d35', color: '#c9cdd5' };
const SEV_COLOR = { CRITICAL: '#ef4444', HIGH: '#f97316', MEDIUM: '#eab308', LOW: '#3b82f6' };
const STATUS_COLOR = { OPEN: '#ef4444', ACKNOWLEDGED: '#f59e0b', RESOLVED: '#22c55e' };

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
      {/* Header */}
      <div className="flex items-end justify-between">
        <div>
          <h2 className="text-lg font-semibold text-white">Incidents</h2>
          <p className="text-xs mt-0.5" style={{ color: '#5a5f6b' }}>Incidents détectés par la plateforme</p>
        </div>
        <button onClick={load}
          className="p-1.5 rounded transition-colors"
          style={{ color: '#6b7080' }}
          onMouseOver={e => e.currentTarget.style.color = '#fff'}
          onMouseOut={e => e.currentTarget.style.color = '#6b7080'}>
          <RefreshCw size={14} />
        </button>
      </div>

      {/* Filters */}
      <div className="flex gap-2">
        <select value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}
          className="rounded px-2.5 py-1.5 text-xs focus:outline-none" style={INPUT}>
          <option value="">Tous les statuts</option>
          <option value="OPEN">Ouverts</option>
          <option value="ACKNOWLEDGED">Acquittés</option>
          <option value="RESOLVED">Résolus</option>
        </select>
        <select value={filterSeverity} onChange={(e) => setFilterSeverity(e.target.value)}
          className="rounded px-2.5 py-1.5 text-xs focus:outline-none" style={INPUT}>
          <option value="">Toutes sévérités</option>
          <option value="CRITICAL">Critical</option>
          <option value="HIGH">High</option>
          <option value="MEDIUM">Medium</option>
          <option value="LOW">Low</option>
        </select>
      </div>

      {/* Incident list */}
      {loading ? (
        <p className="text-xs" style={{ color: '#5a5f6b' }}>Chargement...</p>
      ) : incidents.length === 0 ? (
        <p className="text-xs" style={{ color: '#3a3d45' }}>Aucun incident</p>
      ) : (
        <div className="rounded-md overflow-hidden" style={PANEL}>
          {incidents.map((inc, idx) => (
            <div key={inc.id}
              className="px-4 py-3 hover:bg-white/[0.02] transition-colors"
              style={{ borderTop: idx > 0 ? '1px solid #1e2028' : 'none' }}>
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                  {/* Title row */}
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="w-2 h-2 rounded-full flex-shrink-0" style={{ background: SEV_COLOR[inc.severity] }} />
                    <span className="text-sm font-medium text-white">{inc.type.replace(/_/g, ' ')}</span>
                    <StatusPill status={inc.status} />
                    <span className="text-[10px] font-mono" style={{ color: '#4a4f5a' }}>#{inc.id?.slice(0, 8)}</span>
                  </div>
                  {/* Description */}
                  <p className="text-xs mt-1 ml-4" style={{ color: '#6b7080' }}>{inc.description}</p>
                  {/* Meta */}
                  <div className="flex gap-4 mt-1.5 ml-4 text-[11px] font-mono" style={{ color: '#5a5f6b' }}>
                    <span>src: <span style={{ color: '#8b909c' }}>{inc.source}</span></span>
                    <span>occ: <span style={{ color: '#8b909c' }}>{inc.occurrenceCount}</span></span>
                    <span>dernière: <span style={{ color: '#8b909c' }}>{inc.lastSeen ? new Date(inc.lastSeen).toLocaleString('fr-FR') : '-'}</span></span>
                    {inc.relatedLogIds?.length > 0 && (
                      <span>logs: <span style={{ color: '#8b909c' }}>{inc.relatedLogIds.length}</span></span>
                    )}
                    {inc.jiraTicketKey && (
                      <span>jira: <span style={{ color: '#3b82f6' }}>{inc.jiraTicketKey}</span></span>
                    )}
                  </div>
                </div>

                {/* Actions */}
                <div className="flex gap-1.5 flex-shrink-0">
                  {inc.status === 'OPEN' && (
                    <button onClick={() => handleAck(inc.id)}
                      className="flex items-center gap-1 px-2.5 py-1 rounded text-[11px] font-medium transition-colors"
                      style={{ background: '#f59e0b15', color: '#f59e0b', border: '1px solid #f59e0b30' }}
                      onMouseOver={e => e.currentTarget.style.background = '#f59e0b25'}
                      onMouseOut={e => e.currentTarget.style.background = '#f59e0b15'}>
                      <Eye size={11} /> ACK
                    </button>
                  )}
                  {(inc.status === 'OPEN' || inc.status === 'ACKNOWLEDGED') && (
                    <button onClick={() => handleResolve(inc.id)}
                      className="flex items-center gap-1 px-2.5 py-1 rounded text-[11px] font-medium transition-colors"
                      style={{ background: '#22c55e15', color: '#22c55e', border: '1px solid #22c55e30' }}
                      onMouseOver={e => e.currentTarget.style.background = '#22c55e25'}
                      onMouseOut={e => e.currentTarget.style.background = '#22c55e15'}>
                      <CheckCircle size={11} /> Résoudre
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

function StatusPill({ status }) {
  return (
    <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium"
      style={{ background: STATUS_COLOR[status] + '18', color: STATUS_COLOR[status] }}>
      <span className="w-1 h-1 rounded-full" style={{ background: STATUS_COLOR[status] }} />
      {status}
    </span>
  );
}
