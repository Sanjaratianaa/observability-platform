import { useEffect, useRef, useState } from 'react';
import { sendChatOps } from '../api';
import { Terminal, Send } from 'lucide-react';

const PANEL = { background: '#16181e', border: '1px solid #1e2028' };

const SUGGESTIONS = ['help', 'list', 'stats', 'ack', 'resolve'];

export default function ChatOps() {
  const [history, setHistory] = useState([
    { type: 'system', text: 'ChatOps — interface de commandes. Tapez "help" pour la liste des commandes.' },
  ]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const bottomRef = useRef(null);
  const inputRef = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [history]);

  const send = async (cmd) => {
    const command = (cmd || input).trim();
    if (!command || sending) return;
    setInput('');
    setSending(true);
    setHistory(h => [...h, { type: 'cmd', text: command }]);
    try {
      const res = await sendChatOps(command);
      setHistory(h => [...h, { type: 'res', text: res || '(réponse vide)' }]);
    } catch (e) {
      setHistory(h => [...h, { type: 'err', text: 'Erreur: ' + e.message }]);
    }
    setSending(false);
    inputRef.current?.focus();
  };

  return (
    <div className="space-y-4 h-full flex flex-col">
      {/* Header */}
      <div className="flex items-end justify-between">
        <div>
          <h2 className="text-lg font-semibold text-white flex items-center gap-2">
            <Terminal size={18} style={{ color: '#3b82f6' }} /> ChatOps
          </h2>
          <p className="text-xs mt-0.5" style={{ color: '#5a5f6b' }}>Commandes interactives sur les incidents</p>
        </div>
      </div>

      {/* Terminal */}
      <div className="rounded-md flex-1 flex flex-col min-h-[420px]" style={PANEL}>
        {/* Title bar */}
        <div className="px-4 py-2.5 border-b flex items-center gap-2" style={{ borderColor: '#1e2028', background: '#13151a' }}>
          <span className="w-2.5 h-2.5 rounded-full" style={{ background: '#ef4444' }} />
          <span className="w-2.5 h-2.5 rounded-full" style={{ background: '#f59e0b' }} />
          <span className="w-2.5 h-2.5 rounded-full" style={{ background: '#22c55e' }} />
          <span className="ml-2 text-[11px] font-mono" style={{ color: '#5a5f6b' }}>chatops — /api/chatops</span>
        </div>

        {/* Output */}
        <div className="flex-1 overflow-auto p-4 font-mono text-xs space-y-2" onClick={() => inputRef.current?.focus()}>
          {history.map((entry, i) => (
            <div key={i}>
              {entry.type === 'cmd' && (
                <div className="flex gap-2">
                  <span style={{ color: '#3b82f6' }}>❯</span>
                  <span className="text-white">{entry.text}</span>
                </div>
              )}
              {entry.type === 'res' && (
                <pre className="whitespace-pre-wrap pl-4" style={{ color: '#9aa0ad' }}>{entry.text}</pre>
              )}
              {entry.type === 'err' && (
                <pre className="whitespace-pre-wrap pl-4" style={{ color: '#ef4444' }}>{entry.text}</pre>
              )}
              {entry.type === 'system' && (
                <pre className="whitespace-pre-wrap" style={{ color: '#4a4f5a' }}>{entry.text}</pre>
              )}
            </div>
          ))}
          {sending && <div className="pl-4 animate-pulse" style={{ color: '#5a5f6b' }}>...</div>}
          <div ref={bottomRef} />
        </div>

        {/* Input */}
        <div className="border-t px-4 py-3 flex items-center gap-2" style={{ borderColor: '#1e2028' }}>
          <span className="font-mono text-xs" style={{ color: '#3b82f6' }}>❯</span>
          <input
            ref={inputRef}
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && send()}
            placeholder="help, list, stats, ack <id>, resolve <id>..."
            className="flex-1 bg-transparent font-mono text-xs text-white focus:outline-none placeholder:text-[#3a3d45]"
            autoFocus
          />
          <button onClick={() => send()} disabled={sending}
            className="p-1.5 rounded transition-colors disabled:opacity-30"
            style={{ color: '#3b82f6' }}>
            <Send size={14} />
          </button>
        </div>
      </div>

      {/* Quick commands */}
      <div className="flex gap-2 flex-wrap">
        {SUGGESTIONS.map(cmd => (
          <button key={cmd} onClick={() => send(cmd)}
            className="px-2.5 py-1 rounded text-[11px] font-mono transition-colors"
            style={{ ...PANEL, color: '#6b7080' }}
            onMouseOver={e => { e.currentTarget.style.color = '#fff'; e.currentTarget.style.borderColor = '#3b82f6'; }}
            onMouseOut={e => { e.currentTarget.style.color = '#6b7080'; e.currentTarget.style.borderColor = '#1e2028'; }}>
            {cmd}
          </button>
        ))}
      </div>
    </div>
  );
}
