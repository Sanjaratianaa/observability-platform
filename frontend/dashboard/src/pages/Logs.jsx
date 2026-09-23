import { useEffect, useState } from 'react';
import { fetchLogs, searchLogs } from '../api';
import { Search, ChevronLeft, ChevronRight, X } from 'lucide-react';

const PANEL = { background: '#16181e', border: '1px solid #1e2028' };
const INPUT = { background: '#1e2028', border: '1px solid #2a2d35', color: '#c9cdd5' };
const LEVEL_COLOR = { ERROR: '#ef4444', WARN: '#f59e0b', INFO: '#3b82f6', DEBUG: '#6b7080' };

export default function Logs() {
  const [logs, setLogs] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const [searchMode, setSearchMode] = useState(false);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [level, setLevel] = useState('');

  const loadPage = (p) => {
    setLoading(true);
    fetchLogs(p, 20).then((data) => {
      setLogs(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
      setPage(p);
      setLoading(false);
    }).catch(() => setLoading(false));
  };

  const doSearch = () => {
    if (!from || !to) return;
    setLoading(true);
    setSearchMode(true);
    searchLogs(from, to, level || null).then((data) => {
      setLogs(Array.isArray(data) ? data : []);
      setTotalPages(1);
      setTotalElements(Array.isArray(data) ? data.length : 0);
      setLoading(false);
    }).catch(() => setLoading(false));
  };

  const resetSearch = () => {
    setSearchMode(false);
    setFrom('');
    setTo('');
    setLevel('');
    loadPage(0);
  };

  useEffect(() => { loadPage(0); }, []);

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-end justify-between">
        <div>
          <h2 className="text-lg font-semibold text-white">Logs</h2>
          <p className="text-xs mt-0.5" style={{ color: '#5a5f6b' }}>Flux de logs ingérés</p>
        </div>
        <span className="text-xs font-mono" style={{ color: '#5a5f6b' }}>{totalElements} entrées</span>
      </div>

      {/* Search bar */}
      <div className="rounded-md p-3 flex flex-wrap items-end gap-3" style={PANEL}>
        <Field label="De" type="datetime-local" value={from} onChange={setFrom} />
        <Field label="À" type="datetime-local" value={to} onChange={setTo} />
        <Field label="Niveau" type="select" value={level} onChange={setLevel}
          options={['', 'ERROR', 'WARN', 'INFO', 'DEBUG']} />
        <button onClick={doSearch}
          className="px-3 py-1.5 rounded text-xs font-medium flex items-center gap-1.5 transition-colors text-white"
          style={{ background: '#2563eb' }}
          onMouseOver={e => e.currentTarget.style.background = '#1d4ed8'}
          onMouseOut={e => e.currentTarget.style.background = '#2563eb'}>
          <Search size={12} /> Rechercher
        </button>
        {searchMode && (
          <button onClick={resetSearch}
            className="px-3 py-1.5 rounded text-xs flex items-center gap-1 transition-colors"
            style={{ color: '#6b7080' }}
            onMouseOver={e => e.currentTarget.style.color = '#fff'}
            onMouseOut={e => e.currentTarget.style.color = '#6b7080'}>
            <X size={12} /> Réinitialiser
          </button>
        )}
      </div>

      {/* Table */}
      <div className="rounded-md overflow-hidden" style={PANEL}>
        {loading ? (
          <p className="text-xs p-6 text-center" style={{ color: '#5a5f6b' }}>Chargement...</p>
        ) : logs.length === 0 ? (
          <p className="text-xs p-6 text-center" style={{ color: '#3a3d45' }}>Aucun log trouvé</p>
        ) : (
          <table className="w-full text-xs">
            <thead>
              <tr style={{ background: '#13151a' }}>
                <th className="text-left px-4 py-2.5 font-medium" style={{ color: '#5a5f6b' }}>Timestamp</th>
                <th className="text-left px-4 py-2.5 font-medium" style={{ color: '#5a5f6b' }}>Niveau</th>
                <th className="text-left px-4 py-2.5 font-medium" style={{ color: '#5a5f6b' }}>Source</th>
                <th className="text-left px-4 py-2.5 font-medium" style={{ color: '#5a5f6b' }}>Message</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log, idx) => (
                <tr key={log.id} className="hover:bg-white/[0.02] transition-colors"
                  style={{ borderTop: idx > 0 ? '1px solid #1e2028' : 'none' }}>
                  <td className="px-4 py-2 whitespace-nowrap font-mono" style={{ color: '#5a5f6b' }}>
                    {log.timestamp ? new Date(log.timestamp).toLocaleString('fr-FR') : '-'}
                  </td>
                  <td className="px-4 py-2">
                    <span className="inline-flex items-center gap-1.5 text-[11px] font-medium"
                      style={{ color: LEVEL_COLOR[log.level] || '#6b7080' }}>
                      <span className="w-1.5 h-1.5 rounded-full" style={{ background: LEVEL_COLOR[log.level] || '#6b7080' }} />
                      {log.level}
                    </span>
                  </td>
                  <td className="px-4 py-2 font-mono" style={{ color: '#6b7080' }}>{log.source}</td>
                  <td className="px-4 py-2 max-w-md truncate" style={{ color: '#c9cdd5' }}>{log.message}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Pagination */}
      {!searchMode && totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <button onClick={() => loadPage(page - 1)} disabled={page === 0}
            className="p-1.5 rounded transition-colors disabled:opacity-30"
            style={{ ...PANEL, color: '#6b7080' }}>
            <ChevronLeft size={14} />
          </button>
          <span className="text-xs font-mono" style={{ color: '#5a5f6b' }}>{page + 1} / {totalPages}</span>
          <button onClick={() => loadPage(page + 1)} disabled={page >= totalPages - 1}
            className="p-1.5 rounded transition-colors disabled:opacity-30"
            style={{ ...PANEL, color: '#6b7080' }}>
            <ChevronRight size={14} />
          </button>
        </div>
      )}
    </div>
  );
}

function Field({ label, type, value, onChange, options }) {
  return (
    <div className="flex flex-col gap-1">
      <label className="text-[10px] uppercase tracking-wider" style={{ color: '#5a5f6b' }}>{label}</label>
      {type === 'select' ? (
        <select value={value} onChange={(e) => onChange(e.target.value)}
          className="rounded px-2.5 py-1.5 text-xs focus:outline-none"
          style={{ ...INPUT, borderColor: '#2a2d35' }}>
          {options.map((o) => <option key={o} value={o}>{o || 'Tous'}</option>)}
        </select>
      ) : (
        <input type={type} value={value} onChange={(e) => onChange(e.target.value)}
          className="rounded px-2.5 py-1.5 text-xs focus:outline-none"
          style={{ ...INPUT, borderColor: '#2a2d35' }} />
      )}
    </div>
  );
}
