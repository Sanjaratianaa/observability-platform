import { useEffect, useState } from 'react';
import { fetchAudit, fetchNotifications } from '../api';
import { RefreshCw } from 'lucide-react';

const PANEL = { background: '#16181e', border: '1px solid #1e2028' };
const INPUT = { background: '#1e2028', border: '1px solid #2a2d35', color: '#c9cdd5' };
const ACTION_COLOR = {
  INCIDENT_CREATED: '#ef4444',
  INCIDENT_ACKNOWLEDGED: '#f59e0b',
  INCIDENT_RESOLVED: '#22c55e',
  NOTIFICATION_SENT: '#3b82f6',
};

export default function Audit() {
  const [tab, setTab] = useState('audit');
  const [audit, setAudit] = useState([]);
  const [notifs, setNotifs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterAction, setFilterAction] = useState('');
  const [filterChannel, setFilterChannel] = useState('');

  const load = () => {
    setLoading(true);
    Promise.all([
      fetchAudit(filterAction || null),
      fetchNotifications(filterChannel || null),
    ]).then(([a, n]) => {
      setAudit(Array.isArray(a) ? a : []);
      setNotifs(Array.isArray(n) ? n : []);
      setLoading(false);
    }).catch(() => setLoading(false));
  };

  useEffect(() => { load(); }, [filterAction, filterChannel]);

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-end justify-between">
        <div>
          <h2 className="text-lg font-semibold text-white">Audit & Notifications</h2>
          <p className="text-xs mt-0.5" style={{ color: '#5a5f6b' }}>Traçabilité des actions et des envois</p>
        </div>
        <button onClick={load}
          className="p-1.5 rounded transition-colors"
          style={{ color: '#6b7080' }}
          onMouseOver={e => e.currentTarget.style.color = '#fff'}
          onMouseOut={e => e.currentTarget.style.color = '#6b7080'}>
          <RefreshCw size={14} />
        </button>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 border-b" style={{ borderColor: '#1e2028' }}>
        {[{ id: 'audit', label: 'Journal d\'audit' }, { id: 'notifs', label: 'Notifications' }].map(t => (
          <button key={t.id} onClick={() => setTab(t.id)}
            className="px-3 py-2 text-xs font-medium transition-colors"
            style={{
              color: tab === t.id ? '#fff' : '#5a5f6b',
              borderBottom: tab === t.id ? '2px solid #3b82f6' : '2px solid transparent',
            }}>
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'audit' && (
        <>
          <div className="flex gap-2">
            <select value={filterAction} onChange={e => setFilterAction(e.target.value)}
              className="rounded px-2.5 py-1.5 text-xs focus:outline-none" style={INPUT}>
              <option value="">Toutes les actions</option>
              <option value="INCIDENT_CREATED">INCIDENT_CREATED</option>
              <option value="INCIDENT_ACKNOWLEDGED">INCIDENT_ACKNOWLEDGED</option>
              <option value="INCIDENT_RESOLVED">INCIDENT_RESOLVED</option>
              <option value="NOTIFICATION_SENT">NOTIFICATION_SENT</option>
            </select>
          </div>

          <div className="rounded-md overflow-hidden" style={PANEL}>
            {loading ? (
              <p className="text-xs p-6 text-center" style={{ color: '#5a5f6b' }}>Chargement...</p>
            ) : audit.length === 0 ? (
              <p className="text-xs p-6 text-center" style={{ color: '#3a3d45' }}>Aucune entrée d'audit</p>
            ) : (
              <table className="w-full text-xs">
                <thead>
                  <tr style={{ background: '#13151a' }}>
                    <th className="text-left px-4 py-2.5 font-medium" style={{ color: '#5a5f6b' }}>Date</th>
                    <th className="text-left px-4 py-2.5 font-medium" style={{ color: '#5a5f6b' }}>Action</th>
                    <th className="text-left px-4 py-2.5 font-medium" style={{ color: '#5a5f6b' }}>Entité</th>
                    <th className="text-left px-4 py-2.5 font-medium" style={{ color: '#5a5f6b' }}>Détails</th>
                    <th className="text-left px-4 py-2.5 font-medium" style={{ color: '#5a5f6b' }}>Par</th>
                  </tr>
                </thead>
                <tbody>
                  {audit.map((entry, idx) => (
                    <tr key={entry.id} className="hover:bg-white/[0.02] transition-colors"
                      style={{ borderTop: idx > 0 ? '1px solid #1e2028' : 'none' }}>
                      <td className="px-4 py-2 whitespace-nowrap font-mono" style={{ color: '#5a5f6b' }}>
                        {entry.createdAt ? new Date(entry.createdAt).toLocaleString('fr-FR') : '-'}
                      </td>
                      <td className="px-4 py-2">
                        <span className="font-mono text-[11px]" style={{ color: ACTION_COLOR[entry.action] || '#8b909c' }}>
                          {entry.action}
                        </span>
                      </td>
                      <td className="px-4 py-2 font-mono" style={{ color: '#6b7080' }}>
                        {entry.entityType}{entry.entityId ? `#${entry.entityId.slice(0, 8)}` : ''}
                      </td>
                      <td className="px-4 py-2 max-w-xs truncate" style={{ color: '#c9cdd5' }}>{entry.details || '-'}</td>
                      <td className="px-4 py-2" style={{ color: '#6b7080' }}>{entry.performedBy || 'system'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}

      {tab === 'notifs' && (
        <>
          <div className="flex gap-2">
            <select value={filterChannel} onChange={e => setFilterChannel(e.target.value)}
              className="rounded px-2.5 py-1.5 text-xs focus:outline-none" style={INPUT}>
              <option value="">Tous les canaux</option>
              <option value="TEAMS">Teams</option>
              <option value="JIRA">Jira</option>
            </select>
          </div>

          <div className="rounded-md overflow-hidden" style={PANEL}>
            {loading ? (
              <p className="text-xs p-6 text-center" style={{ color: '#5a5f6b' }}>Chargement...</p>
            ) : notifs.length === 0 ? (
              <p className="text-xs p-6 text-center" style={{ color: '#3a3d45' }}>Aucune notification enregistrée</p>
            ) : (
              <table className="w-full text-xs">
                <thead>
                  <tr style={{ background: '#13151a' }}>
                    <th className="text-left px-4 py-2.5 font-medium" style={{ color: '#5a5f6b' }}>Date</th>
                    <th className="text-left px-4 py-2.5 font-medium" style={{ color: '#5a5f6b' }}>Canal</th>
                    <th className="text-left px-4 py-2.5 font-medium" style={{ color: '#5a5f6b' }}>Événement</th>
                    <th className="text-left px-4 py-2.5 font-medium" style={{ color: '#5a5f6b' }}>Incident</th>
                    <th className="text-left px-4 py-2.5 font-medium" style={{ color: '#5a5f6b' }}>Statut</th>
                    <th className="text-left px-4 py-2.5 font-medium" style={{ color: '#5a5f6b' }}>Réf. externe</th>
                  </tr>
                </thead>
                <tbody>
                  {notifs.map((n, idx) => (
                    <tr key={n.id} className="hover:bg-white/[0.02] transition-colors"
                      style={{ borderTop: idx > 0 ? '1px solid #1e2028' : 'none' }}>
                      <td className="px-4 py-2 whitespace-nowrap font-mono" style={{ color: '#5a5f6b' }}>
                        {n.sentAt ? new Date(n.sentAt).toLocaleString('fr-FR') : '-'}
                      </td>
                      <td className="px-4 py-2">
                        <span className="font-mono text-[11px] font-medium"
                          style={{ color: n.channel === 'JIRA' ? '#3b82f6' : '#8b5cf6' }}>
                          {n.channel}
                        </span>
                      </td>
                      <td className="px-4 py-2" style={{ color: '#c9cdd5' }}>{n.eventType}</td>
                      <td className="px-4 py-2 font-mono" style={{ color: '#6b7080' }}>{n.incidentId?.slice(0, 8)}</td>
                      <td className="px-4 py-2">
                        <span className="inline-flex items-center gap-1 text-[11px]"
                          style={{ color: n.success ? '#22c55e' : '#ef4444' }}>
                          <span className="w-1.5 h-1.5 rounded-full" style={{ background: n.success ? '#22c55e' : '#ef4444' }} />
                          {n.success ? 'OK' : 'Échec'}
                        </span>
                      </td>
                      <td className="px-4 py-2 font-mono" style={{ color: '#3b82f6' }}>{n.externalRef || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}
    </div>
  );
}
