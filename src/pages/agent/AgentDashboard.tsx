import { useEffect, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { ticketService } from '@/services';
import type { Ticket } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { PageHeader } from '@/components/layout/PageHeader';
import { StatCard, StatGrid } from '@/components/dashboard/StatCard';
import { TicketList } from '@/components/tickets/TicketList';
import { Card, CardHeader, EmptyState, InlineLoader } from '@/components/ui';
import { StatusDistribution } from '@/components/dashboard/Distribution';
import { Inbox, Clock, AlertTriangle, CheckCircle, Headphones } from 'lucide-react';

export function AgentDashboard() {
  const { user } = useAuth();
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({ assigned: 0, open: 0, inProgress: 0, resolved: 0, closed: 0 });

  useEffect(() => {
    if (!user) return;
    (async () => {
      setLoading(true);
      const [agentTickets, agentStats] = await Promise.all([
        ticketService.getTicketsByAgent(user.id),
        ticketService.getAgentStats(user.id),
      ]);
      setTickets(agentTickets);
      setStats(agentStats);
      setLoading(false);
    })();
  }, [user]);

  if (!user) return null;
  if (loading) return <AppLayout><InlineLoader message="Loading your agent dashboard..." /></AppLayout>;

  const recentTickets = tickets.slice(0, 5);

  return (
    <AppLayout>
      <PageHeader
        title={`Agent Workspace — ${user.firstName}`}
        description="Tickets assigned to you that need attention."
      />

      <StatGrid columns={4}>
        <StatCard label="Assigned to You" value={stats.assigned} icon={Headphones} accent="indigo" />
        <StatCard label="Open" value={stats.open} icon={Clock} accent="blue" />
        <StatCard label="In Progress" value={stats.inProgress} icon={AlertTriangle} accent="amber" />
        <StatCard label="Resolved" value={stats.resolved} icon={CheckCircle} accent="emerald" />
      </StatGrid>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">
        <div className="lg:col-span-2">
          <Card padding={false}>
            <div className="p-5 pb-0">
              <CardHeader title="Recent Assigned Tickets" subtitle="Most recently updated tickets assigned to you" />
            </div>
            {recentTickets.length === 0 ? (
              <EmptyState
                icon={<Inbox size={24} />}
                title="No assigned tickets"
                description="You have no tickets assigned to you right now. New assignments will appear here."
              />
            ) : (
              <TicketList tickets={recentTickets} linkPrefix="/agent/tickets" />
            )}
          </Card>
        </div>

        <div>
          <Card>
            <CardHeader title="Your Ticket Status" subtitle="Distribution of your assigned tickets" />
            <StatusDistribution counts={stats} total={stats.assigned} />
          </Card>
        </div>
      </div>
    </AppLayout>
  );
}
