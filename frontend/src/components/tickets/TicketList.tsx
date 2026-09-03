import type { Ticket } from '@/types';
import { StatusBadge, PriorityBadge, CategoryBadge } from '@/components/ui/Badge';
import { timeAgo } from '@/utils/format';
import { Link } from 'react-router-dom';
import { MessageSquare } from 'lucide-react';

interface TicketRowProps { ticket: Ticket; linkTo: string }

export function TicketRow({ ticket, linkTo }: TicketRowProps) {
  return (
    <Link to={linkTo} className="flex items-center gap-4 px-4 py-3 hover:bg-slate-50 transition-colors border-b border-slate-100 last:border-0">
      <div className="hidden sm:block w-28 shrink-0"><span className="text-xs font-mono font-medium text-slate-500">{ticket.ticketNumber}</span></div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-slate-900 truncate">{ticket.title}</p>
        <p className="text-xs text-slate-500 mt-0.5">{ticket.requesterName}{ticket.assignedAgentName && ` · Assigned: ${ticket.assignedAgentName}`}</p>
      </div>
      <div className="hidden md:block shrink-0"><CategoryBadge category={ticket.category} /></div>
      <div className="hidden sm:block shrink-0"><PriorityBadge priority={ticket.priority} /></div>
      <div className="shrink-0"><StatusBadge status={ticket.status} /></div>
      <div className="hidden lg:flex items-center gap-1 shrink-0 w-20 justify-end text-xs text-slate-400">{ticket.comments.length > 0 && <MessageSquare size={13} />}<span>{timeAgo(ticket.updatedAt)}</span></div>
    </Link>
  );
}

export function TicketList({ tickets, linkPrefix }: { tickets: Ticket[]; linkPrefix: string }) {
  return <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">{tickets.map((ticket) => <TicketRow key={ticket.id} ticket={ticket} linkTo={`${linkPrefix}/${ticket.id}`} />)}</div>;
}

export function TicketTable({ tickets, linkPrefix }: { tickets: Ticket[]; linkPrefix: string }) {
  return (
    <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden overflow-x-auto">
      <table className="w-full"><thead><tr className="border-b border-slate-200 bg-slate-50">
        {['ID', 'Title', 'Requester', 'Category', 'Priority', 'Status', 'Updated'].map((label, index) => <th key={label} className={`text-left text-xs font-semibold text-slate-500 uppercase tracking-wider px-4 py-3 ${index === 2 ? 'hidden md:table-cell' : ''} ${index === 3 || index === 6 ? 'hidden lg:table-cell' : ''}`}>{label}</th>)}
      </tr></thead><tbody>
        {tickets.map((ticket) => <tr key={ticket.id} className="border-b border-slate-100 last:border-0 hover:bg-slate-50 transition-colors">
          <td className="px-4 py-3"><Link to={`${linkPrefix}/${ticket.id}`} className="text-xs font-mono font-medium text-slate-500 hover:text-blue-600">{ticket.ticketNumber}</Link></td>
          <td className="px-4 py-3"><Link to={`${linkPrefix}/${ticket.id}`} className="text-sm font-medium text-slate-900 hover:text-blue-600">{ticket.title}</Link></td>
          <td className="px-4 py-3 hidden md:table-cell"><span className="text-sm text-slate-600">{ticket.requesterName}</span></td>
          <td className="px-4 py-3 hidden lg:table-cell"><CategoryBadge category={ticket.category} /></td>
          <td className="px-4 py-3"><PriorityBadge priority={ticket.priority} /></td>
          <td className="px-4 py-3"><StatusBadge status={ticket.status} /></td>
          <td className="px-4 py-3 hidden lg:table-cell"><span className="text-xs text-slate-400">{timeAgo(ticket.updatedAt)}</span></td>
        </tr>)}
      </tbody></table>
    </div>
  );
}
