import { useEffect, useState } from 'react';
import { fetchLogs, searchLogs } from '../api';
import { Search, ChevronLeft, ChevronRight } from 'lucide-react';

const LEVEL_COLORS = {
  ERROR: 'bg-red-500/20 text-red-400',
  WARN: 'bg-amber-500/20 text-amber-400',
  INFO: 'bg-blue-500/20 text-blue-400',
  DEBUG: 'bg-slate-500/20 text-slate-400',
};

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
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-bold text-white">Logs</h2>
        <span className="text-sm text-slate-400">{totalElements} logs au total</span>
      </div>

      {/* Search bar */}
      <div className="bg-slate-800 border border-slate-700 rounded-xl p-4 flex flex-wrap items-end gap-3">
        <Field label="De" type="datetime-local" value={from} onChange={setFrom} />
        <Field label="À" type="datetime-local" value={to} onChange={setTo} />
        <Field label="Niveau" type="select" value={level} onChange={setLevel}
          options={['', 'ERROR', 'WARN', 'INFO', 'DEBUG']} />
        <button onClick={doSearch}
          className="bg-emerald-600 hover:bg-emerald-500 text-white px-4 py-2 rounded-lg text-sm flex items-center gap-1 transition-colors">
          <Search size={14} /> Rechercher
        </button>
        {searchMode && (
          <button onClick={resetSearch}
            className="text-slate-400 hover:text-white px-3 py-2 text-sm transition-colors">
            Réinitialiser
          </button>
        )}
      </div>

      {/* Table */}
      <div className="bg-slate-800 border border-slate-700 rounded-xl overflow-hidden">
        {loading ? (
          <p className="text-slate-400 text-sm p-6 text-center">Chargement...</p>
        ) : logs.length === 0 ? (
          <p className="text-slate-500 text-sm p-6 text-center">Aucun log trouvé</p>
        ) : (
          <table className="w-full text-sm text-left">
            <thead className="bg-slate-700/50 text-slate-400 text-xs uppercase">
              <tr>
                <th className="px-4 py-3">Timestamp</th>
                <th className="px-4 py-3">Niveau</th>
                <th className="px-4 py-3">Source</th>
                <th className="px-4 py-3">Message</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-700">
              {logs.map((log) => (
                <tr key={log.id} className="hover:bg-slate-700/30 transition-colors">
                  <td className="px-4 py-2 text-slate-400 whitespace-nowrap font-mono text-xs">
                    {log.timestamp ? new Date(log.timestamp).toLocaleString('fr-FR') : '-'}
                  </td>
                  <td className="px-4 py-2">
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${LEVEL_COLORS[log.level] || 'bg-slate-600 text-slate-300'}`}>
                      {log.level}
                    </span>
                  </td>
                  <td className="px-4 py-2 text-slate-300 font-mono text-xs">{log.source}</td>
                  <td className="px-4 py-2 text-slate-300 max-w-md truncate">{log.message}</td>
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
            className="p-2 rounded-lg bg-slate-800 border border-slate-700 disabled:opacity-30 hover:bg-slate-700 transition-colors">
            <ChevronLeft size={16} />
          </button>
          <span className="text-sm text-slate-400">Page {page + 1} / {totalPages}</span>
          <button onClick={() => loadPage(page + 1)} disabled={page >= totalPages - 1}
            className="p-2 rounded-lg bg-slate-800 border border-slate-700 disabled:opacity-30 hover:bg-slate-700 transition-colors">
            <ChevronRight size={16} />
          </button>
        </div>
      )}
    </div>
  );
}

function Field({ label, type, value, onChange, options }) {
  return (
    <div className="flex flex-col gap-1">
      <label className="text-xs text-slate-400">{label}</label>
      {type === 'select' ? (
        <select value={value} onChange={(e) => onChange(e.target.value)}
          className="bg-slate-700 border border-slate-600 text-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-emerald-500">
          {options.map((o) => <option key={o} value={o}>{o || 'Tous'}</option>)}
        </select>
      ) : (
        <input type={type} value={value} onChange={(e) => onChange(e.target.value)}
          className="bg-slate-700 border border-slate-600 text-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-emerald-500" />
      )}
    </div>
  );
}
