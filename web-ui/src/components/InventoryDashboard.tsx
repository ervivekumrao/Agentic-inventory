import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { Package, DollarSign, AlertTriangle, Layers, Building2, RefreshCw } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell, PieChart, Pie } from 'recharts';

interface InventoryItem {
  id: number;
  sku: string;
  quantity: number;
  reorderThreshold: number;
  warehouseLocation: string;
  category?: string;
  price?: number;
}

const COLORS = ['#38bdf8', '#818cf8', '#34d399', '#fbbf24', '#f87171', '#a78bfa'];

export const InventoryDashboard: React.FC = () => {
  const [items, setItems] = useState<InventoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchInventory = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get('/api/inventory');
      setItems(response.data || []);
    } catch (err: any) {
      setError('Failed to fetch stock telemetry data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInventory();
  }, []);

  const totalSkus = items.length;
  const totalQuantity = items.reduce((acc, curr) => acc + (curr.quantity || 0), 0);
  const lowStockCount = items.filter((item) => (item.quantity || 0) <= (item.reorderThreshold || 10)).length;
  const totalValuation = items.reduce((acc, curr) => acc + (curr.quantity || 0) * (curr.price || 15.0), 0);

  // Warehouse breakdown aggregation
  const warehouseMap: Record<string, number> = {};
  items.forEach((item) => {
    const wh = item.warehouseLocation || 'Unassigned';
    warehouseMap[wh] = (warehouseMap[wh] || 0) + (item.quantity || 0);
  });
  const warehouseData = Object.keys(warehouseMap).map((key) => ({
    name: key,
    quantity: warehouseMap[key],
  }));

  // Category valuation aggregation
  const categoryMap: Record<string, number> = {};
  items.forEach((item) => {
    const cat = item.category || 'General';
    const val = (item.quantity || 0) * (item.price || 15.0);
    categoryMap[cat] = (categoryMap[cat] || 0) + val;
  });
  const categoryData = Object.keys(categoryMap).map((key) => ({
    name: key,
    value: Math.round(categoryMap[key]),
  }));

  return (
    <div className="space-y-6">
      {/* Top Controls & Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-100 flex items-center gap-2">
            Operations Telemetry & Stock Overview
          </h2>
          <p className="text-xs text-slate-400">Real-time telemetry and warehouse distribution metrics</p>
        </div>
        <button
          onClick={fetchInventory}
          disabled={loading}
          className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:text-white text-xs font-medium transition-colors"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Refresh Metrics
        </button>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-lg relative overflow-hidden group">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400">Total Distinct SKUs</span>
            <div className="w-9 h-9 rounded-xl bg-sky-500/10 border border-sky-500/20 flex items-center justify-center text-sky-400">
              <Package className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-extrabold text-slate-100">{totalSkus}</span>
            <span className="text-xs text-slate-400 ml-2">Active SKUs</span>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-lg relative overflow-hidden group">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400">Total Physical Units</span>
            <div className="w-9 h-9 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <Layers className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-extrabold text-slate-100">{totalQuantity.toLocaleString()}</span>
            <span className="text-xs text-emerald-400 ml-2 font-medium">In Stock</span>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-lg relative overflow-hidden group">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400">Low Stock Reorders</span>
            <div className="w-9 h-9 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
              <AlertTriangle className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-extrabold text-amber-400">{lowStockCount}</span>
            <span className="text-xs text-slate-400 ml-2">Items Below Reorder Point</span>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-lg relative overflow-hidden group">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400">Est. Holding Valuation</span>
            <div className="w-9 h-9 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400">
              <DollarSign className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-extrabold text-slate-100">${totalValuation.toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
            <span className="text-xs text-indigo-400 ml-2 font-medium">Asset Valuation</span>
          </div>
        </div>
      </div>

      {/* Visual Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Warehouse Unit Distribution Bar Chart */}
        <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-xl">
          <h3 className="text-sm font-bold text-slate-200 mb-1 flex items-center gap-2">
            <Building2 className="w-4 h-4 text-sky-400" />
            Physical Unit Volume per Storage Facility
          </h3>
          <p className="text-xs text-slate-400 mb-6">Stock unit distribution across warehouses</p>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={warehouseData}>
                <XAxis dataKey="name" stroke="#64748b" fontSize={11} tickLine={false} />
                <YAxis stroke="#64748b" fontSize={11} tickLine={false} />
                <Tooltip
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px', fontSize: '12px' }}
                />
                <Bar dataKey="quantity" radius={[6, 6, 0, 0]}>
                  {warehouseData.map((_, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Category Valuation Pie Chart */}
        <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800/80 shadow-xl">
          <h3 className="text-sm font-bold text-slate-200 mb-1 flex items-center gap-2">
            <DollarSign className="w-4 h-4 text-indigo-400" />
            Holding Valuation by Product Category ($)
          </h3>
          <p className="text-xs text-slate-400 mb-6">Capital allocation across inventory categories</p>
          <div className="h-64 flex items-center justify-center">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={categoryData}
                  dataKey="value"
                  nameKey="name"
                  cx="50%"
                  cy="50%"
                  outerRadius={80}
                  innerRadius={45}
                  paddingAngle={4}
                  label={({ name, percent }) => `${name} (${(percent * 100).toFixed(0)}%)`}
                >
                  {categoryData.map((_, index) => (
                    <Cell key={`pie-cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px', fontSize: '12px' }}
                  formatter={(val: number) => `$${val.toLocaleString()}`}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
};
