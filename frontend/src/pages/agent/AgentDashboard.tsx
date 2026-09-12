import { useEffect, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { ticketApi } from '@/api/tickets';
import { dashboardApi } from '@/api/dashboard';
import { extractErrorMessage } from '@/api/auth';
import type { TicketSummary, AgentDashboardStats } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { PageHeader } from '@/components/layout/PageHeader';
import { StatCard, StatGrid } from '@/components/dashboard/StatCard';
import { TicketList } from '@/components/tickets/TicketList';
import { Card, CardHeader, EmptyState, InlineLoader, ErrorState } from '@/components/ui';
import { StatusDistribution } from '@/components/dashboard/Distribution';
import { Inbox, Clock, AlertTriangle, CheckCircle, Headphones } from 'lucide-react';

export function AgentDashboard() {
  const { user } = useAuth();
  const [tickets, setTickets] = useState<TicketSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [stats, setStats] = useState<AgentDashboardStats>({
    assignedTickets: 0,
    openTickets: 0,
    inProgressTickets: 0,
    resolvedTickets: 0,
    closedTickets: 0,
  });

  const fetchAgentDashboard = async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const [statsData, ticketsResponse] = await Promise.all([
        dashboardApi.getAgentDashboard(),
        ticketApi.getTickets({ page: 0, size: 5 }),
      ]);
      setStats(statsData);
      setTickets(ticketsResponse.content);
    } catch (err) {
      setError(extractErrorMessage(err, 'Failed to load agent dashboard.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAgentDashboard();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  if (!user) return null;

  if (loading) {
    return (
      <AppLayout>
        <InlineLoader message="Loading your agent dashboard..." />
      </AppLayout>
    );
  }

  if (error) {
    return (
      <AppLayout>
        <ErrorState message={error} onRetry={fetchAgentDashboard} />
      </AppLayout>
    );
  }

  const recentTickets = tickets.slice(0, 5);

  return (
    <AppLayout>
      <PageHeader
        title={`Agent Workspace — ${user.firstName}`}
        description="Tickets assigned to you that need attention."
      />

      <StatGrid columns={4}>
        <StatCard label="Assigned to You" value={stats.assignedTickets} icon={Headphones} accent="indigo" />
        <StatCard label="Open" value={stats.openTickets} icon={Clock} accent="blue" />
        <StatCard label="In Progress" value={stats.inProgressTickets} icon={AlertTriangle} accent="amber" />
        <StatCard label="Resolved" value={stats.resolvedTickets} icon={CheckCircle} accent="emerald" />
      </StatGrid>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">
        <div className="lg:col-span-2">
          <Card padding={false}>
            <div className="p-5 pb-0">
              <CardHeader
                title="Recent Assigned Tickets"
                subtitle="Most recently updated tickets assigned to you"
              />
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
            <StatusDistribution
              counts={{
                open: stats.openTickets,
                inProgress: stats.inProgressTickets,
                resolved: stats.resolvedTickets,
                closed: stats.closedTickets,
              }}
              total={stats.assignedTickets}
            />
          </Card>
        </div>
      </div>
    </AppLayout>
  );
}
