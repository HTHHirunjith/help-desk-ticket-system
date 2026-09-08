import { useEffect, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { ticketApi } from '@/api/tickets';
import { extractErrorMessage } from '@/api/auth';
import type { TicketSummary } from '@/types';
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
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [stats, setStats] = useState({ assigned: 0, open: 0, inProgress: 0, resolved: 0, closed: 0 });

  const fetchAgentDashboard = async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      // Backend automatically scopes to assigned tickets for SUPPORT_AGENT
      const response = await ticketApi.getTickets({ page: 0, size: 20 });
      setTickets(response.content);
      setTotalElements(response.totalElements);

      const openCount = response.content.filter((t) => t.status === 'OPEN').length;
      const inProgCount = response.content.filter((t) => t.status === 'IN_PROGRESS').length;
      const resolvedCount = response.content.filter((t) => t.status === 'RESOLVED').length;
      const closedCount = response.content.filter((t) => t.status === 'CLOSED').length;

      setStats({
        assigned: response.totalElements,
        open: openCount,
        inProgress: inProgCount,
        resolved: resolvedCount,
        closed: closedCount,
      });
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
        <StatCard label="Assigned to You" value={totalElements} icon={Headphones} accent="indigo" />
        <StatCard label="Open" value={stats.open} icon={Clock} accent="blue" />
        <StatCard label="In Progress" value={stats.inProgress} icon={AlertTriangle} accent="amber" />
        <StatCard label="Resolved" value={stats.resolved} icon={CheckCircle} accent="emerald" />
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
            <StatusDistribution counts={stats} total={stats.assigned} />
          </Card>
        </div>
      </div>
    </AppLayout>
  );
}
