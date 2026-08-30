import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom';
import { Activity, FileText, AlertTriangle } from 'lucide-react';
import Dashboard from './pages/Dashboard';
import Logs from './pages/Logs';
import Incidents from './pages/Incidents';

function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen bg-slate-900 text-slate-200 flex">
        {/* Sidebar */}
        <nav className="w-56 bg-slate-800 border-r border-slate-700 p-4 flex flex-col gap-1">
          <h1 className="text-lg font-bold text-white mb-6 flex items-center gap-2">
            <Activity size={20} className="text-emerald-400" />
            Observability
          </h1>
          <SideLink to="/" icon={<Activity size={18} />} label="Dashboard" />
          <SideLink to="/logs" icon={<FileText size={18} />} label="Logs" />
          <SideLink to="/incidents" icon={<AlertTriangle size={18} />} label="Incidents" />
        </nav>

        {/* Content */}
        <main className="flex-1 p-6 overflow-auto">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/logs" element={<Logs />} />
            <Route path="/incidents" element={<Incidents />} />
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
        `flex items-center gap-2 px-3 py-2 rounded-lg text-sm transition-colors ${
          isActive
            ? 'bg-emerald-500/10 text-emerald-400 font-medium'
            : 'text-slate-400 hover:text-white hover:bg-slate-700'
        }`
      }
    >
      {icon} {label}
    </NavLink>
  );
}

export default App;
