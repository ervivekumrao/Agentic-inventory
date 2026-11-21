import React, { useState } from 'react';
import axios from 'axios';
import { Lock, User, AlertCircle, ShieldCheck, Cpu, Database, Server } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const LoginScreen: React.FC = () => {
  const { login } = useAuth();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleQuickLogin = (roleUser: string, rolePass: string) => {
    setUsername(roleUser);
    setPassword(rolePass);
    executeLogin(roleUser, rolePass);
  };

  const executeLogin = async (userToSubmit: string, passToSubmit: string) => {
    setLoading(true);
    setError(null);

    try {
      const response = await axios.post('/api/auth/login', { username: userToSubmit, password: passToSubmit });
      const { accessToken, username: authUsername, roles } = response.data;
      login(accessToken, authUsername, roles);
    } catch (err: any) {
      setError(err.response?.data?.error || err.response?.data?.message || 'Authentication failed. Please check backend connection.');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    executeLogin(username, password);
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-center items-center p-6 relative overflow-hidden">
      {/* Background Glow Accents */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-sky-500/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-10 right-10 w-[400px] h-[400px] bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-xl bg-slate-900/90 border border-slate-800 rounded-3xl p-8 shadow-2xl backdrop-blur-xl relative z-10">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-tr from-sky-500 to-indigo-600 text-white shadow-lg shadow-sky-500/25 mb-4">
            <Cpu className="w-8 h-8" />
          </div>
          <h1 className="text-2xl font-black text-slate-100 tracking-tight">Smart Inventory Enterprise</h1>
          <p className="text-xs text-sky-400 font-semibold tracking-wider uppercase mt-1">High-Scale Telemetry & Agentic AI Hub</p>
        </div>

        {error && (
          <div className="mb-6 p-4 rounded-2xl bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs flex items-center gap-3">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">Username</label>
            <div className="relative">
              <User className="w-5 h-5 text-slate-500 absolute left-3.5 top-3.5" />
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                className="w-full pl-11 pr-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-slate-100 text-sm focus:outline-none focus:border-sky-500 transition-all"
                placeholder="admin"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">Password</label>
            <div className="relative">
              <Lock className="w-5 h-5 text-slate-500 absolute left-3.5 top-3.5" />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="w-full pl-11 pr-4 py-3 rounded-xl bg-slate-950/80 border border-slate-800 text-slate-100 text-sm focus:outline-none focus:border-sky-500 transition-all"
                placeholder="admin123"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3.5 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white font-bold text-sm shadow-xl shadow-sky-500/25 transition-all disabled:opacity-50 flex items-center justify-center gap-2"
          >
            <ShieldCheck className="w-5 h-5" />
            <span>{loading ? 'Authenticating...' : 'Sign In & Access Platform'}</span>
          </button>
        </form>

        <div className="mt-8 pt-6 border-t border-slate-800/80">
          <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-3 text-center">1-Click Fast Pilot Sign-In</p>
          <div className="grid grid-cols-2 gap-2">
            <button
              onClick={() => handleQuickLogin('admin', 'admin123')}
              className="px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-800 hover:border-sky-500/50 text-xs font-medium text-slate-300 hover:text-sky-400 transition-all text-left flex items-center gap-2"
            >
              <div className="w-2 h-2 rounded-full bg-sky-400 shrink-0" />
              <div>
                <div className="font-bold text-slate-200">Admin</div>
                <div className="text-[10px] text-slate-500">ROLE_ADMIN</div>
              </div>
            </button>

            <button
              onClick={() => handleQuickLogin('manager', 'manager123')}
              className="px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-800 hover:border-indigo-500/50 text-xs font-medium text-slate-300 hover:text-indigo-400 transition-all text-left flex items-center gap-2"
            >
              <div className="w-2 h-2 rounded-full bg-indigo-400 shrink-0" />
              <div>
                <div className="font-bold text-slate-200">Manager</div>
                <div className="text-[10px] text-slate-500">ROLE_MANAGER</div>
              </div>
            </button>

            <button
              onClick={() => handleQuickLogin('operator', 'operator123')}
              className="px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-800 hover:border-emerald-500/50 text-xs font-medium text-slate-300 hover:text-emerald-400 transition-all text-left flex items-center gap-2"
            >
              <div className="w-2 h-2 rounded-full bg-emerald-400 shrink-0" />
              <div>
                <div className="font-bold text-slate-200">Operator</div>
                <div className="text-[10px] text-slate-500">ENTRY_OPERATOR</div>
              </div>
            </button>

            <button
              onClick={() => handleQuickLogin('viewer', 'viewer123')}
              className="px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-800 hover:border-amber-500/50 text-xs font-medium text-slate-300 hover:text-amber-400 transition-all text-left flex items-center gap-2"
            >
              <div className="w-2 h-2 rounded-full bg-amber-400 shrink-0" />
              <div>
                <div className="font-bold text-slate-200">Viewer</div>
                <div className="text-[10px] text-slate-500">ROLE_VIEWER</div>
              </div>
            </button>
          </div>
        </div>

        <div className="mt-6 flex items-center justify-between text-[11px] text-slate-500 px-1">
          <div className="flex items-center gap-1.5">
            <Database className="w-3.5 h-3.5 text-emerald-400" />
            <span>PostgreSQL + Redis Connected</span>
          </div>
          <div className="flex items-center gap-1.5">
            <Server className="w-3.5 h-3.5 text-sky-400" />
            <span>Java 21 Spring Boot Active</span>
          </div>
        </div>
      </div>
    </div>
  );
};
