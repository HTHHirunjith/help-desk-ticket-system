import { useState, type ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { navConfig, roleLabels } from '@/config/navigation';
import { RoleBadge } from '@/components/ui/Badge';
import { getInitials } from '@/utils/format';
import { LogOut, Menu, X, LifeBuoy } from 'lucide-react';
import type { UserRole } from '@/types';

interface AppLayoutProps {
  children: ReactNode;
  allowedRoles?: UserRole[];
}

export function AppLayout({ children }: AppLayoutProps) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  if (!user) return null;

  const navItems = navConfig[user.role];

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const sidebarContent = (
    <div className="flex flex-col h-full">
      <div className="flex items-center gap-2.5 px-5 h-16 border-b border-slate-200 shrink-0">
        <div className="flex items-center justify-center w-9 h-9 rounded-lg bg-blue-600 text-white">
          <LifeBuoy size={20} />
        </div>
        <div>
          <p className="text-sm font-bold text-slate-900 leading-none">HelpDesk</p>
          <p className="text-xs text-slate-500 mt-0.5">Support Portal</p>
        </div>
      </div>

      <nav className="flex-1 overflow-y-auto py-4 px-3 space-y-1">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.path}
              to={item.path}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) =>
                [
                  'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-blue-50 text-blue-700'
                    : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900',
                ].join(' ')
              }
            >
              <Icon size={18} />
              {item.label}
            </NavLink>
          );
        })}
      </nav>

      <div className="border-t border-slate-200 p-3 shrink-0">
        <div className="flex items-center gap-3 px-2 py-2 mb-2">
          <div className="flex items-center justify-center w-9 h-9 rounded-full bg-slate-200 text-slate-700 text-sm font-semibold">
            {getInitials(user.firstName, user.lastName)}
          </div>
          <div className="min-w-0 flex-1">
            <p className="text-sm font-medium text-slate-900 truncate">
              {user.firstName} {user.lastName}
            </p>
            <p className="text-xs text-slate-500 truncate">{user.email}</p>
          </div>
        </div>
        <div className="flex items-center justify-between px-2 mb-2">
          <RoleBadge role={user.role} />
          <span className="text-xs text-slate-400">{roleLabels[user.role]}</span>
        </div>
        <button
          onClick={handleLogout}
          className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition-colors"
        >
          <LogOut size={18} />
          Sign Out
        </button>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Desktop sidebar */}
      <aside className="hidden lg:flex fixed inset-y-0 left-0 w-64 bg-white border-r border-slate-200 flex-col z-30">
        {sidebarContent}
      </aside>

      {/* Mobile sidebar */}
      {sidebarOpen && (
        <div className="lg:hidden fixed inset-0 z-50">
          <div className="absolute inset-0 bg-slate-900/40" onClick={() => setSidebarOpen(false)} />
          <aside className="absolute inset-y-0 left-0 w-64 bg-white border-r border-slate-200 flex flex-col">
            <button
              onClick={() => setSidebarOpen(false)}
              className="absolute top-4 right-3 text-slate-400 hover:text-slate-600 p-1"
            >
              <X size={20} />
            </button>
            {sidebarContent}
          </aside>
        </div>
      )}

      {/* Main content */}
      <div className="lg:pl-64">
        {/* Mobile top bar */}
        <header className="lg:hidden sticky top-0 z-20 flex items-center justify-between h-16 px-4 bg-white border-b border-slate-200">
          <button
            onClick={() => setSidebarOpen(true)}
            className="text-slate-600 hover:text-slate-900 p-1.5 -ml-1.5 rounded-lg hover:bg-slate-100"
          >
            <Menu size={22} />
          </button>
          <div className="flex items-center gap-2">
            <div className="flex items-center justify-center w-7 h-7 rounded-md bg-blue-600 text-white">
              <LifeBuoy size={16} />
            </div>
            <span className="text-sm font-bold text-slate-900">HelpDesk</span>
          </div>
          <div className="flex items-center justify-center w-8 h-8 rounded-full bg-slate-200 text-slate-700 text-xs font-semibold">
            {getInitials(user.firstName, user.lastName)}
          </div>
        </header>

        <main className="p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto">{children}</main>
      </div>
    </div>
  );
}
