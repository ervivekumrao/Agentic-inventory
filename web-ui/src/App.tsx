import React, { useState } from 'react';
import { AuthProvider } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { InventoryDashboard } from './components/InventoryDashboard';
import { InventoryTable } from './components/InventoryTable';
import { AgenticWorkspace } from './components/AgenticWorkspace';
import { LoggingAdmin } from './components/LoggingAdmin';
import { LoginModal } from './components/LoginModal';

export const AppContent: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'dashboard' | 'inventory' | 'chat' | 'logging'>('dashboard');
  const [isLoginOpen, setIsLoginOpen] = useState(false);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        onOpenLogin={() => setIsLoginOpen(true)}
      />

      <main className="flex-1 p-6 max-w-7xl mx-auto w-full">
        {activeTab === 'dashboard' && <InventoryDashboard />}
        {activeTab === 'inventory' && <InventoryTable />}
        {activeTab === 'chat' && <AgenticWorkspace />}
        {activeTab === 'logging' && <LoggingAdmin />}
      </main>

      <LoginModal isOpen={isLoginOpen} onClose={() => setIsLoginOpen(false)} />
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
};

export default App;
