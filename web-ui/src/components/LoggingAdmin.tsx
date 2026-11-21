import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { ShieldAlert, Search, RefreshCw, CheckCircle, RotateCcw, X, Layers } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

interface ServiceStatus {
  id: string;
  name: string;
  status: string;
  url: string;
}

interface LoggerEntry {
  serviceId: string;
  serviceName: string;
  name: string;
  effectiveLevel: string;
  configuredLevel: string | null;
}

const LOG_LEVELS = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'OFF'];

// Red triangle filter icon matching the exact red triangle indicator requested
const RedTriangleFilterIcon: React.FC<{ active?: boolean; onClick?: () => void }> = ({ active, onClick }) => (
  <button
    type="button"
    onClick={onClick}
    title="Click to toggle column filter"
    className="inline-flex items-center justify-center p-0.5 rounded hover:bg-slate-800 transition-all focus:outline-none"
  >
    <svg
      viewBox="0 0 24 24"
      className={`w-3.5 h-3.5 transition-all ${
        active
          ? 'text-rose-400 fill-rose-500 scale-110 drop-shadow-[0_0_8px_rgba(244,63,94,0.8)]'
          : 'text-rose-500 fill-rose-500/80 hover:fill-rose-400 hover:scale-105'
      }`}
    >
      <polygon points="12,4 2,20 22,20" />
    </svg>
  </button>
);

export const LoggingAdmin: React.FC = () => {
  const { token } = useAuth();
  const [loggers, setLoggers] = useState<LoggerEntry[]>([]);
  const [services, setServices] = useState<ServiceStatus[]>([]);
  const [filterName, setFilterName] = useState('');
  const [filterService, setFilterService] = useState('ALL');
  const [filterEffectiveLevel, setFilterEffectiveLevel] = useState('ALL');
  const [filterSetLevel, setFilterSetLevel] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [statusMsg, setStatusMsg] = useState<string | null>(null);

  // States to toggle visible filter inputs for each column header
  const [showNameFilter, setShowNameFilter] = useState(true);
  const [showEffectiveFilter, setShowEffectiveFilter] = useState(true);
  const [showSetFilter, setShowSetFilter] = useState(true);

  const fetchLoggers = async () => {
    setLoading(true);
    setStatusMsg(null);
    try {
      // First attempt multi-service aggregator endpoint
      const response = await axios.get('/api/admin/multi-loggers');
      const data = response.data;
      if (data && Array.isArray(data.loggers)) {
        setLoggers(data.loggers);
        if (Array.isArray(data.services)) {
          setServices(data.services);
        }
        setLoading(false);
        return;
      }
    } catch (err: any) {
      // Fallback to single-service Spring Boot actuator
      try {
        const response = await axios.get('/actuator/loggers');
        const loggerMap = response.data.loggers || {};
        const list: LoggerEntry[] = Object.keys(loggerMap).map((key) => ({
          serviceId: 'inventory-service',
          serviceName: 'Inventory Service',
          name: key,
          effectiveLevel: loggerMap[key].effectiveLevel,
          configuredLevel: loggerMap[key].configuredLevel,
        }));
        setLoggers(list);
        setServices([
          { id: 'inventory-service', name: 'Inventory Service', status: 'UP', url: 'local:8081' }
        ]);
      } catch (fallbackErr: any) {
        setStatusMsg(`Failed to fetch loggers across services: ${err.response?.data?.message || err.message}`);
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (token) {
      fetchLoggers();
    }
  }, [token]);

  const updateLoggerLevel = async (serviceId: string, loggerName: string, level: string | null) => {
    try {
      try {
        await axios.post(`/api/admin/multi-loggers/${serviceId}/${encodeURIComponent(loggerName)}`, {
          configuredLevel: level,
        });
      } catch {
        // Fallback for direct actuator endpoint
        await axios.post(`/actuator/loggers/${loggerName}`, { configuredLevel: level });
      }
      setStatusMsg(`Updated logger '${loggerName}' [${serviceId}] level to ${level || 'DEFAULT (Inherited)'}`);
      fetchLoggers();
    } catch (err: any) {
      setStatusMsg(`Failed to update logger '${loggerName}': ${err.response?.data?.message || err.message}`);
    }
  };

  const handleRevertAllOverrides = async () => {
    const overridden = filteredLoggers.filter((l) => l.configuredLevel !== null);
    if (overridden.length === 0) return;
    setLoading(true);
    try {
      for (const logger of overridden) {
        await updateLoggerLevel(logger.serviceId, logger.name, null);
      }
      setStatusMsg(`Reverted ${overridden.length} overridden loggers back to default level.`);
      fetchLoggers();
    } catch (err: any) {
      setStatusMsg(`Failed to revert loggers: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const overriddenCount = loggers.filter((l) => l.configuredLevel !== null).length;

  const filteredLoggers = loggers.filter((l) => {
    if (filterService !== 'ALL' && l.serviceId !== filterService) {
      return false;
    }

    const matchesName =
      l.name.toLowerCase().includes(filterName.toLowerCase()) ||
      l.serviceName.toLowerCase().includes(filterName.toLowerCase());
    if (!matchesName) return false;

    if (filterEffectiveLevel !== 'ALL' && l.effectiveLevel !== filterEffectiveLevel) {
      return false;
    }

    if (filterSetLevel === 'OVERRIDDEN' && l.configuredLevel === null) return false;
    if (filterSetLevel === 'DEFAULT' && l.configuredLevel !== null) return false;
    if (
      filterSetLevel !== 'ALL' &&
      filterSetLevel !== 'OVERRIDDEN' &&
      filterSetLevel !== 'DEFAULT' &&
      l.configuredLevel !== filterSetLevel
    ) {
      return false;
    }

    return true;
  });

  const hasActiveFilter =
    filterName !== '' || filterService !== 'ALL' || filterEffectiveLevel !== 'ALL' || filterSetLevel !== 'ALL';

  const clearFilters = () => {
    setFilterName('');
    setFilterService('ALL');
    setFilterEffectiveLevel('ALL');
    setFilterSetLevel('ALL');
  };

  const getServiceBadgeColor = (serviceId: string) => {
    switch (serviceId) {
      case 'inventory-service':
        return 'bg-sky-500/10 text-sky-400 border-sky-500/30';
      case 'catalog-service':
        return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30';
      case 'forecasting-service':
        return 'bg-purple-500/10 text-purple-400 border-purple-500/30';
      default:
        return 'bg-slate-800 text-slate-400 border-slate-700';
    }
  };

  return (
    <div className="space-y-5">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-100 flex items-center gap-2">
            <ShieldAlert className="w-5 h-5 text-amber-400" />
            Dynamic Log Level Governance (Actuator Admin)
          </h2>
          <p className="text-xs text-slate-400">
            Zero-downtime logback level adjustments for production telemetry & connection pools
          </p>
        </div>
        <div className="flex items-center gap-3">
          {overriddenCount > 0 && (
            <button
              onClick={handleRevertAllOverrides}
              className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-400 hover:bg-amber-500/20 text-xs font-bold transition-all shadow-sm"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              Revert {overriddenCount} Overrides to INFO
            </button>
          )}
          <button
            onClick={fetchLoggers}
            disabled={loading}
            className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:text-white text-xs font-medium transition-all"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            Refresh Loggers
          </button>
        </div>
      </div>

      {statusMsg && (
        <div className="p-3.5 rounded-xl bg-slate-900 border border-slate-800 text-xs text-sky-300 flex items-center gap-2">
          <CheckCircle className="w-4 h-4 text-sky-400 shrink-0" />
          <span>{statusMsg}</span>
        </div>
      )}

      {/* Main Logger Governance Table */}
      <div className="rounded-2xl bg-slate-900/80 border border-slate-800 overflow-hidden shadow-xl">
        {/* Table Toolbar / Counter */}
        <div className="px-5 py-3.5 bg-slate-950/70 border-b border-slate-800 flex flex-wrap items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-3">
            <span className="text-slate-400 font-medium">
              Showing <strong className="text-slate-100 font-bold">{filteredLoggers.length}</strong> of{' '}
              <strong className="text-slate-300">{loggers.length}</strong> loggers
            </span>
            {hasActiveFilter && (
              <span className="px-2 py-0.5 text-[10px] font-bold rounded-full bg-sky-500/10 text-sky-400 border border-sky-500/20">
                Filtered
              </span>
            )}
          </div>

          <div className="flex items-center gap-3">
            {services.length > 1 && (
              <div className="flex items-center gap-1.5 text-xs text-slate-400">
                <Layers className="w-3.5 h-3.5" />
                <span>Service:</span>
                <select
                  value={filterService}
                  onChange={(e) => setFilterService(e.target.value)}
                  className="px-2.5 py-1 rounded-lg bg-slate-900 border border-slate-800 text-xs text-slate-200 focus:outline-none focus:border-sky-500"
                >
                  <option value="ALL">All Services</option>
                  <option value="inventory-service">Inventory Service</option>
                  <option value="catalog-service">Catalog Service</option>
                  <option value="forecasting-service">Forecasting Service</option>
                </select>
              </div>
            )}

            {hasActiveFilter && (
              <button
                onClick={clearFilters}
                className="text-sky-400 hover:text-sky-300 text-xs font-semibold flex items-center gap-1 transition-colors"
              >
                <X className="w-3.5 h-3.5" /> Clear All Filters
              </button>
            )}
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-950/90 border-b border-slate-800 text-[11px] font-bold text-slate-300 uppercase tracking-wider">
                {/* Column 1: Logger Category Name */}
                <th className="px-5 py-3.5 w-5/12">
                  <div className="space-y-2">
                    <div className="flex items-center gap-1">
                      <span className="text-slate-300 font-bold">LOGGER CATEGORY NAME</span>
                      <RedTriangleFilterIcon
                        active={filterName !== ''}
                        onClick={() => setShowNameFilter(!showNameFilter)}
                      />
                    </div>
                    {showNameFilter && (
                      <div className="relative flex items-center">
                        <Search className="w-3.5 h-3.5 text-slate-500 absolute left-2.5 pointer-events-none" />
                        <input
                          type="text"
                          value={filterName}
                          onChange={(e) => setFilterName(e.target.value)}
                          placeholder="Filter by category name..."
                          className="w-full pl-8 pr-6 py-1.5 rounded-lg bg-slate-900/90 border border-slate-800 text-xs font-mono text-slate-100 placeholder:text-slate-500 focus:outline-none focus:border-sky-500 transition-colors"
                        />
                        {filterName && (
                          <button onClick={() => setFilterName('')} className="absolute right-2 text-slate-500 hover:text-slate-200">
                            <X className="w-3 h-3" />
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                </th>

                {/* Column 2: Effective Level */}
                <th className="px-5 py-3.5 w-3/12">
                  <div className="space-y-2">
                    <div className="flex items-center gap-1">
                      <span className="text-slate-300 font-bold">EFFECTIVE LEVEL</span>
                      <RedTriangleFilterIcon
                        active={filterEffectiveLevel !== 'ALL'}
                        onClick={() => setShowEffectiveFilter(!showEffectiveFilter)}
                      />
                    </div>
                    {showEffectiveFilter && (
                      <select
                        value={filterEffectiveLevel}
                        onChange={(e) => setFilterEffectiveLevel(e.target.value)}
                        className="w-full px-2.5 py-1.5 rounded-lg bg-slate-900/90 border border-slate-800 text-xs font-medium text-slate-200 focus:outline-none focus:border-sky-500 transition-colors"
                      >
                        <option value="ALL">All Effective Levels</option>
                        <option value="TRACE">TRACE</option>
                        <option value="DEBUG">DEBUG</option>
                        <option value="INFO">INFO</option>
                        <option value="WARN">WARN</option>
                        <option value="ERROR">ERROR</option>
                        <option value="OFF">OFF</option>
                      </select>
                    )}
                  </div>
                </th>

                {/* Column 3: Set Log Level */}
                <th className="px-5 py-3.5 w-4/12">
                  <div className="space-y-2">
                    <div className="flex items-center gap-1">
                      <span className="text-slate-300 font-bold">SET LOG LEVEL</span>
                      <RedTriangleFilterIcon
                        active={filterSetLevel !== 'ALL'}
                        onClick={() => setShowSetFilter(!showSetFilter)}
                      />
                    </div>
                    {showSetFilter && (
                      <select
                        value={filterSetLevel}
                        onChange={(e) => setFilterSetLevel(e.target.value)}
                        className="w-full px-2.5 py-1.5 rounded-lg bg-slate-900/90 border border-slate-800 text-xs font-medium text-slate-200 focus:outline-none focus:border-sky-500 transition-colors"
                      >
                        <option value="ALL">All States</option>
                        <option value="OVERRIDDEN">Overridden Only ({overriddenCount})</option>
                        <option value="DEFAULT">Default / Inherited</option>
                        <option value="TRACE">Configured TRACE</option>
                        <option value="DEBUG">Configured DEBUG</option>
                        <option value="INFO">Configured INFO</option>
                        <option value="WARN">Configured WARN</option>
                        <option value="ERROR">Configured ERROR</option>
                        <option value="OFF">Configured OFF</option>
                      </select>
                    )}
                  </div>
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-xs text-slate-200">
              {filteredLoggers.length > 0 ? (
                filteredLoggers.slice(0, 150).map((logger) => {
                  const isOverridden = logger.configuredLevel !== null && logger.configuredLevel !== undefined;
                  return (
                    <tr key={`${logger.serviceId}-${logger.name}`} className="hover:bg-slate-800/40 transition-colors">
                      {/* Logger Category Name */}
                      <td className="px-5 py-3 font-mono text-xs">
                        <div className="flex items-center gap-2 flex-wrap">
                          {services.length > 1 && (
                            <span
                              className={`px-1.5 py-0.5 rounded text-[9px] font-bold border uppercase ${getServiceBadgeColor(
                                logger.serviceId
                              )}`}
                            >
                              {logger.serviceName.replace(' Service', '')}
                            </span>
                          )}

                          <span className="text-sky-400 font-semibold">{logger.name}</span>

                          {isOverridden && (
                            <span className="px-1.5 py-0.5 rounded text-[9px] font-extrabold uppercase bg-amber-500/20 text-amber-400 border border-amber-500/30">
                              OVERRIDDEN
                            </span>
                          )}
                        </div>
                      </td>

                      {/* Effective Level */}
                      <td className="px-5 py-3">
                        <span
                          className={`px-2.5 py-1 rounded-md text-[11px] font-bold border ${
                            logger.effectiveLevel === 'DEBUG' || logger.effectiveLevel === 'TRACE'
                              ? 'bg-amber-500/10 text-amber-400 border-amber-500/30'
                              : logger.effectiveLevel === 'WARN' || logger.effectiveLevel === 'ERROR'
                              ? 'bg-rose-500/10 text-rose-400 border-rose-500/30'
                              : 'bg-slate-800 text-slate-300 border-slate-700'
                          }`}
                        >
                          {logger.effectiveLevel}
                        </span>
                      </td>

                      {/* Set Log Level */}
                      <td className="px-5 py-3">
                        <div className="flex items-center gap-1.5 flex-wrap">
                          {LOG_LEVELS.map((lvl) => {
                            const isActive = logger.effectiveLevel === lvl;
                            return (
                              <button
                                key={lvl}
                                onClick={() => updateLoggerLevel(logger.serviceId, logger.name, lvl)}
                                className={`px-2.5 py-1 rounded-md text-[10px] font-bold transition-all ${
                                  isActive
                                    ? 'bg-sky-500 text-white shadow-md shadow-sky-500/20'
                                    : 'bg-slate-950 text-slate-400 hover:text-slate-200 border border-slate-800 hover:bg-slate-800'
                                }`}
                              >
                                {lvl}
                              </button>
                            );
                          })}

                          {isOverridden && (
                            <button
                              onClick={() => updateLoggerLevel(logger.serviceId, logger.name, null)}
                              title="Revert override to default level"
                              className="px-2 py-1 rounded-md text-[10px] font-bold bg-amber-500/10 text-amber-400 hover:bg-amber-500/20 border border-amber-500/30 transition-all flex items-center gap-1"
                            >
                              <RotateCcw className="w-3 h-3" /> Revert
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td colSpan={3} className="px-5 py-8 text-center text-slate-500 text-xs">
                    No loggers matching current column filters.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
