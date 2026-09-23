import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom';
import { LayoutDashboard, ScrollText, AlertTriangle, Radio, Terminal, History } from 'lucide-react';
import Dashboard from './pages/Dashboard';
import Logs from './pages/Logs';
import Incidents from './pages/Incidents';
import ChatOps from './pages/ChatOps';
import Audit from './pages/Audit';

function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen flex" style={{ background: '#111318', color: '#c9cdd5' }}>
        {/* Sidebar */}
        <nav className="w-52 flex flex-col border-r" style={{ background: '#16181e', borderColor: '#1e2028' }}>
          {/* Brand */}
          <div className="px-4 py-4 border-b" style={{ borderColor: '#1e2028' }}>
            <div className="flex items-center gap-2">
              <div className="w-7 h-7 rounded-md flex items-center justify-center" style={{ background: '#2563eb' }}>
                <Radio size={14} color="#fff" />
              </div>
              <div>
                <div className="text-sm font-semibold text-white leading-tight">Observability</div>
                <div className="text-[10px] tracking-wider uppercase" style={{ color: '#5a5f6b' }}>Platform v1.0</div>
              </div>
            </div>
          </div>

          {/* Nav */}
          <div className="flex-1 px-2 py-3 flex flex-col gap-0.5">
            <div className="px-3 py-1.5 text-[10px] font-medium uppercase tracking-wider" style={{ color: '#4a4f5a' }}>
              Monitoring
            </div>
            <SideLink to="/" icon={<LayoutDashboard size={15} />} label="Dashboard" />
            <SideLink to="/logs" icon={<ScrollText size={15} />} label="Logs" />
            <SideLink to="/incidents" icon={<AlertTriangle size={15} />} label="Incidents" />
            <div className="px-3 py-1.5 mt-3 text-[10px] font-medium uppercase tracking-wider" style={{ color: '#4a4f5a' }}>
              Outils
            </div>
            <SideLink to="/chatops" icon={<Terminal size={15} />} label="ChatOps" />
            <SideLink to="/audit" icon={<History size={15} />} label="Audit" />
          </div>

          {/* Status */}
          <div className="px-4 py-3 border-t" style={{ borderColor: '#1e2028' }}>
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
              <span className="text-xs" style={{ color: '#5a5f6b' }}>Système actif</span>
            </div>
          </div>
        </nav>

        {/* Content */}
        <main className="flex-1 overflow-auto p-5">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/logs" element={<Logs />} />
            <Route path="/incidents" element={<Incidents />} />
            <Route path="/chatops" element={<ChatOps />} />
            <Route path="/audit" element={<Audit />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

function SideLink({ to, icon, label }) {
  return (
    <NavLink
      to={to}
      end
      className={({ isActive }) =>
        `flex items-center gap-2.5 px-3 py-2 rounded text-[13px] transition-all ${
          isActive
            ? 'text-white font-medium'
            : 'hover:text-white'
        }`
      }
      style={({ isActive }) => ({
        background: isActive ? '#1e2028' : 'transparent',
        color: isActive ? '#fff' : '#6b7080',
      })}
    >
      {icon} {label}
    </NavLink>
  );
}

export default App;
