import { useEffect, useState } from 'react';
import { agentService } from '@/services';
import type { Agent } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, InlineLoader, EmptyState } from '@/components/ui';
import { formatDate, getInitials } from '@/utils/format';
import { Inbox } from 'lucide-react';

export function AdminAgentsPage() {
  const [agents, setAgents] = useState<Agent[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      setLoading(true);
      const allAgents = await agentService.getAgents();
      setAgents(allAgents);
      setLoading(false);
    })();
  }, []);

  return (
    <AppLayout>
      <PageHeader
        title="Support Agents"
        description="Manage support agents and their workload."
      />

      {loading ? (
        <InlineLoader message="Loading agents..." />
      ) : agents.length === 0 ? (
        <Card>
          <EmptyState icon={<Inbox size={24} />} title="No agents" description="There are no support agents in the system." />
        </Card>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {agents.map((agent) => (
            <Card key={agent.id}>
              <div className="flex items-start gap-3 mb-4">
                <div className="flex items-center justify-center w-11 h-11 rounded-full bg-slate-200 text-slate-700 text-sm font-semibold shrink-0">
                  {getInitials(agent.firstName, agent.lastName)}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-semibold text-slate-900">{agent.firstName} {agent.lastName}</p>
                  <p className="text-xs text-slate-500 truncate">{agent.email}</p>
                </div>
                <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium border shrink-0 ${
                  agent.status === 'AVAILABLE' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' :
                  agent.status === 'BUSY' ? 'bg-amber-50 text-amber-700 border-amber-200' :
                  'bg-slate-100 text-slate-500 border-slate-200'
                }`}>
                  {agent.status.charAt(0) + agent.status.slice(1).toLowerCase()}
                </span>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="rounded-lg bg-slate-50 p-3">
                  <p className="text-xs text-slate-500 mb-1">Active Tickets</p>
                  <p className="text-lg font-bold text-slate-900">{agent.activeTickets}</p>
                </div>
                <div className="rounded-lg bg-slate-50 p-3">
                  <p className="text-xs text-slate-500 mb-1">Resolved</p>
                  <p className="text-lg font-bold text-slate-900">{agent.resolvedTickets}</p>
                </div>
              </div>

              <p className="text-xs text-slate-400 mt-3">Joined {formatDate(agent.createdAt)}</p>
            </Card>
          ))}
        </div>
      )}
    </AppLayout>
  );
}
