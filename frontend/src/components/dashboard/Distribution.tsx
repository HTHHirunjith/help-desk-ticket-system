import type { TicketStatus, TicketPriority } from '@/types';
import { PriorityBadge } from '@/components/ui/Badge';

interface DistributionBarProps {
  label: string;
  count: number;
  total: number;
  color: string;
}

export function DistributionBar({ label, count, total, color }: DistributionBarProps) {
  const pct = total > 0 ? Math.round((count / total) * 100) : 0;
  return (
    <div>
      <div className="flex items-center justify-between mb-1.5">
        <span className="text-sm text-slate-600">{label}</span>
        <span className="text-sm font-medium text-slate-900">{count}</span>
      </div>
      <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
        <div className={`h-full rounded-full ${color}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

interface StatusDistributionProps {
  counts: { open: number; inProgress: number; resolved: number; closed: number };
  total: number;
}

export function StatusDistribution({ counts, total }: StatusDistributionProps) {
  const items: { status: TicketStatus; count: number; color: string }[] = [
    { status: 'OPEN', count: counts.open, color: 'bg-blue-500' },
    { status: 'IN_PROGRESS', count: counts.inProgress, color: 'bg-amber-500' },
    { status: 'RESOLVED', count: counts.resolved, color: 'bg-emerald-500' },
    { status: 'CLOSED', count: counts.closed, color: 'bg-slate-400' },
  ];
  return (
    <div className="space-y-3">
      {items.map((item) => (
        <DistributionBar
          key={item.status}
          label={item.status === 'IN_PROGRESS' ? 'In Progress' : item.status.charAt(0) + item.status.slice(1).toLowerCase()}
          count={item.count}
          total={total}
          color={item.color}
        />
      ))}
    </div>
  );
}

interface PriorityDistributionProps {
  counts: { urgent: number; high: number; medium: number; low: number };
  total: number;
}

export function PriorityDistribution({ counts }: PriorityDistributionProps) {
  const items: { priority: TicketPriority; count: number }[] = [
    { priority: 'URGENT', count: counts.urgent },
    { priority: 'HIGH', count: counts.high },
    { priority: 'MEDIUM', count: counts.medium },
    { priority: 'LOW', count: counts.low },
  ];
  return (
    <div className="space-y-2.5">
      {items.map((item) => (
        <div key={item.priority} className="flex items-center justify-between">
          <PriorityBadge priority={item.priority} />
          <span className="text-sm font-medium text-slate-900">{item.count}</span>
        </div>
      ))}
    </div>
  );
}

interface CategoryDistributionProps {
  categories: { categoryId: string; categoryName: string; ticketCount: number }[];
  total: number;
}

export function CategoryDistribution({ categories, total }: CategoryDistributionProps) {
  if (categories.length === 0) {
    return <p className="text-sm text-slate-500">No categories found.</p>;
  }

  const colors = [
    'bg-blue-500',
    'bg-indigo-500',
    'bg-purple-500',
    'bg-emerald-500',
    'bg-amber-500',
    'bg-rose-500',
    'bg-cyan-500',
  ];

  return (
    <div className="space-y-3">
      {categories.map((cat, idx) => (
        <DistributionBar
          key={cat.categoryId}
          label={cat.categoryName}
          count={cat.ticketCount}
          total={total}
          color={colors[idx % colors.length]}
        />
      ))}
    </div>
  );
}
