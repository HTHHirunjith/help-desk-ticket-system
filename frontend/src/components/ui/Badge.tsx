import type { TicketStatus, TicketPriority, UserRole } from '@/types';

const statusConfig: Record<TicketStatus, { label: string; className: string; dot: string }> = {
  OPEN: {
    label: 'Open',
    className: 'bg-blue-50 text-blue-700 border-blue-200',
    dot: 'bg-blue-500',
  },
  IN_PROGRESS: {
    label: 'In Progress',
    className: 'bg-amber-50 text-amber-700 border-amber-200',
    dot: 'bg-amber-500',
  },
  RESOLVED: {
    label: 'Resolved',
    className: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    dot: 'bg-emerald-500',
  },
  CLOSED: {
    label: 'Closed',
    className: 'bg-slate-100 text-slate-600 border-slate-200',
    dot: 'bg-slate-400',
  },
};

const priorityConfig: Record<TicketPriority, { label: string; className: string }> = {
  LOW: { label: 'Low', className: 'bg-slate-50 text-slate-600 border-slate-200' },
  MEDIUM: { label: 'Medium', className: 'bg-blue-50 text-blue-700 border-blue-200' },
  HIGH: { label: 'High', className: 'bg-orange-50 text-orange-700 border-orange-200' },
  URGENT: { label: 'Urgent', className: 'bg-red-50 text-red-700 border-red-200' },
};

const roleConfig: Record<UserRole, { label: string; className: string }> = {
  USER: { label: 'User', className: 'bg-slate-100 text-slate-700 border-slate-200' },
  SUPPORT_AGENT: { label: 'Agent', className: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  ADMIN: { label: 'Admin', className: 'bg-slate-900 text-white border-slate-900' },
};

export function StatusBadge({ status, size = 'sm' }: { status: TicketStatus; size?: 'sm' | 'md' }) {
  const config = statusConfig[status];
  return (
    <span
      className={[
        'inline-flex items-center gap-1.5 rounded-full border font-medium',
        size === 'sm' ? 'px-2.5 py-0.5 text-xs' : 'px-3 py-1 text-sm',
        config.className,
      ].join(' ')}
    >
      <span className={`w-1.5 h-1.5 rounded-full ${config.dot}`} />
      {config.label}
    </span>
  );
}

export function PriorityBadge({ priority, size = 'sm' }: { priority: TicketPriority; size?: 'sm' | 'md' }) {
  const config = priorityConfig[priority];
  return (
    <span
      className={[
        'inline-flex items-center rounded-full border font-medium',
        size === 'sm' ? 'px-2.5 py-0.5 text-xs' : 'px-3 py-1 text-sm',
        config.className,
      ].join(' ')}
    >
      {config.label}
    </span>
  );
}

export function RoleBadge({ role }: { role: UserRole }) {
  const config = roleConfig[role];
  return (
    <span
      className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium ${config.className}`}
    >
      {config.label}
    </span>
  );
}

export function CategoryBadge({ category }: { category: string }) {
  return (
    <span className="inline-flex items-center rounded-full bg-slate-50 border border-slate-200 px-2.5 py-0.5 text-xs font-medium text-slate-600">
      {category.replace(/_/g, ' ')}
    </span>
  );
}
