import React, { useState, useEffect, useMemo } from 'react';
import axios from 'axios';
import {
  useReactTable,
  getCoreRowModel,
  getFilteredRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  flexRender,
  ColumnDef,
} from '@tanstack/react-table';
import { Search, Filter, AlertTriangle, RefreshCw, ShoppingCart, Sliders, ChevronLeft, ChevronRight, Plus } from 'lucide-react';

interface InventoryItem {
  id: number;
  sku: string;
  quantity: number;
  reorderThreshold: number;
  warehouseLocation: string;
  category?: string;
  price?: number;
}

export const InventoryTable: React.FC = () => {
  const [data, setData] = useState<InventoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [globalFilter, setGlobalFilter] = useState('');
  const [warehouseFilter, setWarehouseFilter] = useState('ALL');
  const [lowStockOnly, setLowStockOnly] = useState(false);

  // Reservation modal state
  const [reserveModalItem, setReserveModalItem] = useState<InventoryItem | null>(null);
  const [reserveQty, setReserveQty] = useState(1);
  const [reserveMsg, setReserveMsg] = useState<string | null>(null);

  const fetchInventory = async () => {
    setLoading(true);
    try {
      const response = await axios.get('/api/inventory');
      setData(response.data || []);
    } catch (err) {
      console.error('Failed to load inventory', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInventory();
  }, []);

  const filteredData = useMemo(() => {
    return data.filter((item) => {
      if (warehouseFilter !== 'ALL' && item.warehouseLocation !== warehouseFilter) {
        return false;
      }
      if (lowStockOnly && item.quantity > item.reorderThreshold) {
        return false;
      }
      return true;
    });
  }, [data, warehouseFilter, lowStockOnly]);

  const handleReserve = async () => {
    if (!reserveModalItem) return;
    setReserveMsg(null);
    try {
      const response = await axios.post(`/api/inventory/reservations/reserve`, null, {
        params: {
          sku: reserveModalItem.sku,
          quantity: reserveQty,
        },
      });
      setReserveMsg(`Successfully reserved ${reserveQty} units! Reservation ID: ${response.data.reservationId || 'CONFIRMED'}`);
      fetchInventory();
    } catch (err: any) {
      setReserveMsg(`Reservation failed: ${err.response?.data?.message || err.message}`);
    }
  };

  const columns = useMemo<ColumnDef<InventoryItem>[]>(
    () => [
      {
        accessorKey: 'sku',
        header: 'SKU Code',
        cell: (info) => (
          <span className="font-mono font-bold text-sky-400 text-xs px-2.5 py-1 rounded-md bg-sky-500/10 border border-sky-500/20">
            {String(info.getValue())}
          </span>
        ),
      },
      {
        accessorKey: 'quantity',
        header: 'Stock Qty',
        cell: (info) => {
          const qty = Number(info.getValue());
          const threshold = Number(info.row.original.reorderThreshold);
          const isLow = qty <= threshold;
          return (
            <span className={`font-semibold text-sm ${isLow ? 'text-amber-400 font-bold' : 'text-slate-100'}`}>
              {qty.toLocaleString()}
            </span>
          );
        },
      },
      {
        accessorKey: 'reorderThreshold',
        header: 'Reorder Point',
        cell: (info) => <span className="text-slate-400 text-xs">{String(info.getValue())} units</span>,
      },
      {
        accessorKey: 'warehouseLocation',
        header: 'Warehouse Facility',
        cell: (info) => (
          <span className="text-xs px-2.5 py-1 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
            {String(info.getValue())}
          </span>
        ),
      },
      {
        id: 'status',
        header: 'Health Status',
        cell: (info) => {
          const item = info.row.original;
          const isLow = item.quantity <= item.reorderThreshold;
          return isLow ? (
            <span className="inline-flex items-center gap-1.5 text-[11px] font-bold text-amber-400 bg-amber-500/10 border border-amber-500/30 px-2.5 py-0.5 rounded-full">
              <AlertTriangle className="w-3 h-3" /> LOW STOCK
            </span>
          ) : (
            <span className="inline-flex items-center gap-1.5 text-[11px] font-bold text-emerald-400 bg-emerald-500/10 border border-emerald-500/30 px-2.5 py-0.5 rounded-full">
              HEALTHY
            </span>
          );
        },
      },
      {
        id: 'actions',
        header: 'Actions',
        cell: (info) => (
          <button
            onClick={() => {
              setReserveModalItem(info.row.original);
              setReserveQty(1);
              setReserveMsg(null);
            }}
            className="flex items-center gap-1.5 px-3 py-1 rounded-lg bg-sky-500/15 text-sky-400 hover:bg-sky-500/25 border border-sky-500/30 text-xs font-semibold transition-colors"
          >
            <ShoppingCart className="w-3.5 h-3.5" /> Reserve
          </button>
        ),
      },
    ],
    []
  );

  const table = useReactTable({
    data: filteredData,
    columns,
    state: { globalFilter },
    onGlobalFilterChange: setGlobalFilter,
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    initialState: { pagination: { pageSize: 10 } },
  });

  return (
    <div className="space-y-4">
      {/* Search & Filter Bar */}
      <div className="p-4 rounded-2xl bg-slate-900/80 border border-slate-800 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3 flex-1 min-w-[280px]">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
            <input
              type="text"
              value={globalFilter ?? ''}
              onChange={(e) => setGlobalFilter(e.target.value)}
              placeholder="Filter by SKU or attributes..."
              className="w-full pl-9 pr-4 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-100 focus:outline-none focus:border-sky-500"
            />
          </div>

          <select
            value={warehouseFilter}
            onChange={(e) => setWarehouseFilter(e.target.value)}
            className="px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-200 focus:outline-none"
          >
            <option value="ALL">All Warehouses</option>
            <option value="Warehouse-North">Warehouse-North</option>
            <option value="Warehouse-South">Warehouse-South</option>
            <option value="Warehouse-East">Warehouse-East</option>
            <option value="Warehouse-West">Warehouse-West</option>
            <option value="Central-Hub">Central-Hub</option>
          </select>

          <button
            onClick={() => setLowStockOnly(!lowStockOnly)}
            className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-semibold border transition-all ${
              lowStockOnly
                ? 'bg-amber-500/20 text-amber-300 border-amber-500/40'
                : 'bg-slate-950 text-slate-400 border-slate-800 hover:text-slate-200'
            }`}
          >
            <AlertTriangle className="w-3.5 h-3.5" />
            Low Stock Only
          </button>
        </div>

        <button
          onClick={fetchInventory}
          disabled={loading}
          className="flex items-center gap-2 px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-slate-300 hover:text-white text-xs font-medium"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Refresh Table
        </button>
      </div>

      {/* Main Data Table */}
      <div className="rounded-2xl bg-slate-900/80 border border-slate-800 overflow-hidden shadow-xl">
        <table className="w-full text-left border-collapse">
          <thead>
            {table.getHeaderGroups().map((headerGroup) => (
              <tr key={headerGroup.id} className="bg-slate-950/80 border-b border-slate-800 text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                {headerGroup.headers.map((header) => (
                  <th key={header.id} className="px-5 py-3.5">
                    {flexRender(header.column.columnDef.header, header.getContext())}
                  </th>
                ))}
              </tr>
            ))}
          </thead>
          <tbody className="divide-y divide-slate-800/60 text-xs text-slate-200">
            {table.getRowModel().rows.length > 0 ? (
              table.getRowModel().rows.map((row) => (
                <tr key={row.id} className="hover:bg-sky-500/5 transition-colors">
                  {row.getVisibleCells().map((cell) => (
                    <td key={cell.id} className="px-5 py-3">
                      {flexRender(cell.column.columnDef.cell, cell.getContext())}
                    </td>
                  ))}
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={columns.length} className="px-5 py-8 text-center text-slate-500 text-xs">
                  No inventory records match the current filters.
                </td>
              </tr>
            )}
          </tbody>
        </table>

        {/* Table Pagination Controls */}
        <div className="px-5 py-3.5 bg-slate-950/60 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
          <span>
            Page <strong className="text-slate-200">{table.getState().pagination.pageIndex + 1}</strong> of{' '}
            <strong className="text-slate-200">{table.getPageCount()}</strong> ({table.getFilteredRowModel().rows.length} records)
          </span>

          <div className="flex items-center gap-2">
            <button
              onClick={() => table.previousPage()}
              disabled={!table.getCanPreviousPage()}
              className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-300 disabled:opacity-40"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              onClick={() => table.nextPage()}
              disabled={!table.getCanNextPage()}
              className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-300 disabled:opacity-40"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Reservation Modal */}
      {reserveModalItem && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 backdrop-blur-sm p-4">
          <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-2xl relative">
            <h3 className="text-base font-bold text-slate-100 mb-1 flex items-center gap-2">
              <ShoppingCart className="w-4 h-4 text-sky-400" />
              Flash-Sale Stock Reservation
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Atomic unit-pool reservation (<code className="text-sky-400">FOR UPDATE SKIP LOCKED</code>)
            </p>

            <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 mb-4 text-xs space-y-1">
              <div>SKU: <strong className="text-sky-400">{reserveModalItem.sku}</strong></div>
              <div>Available: <strong className="text-slate-200">{reserveModalItem.quantity} units</strong></div>
              <div>Facility: <strong className="text-slate-200">{reserveModalItem.warehouseLocation}</strong></div>
            </div>

            {reserveMsg && (
              <div className="mb-4 p-3 rounded-xl bg-sky-500/10 border border-sky-500/30 text-sky-300 text-xs">
                {reserveMsg}
              </div>
            )}

            <div className="mb-4">
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">Reservation Quantity</label>
              <input
                type="number"
                min={1}
                max={50}
                value={reserveQty}
                onChange={(e) => setReserveQty(parseInt(e.target.value) || 1)}
                className="w-full px-4 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-slate-100 text-sm focus:outline-none focus:border-sky-500"
              />
            </div>

            <div className="flex items-center justify-end gap-3">
              <button
                onClick={() => setReserveModalItem(null)}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold"
              >
                Close
              </button>
              <button
                onClick={handleReserve}
                className="px-4 py-2 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white text-xs font-bold shadow-lg shadow-sky-500/20"
              >
                Confirm Reservation
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
