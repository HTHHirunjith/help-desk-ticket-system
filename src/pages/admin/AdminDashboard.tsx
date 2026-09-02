import { useEffect, useState } from 'react';
import { ticketService, agentService } from '@/services';
import type { Ticket, Agent } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { PageHeader } from '@/components/layout/PageHeader';
import { StatCard, StatGrid } from '@/components/dashboard/StatCard';
import { TicketList } from '@/components/tickets/TicketList';
import { Card, CardHeader, InlineLoader } from '@/components/ui';
import { StatusDistribution, PriorityDistribution } from '@/components/dashboard/Distribution';
import { Inbox, Clock, AlertTriangle, CheckCircle, Headphones, Activity } from 'lucide-react';

export function AdminDashboard() {
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [agents, setAgents] = useState<Agent[]>([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({ total: 0, open: 0, inProgress: 0, resolved: 0, closed: 0, urgent: 0, high: 0, medium: 0, low: 0 });

  useEffect(() => {
    (async () => {
      setLoading(true);
      const [allTickets, allAgents, allStats] = await Promise.all([
        ticketService.getTickets(),
        agentService.getAgents(),
        ticketService.getStats(),
      ]);
      setTickets(allTickets);
      setAgents(allAgents);
      setStats(allStats);
      setLoading(false);
    })();
  }, []);

  if (loading) return <AppLayout><InlineLoader message="Loading admin dashboard..." /></AppLayout>;

  const recentTickets = tickets.slice(0, 6);

  return (
    <AppLayout>
      <PageHeader
        title="Admin Dashboard"
        description="System-wide overview of tickets, agents, and support operations."
      />

      <StatGrid columns={4}>
        <StatCard label="Total Tickets" value={stats.total} icon={Inbox} accent="slate" />
        <StatCard label="Open" value={stats.open} icon={Clock} accent="blue" />
        <StatCard label="In Progress" value={stats.inProgress} icon={AlertTriangle} accent="amber" />
        <StatCard label="Resolved" value={stats.resolved} icon={CheckCircle} accent="emerald" />
      </StatGrid>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">
        <div className="lg:col-span-2">
          <Card padding={false}>
            <div className="p-5 pb-0">
              <CardHeader title="Recent Tickets" subtitle="Latest activity across all tickets" icon={<Activity size={16} />} />
            </div>
            <TicketList tickets={recentTickets} linkPrefix="/admin/tickets" />
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader title="Status Distribution" subtitle="All tickets by status" />
            <StatusDistribution counts={stats} total={stats.total} />
          </Card>
          <Card>
            <CardHeader title="Priority Overview" subtitle="All tickets by priority" />
            <PriorityDistribution counts={stats} total={stats.total} />
          </Card>
        </div>
      </div>

      <div className="mt-6">
        <Card>
          <CardHeader title="Agent Workload" subtitle="Current ticket assignments and agent status" icon={<Headphones size={16} />} />
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-slate-200">
                  <th className="text-left text-xs font-semibold text-slate-500 uppercase tracking-wider px-4 py-3">Agent</th>
                  <th className="text-left text-xs font-semibold text-slate-500 uppercase tracking-wider px-4 py-3 hidden sm:table-cell">Email</th>
                  <th className="text-left text-xs font-semibold text-slate-500 uppercase tracking-wider px-4 py-3">Active Tickets</th>
                  <th className="text-left text-xs font-semibold text-slate-500 uppercase tracking-wider px-4 py-3 hidden md:table-cell">Resolved</th>
                  <th className="text-left text-xs font-semibold text-slate-500 uppercase tracking-wider px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody>
                {agents.map((agent) => (
                  <tr key={agent.id} className="border-b border-slate-100 last:border-0">
                    <td className="px-4 py-3">
                      <span className="text-sm font-medium text-slate-900">{agent.firstName} {agent.lastName}</span>
                    </td>
                    <td className="px-4 py-3 hidden sm:table-cell">
                      <span className="text-sm text-slate-500">{agent.email}</span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm font-medium text-slate-900">{agent.activeTickets}</span>
                    </td>
                    <td className="px-4 py-3 hidden md:table-cell">
                      <span className="text-sm text-slate-600">{agent.resolvedTickets}</span>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium border ${
                        agent.status === 'AVAILABLE' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' :
                        agent.status === 'BUSY' ? 'bg-amber-50 text-amber-700 border-amber-200' :
                        'bg-slate-100 text-slate-500 border-slate-200'
                      }`}>
                        {agent.status.charAt(0) + agent.status.slice(1).toLowerCase()}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      </div>
    </AppLayout>
  );
}
