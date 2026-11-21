import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { ShieldAlert, Search, RefreshCw, CheckCircle, AlertCircle } from 'lucide-react';

interface LoggerEntry {
  name: string;
  effectiveLevel: string;
  configuredLevel: string | null;
}

export const LoggingAdmin: React.FC = () => {
  const [loggers, setLoggers] = useState<LoggerEntry[]>([]);
  const [filter, setFilter] = useState('com.smartinventory');
  const [loading, setLoading] = useState(true);
  const [statusMsg, setStatusMsg] = useState<string | null>(null);

  const fetchLoggers = async () => {
    setLoading(true);
    setStatusMsg(null);
    try {
      const response = await axios.get('/actuator/loggers');
      const loggerMap = response.data.loggers || {};
      const list: LoggerEntry[] = Object.keys(loggerMap).map((key) => ({
        name: key,
        effectiveLevel: loggerMap[key].effectiveLevel,
        configuredLevel: loggerMap[key].configuredLevel,
      }));
      setLoggers(list);
    } catch (err: any) {
      setStatusMsg(`Failed to fetch Actuator loggers: ${err.response?.data?.message || 'Access Denied (Requires ROLE_ADMIN)'}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLoggers();
  }, []);

  const updateLoggerLevel = async (loggerName: string, level: string) => {
    try {
      await axios.post(`/actuator/loggers/${loggerName}`, { configuredLevel: level });
      setStatusMsg(`Successfully updated logger '${loggerName}' to level ${level}`);
      fetchLoggers();
    } catch (err: any) {
      setStatusMsg(`Failed to update logger '${loggerName}': ${err.response?.data?.message || err.message}`);
    }
  };

  const filteredLoggers = loggers.filter((l) => l.name.toLowerCase().includes(filter.toLowerCase()));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-100 flex items-center gap-2">
            <ShieldAlert className="w-5 h-5 text-amber-400" />
            Dynamic Log Level Governance (Actuator Admin)
          </h2>
          <p className="text-xs text-slate-400">Zero-downtime logback level adjustments for production telemetry</p>
        </div>
        <button
          onClick={fetchLoggers}
          disabled={loading}
          className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:text-white text-xs font-medium"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Refresh Loggers
        </button>
      </div>

      {statusMsg && (
        <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 text-xs text-sky-300 flex items-center gap-2">
          <CheckCircle className="w-4 h-4 text-sky-400 shrink-0" />
          <span>{statusMsg}</span>
        </div>
      )}

      {/* Filter Bar */}
      <div className="p-4 rounded-2xl bg-slate-900/80 border border-slate-800 flex items-center gap-3">
        <Search className="w-4 h-4 text-slate-500" />
        <input
          type="text"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          placeholder="Filter loggers (e.g., com.smartinventory, org.springframework)..."
          className="w-full bg-transparent text-xs text-slate-100 focus:outline-none"
        />
      </div>

      {/* Logger Table */}
      <div className="rounded-2xl bg-slate-900/80 border border-slate-800 overflow-hidden shadow-xl">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-950/80 border-b border-slate-800 text-[11px] font-bold text-slate-400 uppercase tracking-wider">
              <th className="px-5 py-3.5">Logger Category Name</th>
              <th className="px-5 py-3.5">Effective Level</th>
              <th className="px-5 py-3.5">Set Log Level</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 text-xs text-slate-200">
            {filteredLoggers.slice(0, 50).map((logger) => (
              <tr key={logger.name} className="hover:bg-slate-800/40">
                <td className="px-5 py-3 font-mono text-sky-400 text-xs">{logger.name}</td>
                <td className="px-5 py-3">
                  <span className="px-2.5 py-1 rounded-md text-[11px] font-bold bg-slate-800 text-slate-300 border border-slate-700">
                    {logger.effectiveLevel}
                  </span>
                </td>
                <td className="px-5 py-3">
                  <div className="flex items-center gap-1.5">
                    {['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'].map((lvl) => (
                      <button
                        key={lvl}
                        onClick={() => updateLoggerLevel(logger.name, lvl)}
                        className={`px-2.5 py-1 rounded-md text-[10px] font-bold transition-all ${
                          logger.effectiveLevel === lvl
                            ? 'bg-sky-500 text-white shadow-md'
                            : 'bg-slate-950 text-slate-400 hover:text-slate-200 border border-slate-800'
                        }`}
                      >
                        {lvl}
                      </button>
                    ))}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
