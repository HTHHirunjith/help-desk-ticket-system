import type { LucideIcon } from 'lucide-react';
import type { ReactNode } from 'react';

interface StatCardProps {
  label: string;
  value: string | number;
  icon: LucideIcon;
  accent?: 'blue' | 'amber' | 'emerald' | 'slate' | 'red' | 'indigo';
  sublabel?: string;
}

const accentClasses: Record<NonNullable<StatCardProps['accent']>, { bg: string; text: string }> = {
  blue: { bg: 'bg-blue-50', text: 'text-blue-600' },
  amber: { bg: 'bg-amber-50', text: 'text-amber-600' },
  emerald: { bg: 'bg-emerald-50', text: 'text-emerald-600' },
  slate: { bg: 'bg-slate-100', text: 'text-slate-600' },
  red: { bg: 'bg-red-50', text: 'text-red-600' },
  indigo: { bg: 'bg-indigo-50', text: 'text-indigo-600' },
};

export function StatCard({ label, value, icon: Icon, accent = 'slate', sublabel }: StatCardProps) {
  const a = accentClasses[accent];
  return (
    <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-5">
      <div className="flex items-center justify-between mb-3">
        <span className="text-sm font-medium text-slate-500">{label}</span>
        <div className={`flex items-center justify-center w-9 h-9 rounded-lg ${a.bg} ${a.text}`}>
          <Icon size={18} />
        </div>
      </div>
      <p className="text-2xl font-bold text-slate-900">{value}</p>
      {sublabel && <p className="text-xs text-slate-400 mt-1">{sublabel}</p>}
    </div>
  );
}

interface StatGridProps {
  children: ReactNode;
  columns?: 2 | 3 | 4;
}

export function StatGrid({ children, columns = 4 }: StatGridProps) {
  const colClass = {
    2: 'grid-cols-2',
    3: 'grid-cols-1 sm:grid-cols-3',
    4: 'grid-cols-2 lg:grid-cols-4',
  };
  return <div className={`grid ${colClass[columns]} gap-4`}>{children}</div>;
}
