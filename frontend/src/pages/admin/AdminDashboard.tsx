import { useEffect, useState } from 'react';
import { ticketApi } from '@/api/tickets';
import { extractErrorMessage } from '@/api/auth';
import type { TicketSummary } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { PageHeader } from '@/components/layout/PageHeader';
import { StatCard, StatGrid } from '@/components/dashboard/StatCard';
import { TicketList } from '@/components/tickets/TicketList';
import { Card, CardHeader, InlineLoader, ErrorState } from '@/components/ui';
import { StatusDistribution, PriorityDistribution } from '@/components/dashboard/Distribution';
import { Inbox, Clock, AlertTriangle, CheckCircle, Activity } from 'lucide-react';

export function AdminDashboard() {
  const [tickets, setTickets] = useState<TicketSummary[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [stats, setStats] = useState({
    total: 0,
    open: 0,
    inProgress: 0,
    resolved: 0,
    closed: 0,
    urgent: 0,
    high: 0,
    medium: 0,
    low: 0,
  });

  const fetchAdminDashboard = async () => {
    setLoading(true);
    setError(null);
    try {
      // Backend returns all tickets for ADMIN
      const response = await ticketApi.getTickets({ page: 0, size: 50 });
      setTickets(response.content);
      setTotalElements(response.totalElements);

      const openCount = response.content.filter((t) => t.status === 'OPEN').length;
      const inProgCount = response.content.filter((t) => t.status === 'IN_PROGRESS').length;
      const resolvedCount = response.content.filter((t) => t.status === 'RESOLVED').length;
      const closedCount = response.content.filter((t) => t.status === 'CLOSED').length;

      const urgentCount = response.content.filter((t) => t.priority === 'URGENT').length;
      const highCount = response.content.filter((t) => t.priority === 'HIGH').length;
      const mediumCount = response.content.filter((t) => t.priority === 'MEDIUM').length;
      const lowCount = response.content.filter((t) => t.priority === 'LOW').length;

      setStats({
        total: response.totalElements,
        open: openCount,
        inProgress: inProgCount,
        resolved: resolvedCount,
        closed: closedCount,
        urgent: urgentCount,
        high: highCount,
        medium: mediumCount,
        low: lowCount,
      });
    } catch (err) {
      setError(extractErrorMessage(err, 'Failed to load admin dashboard.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAdminDashboard();
  }, []);

  if (loading) {
    return (
      <AppLayout>
        <InlineLoader message="Loading admin dashboard..." />
      </AppLayout>
    );
  }

  if (error) {
    return (
      <AppLayout>
        <ErrorState message={error} onRetry={fetchAdminDashboard} />
      </AppLayout>
    );
  }

  const recentTickets = tickets.slice(0, 6);

  return (
    <AppLayout>
      <PageHeader
        title="Admin Dashboard"
        description="System-wide overview of tickets, agents, and support operations."
      />

      <StatGrid columns={4}>
        <StatCard label="Total Tickets" value={totalElements} icon={Inbox} accent="slate" />
        <StatCard label="Open" value={stats.open} icon={Clock} accent="blue" />
        <StatCard label="In Progress" value={stats.inProgress} icon={AlertTriangle} accent="amber" />
        <StatCard label="Resolved" value={stats.resolved} icon={CheckCircle} accent="emerald" />
      </StatGrid>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">
        <div className="lg:col-span-2">
          <Card padding={false}>
            <div className="p-5 pb-0">
              <CardHeader
                title="Recent Tickets"
                subtitle="Latest activity across all tickets"
                icon={<Activity size={16} />}
              />
            </div>
            <TicketList tickets={recentTickets} linkPrefix="/admin/tickets" />
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader title="Status Distribution" subtitle="Tickets by status" />
            <StatusDistribution counts={stats} total={stats.total} />
          </Card>
          <Card>
            <CardHeader title="Priority Overview" subtitle="Tickets by priority" />
            <PriorityDistribution counts={stats} total={stats.total} />
          </Card>
        </div>
      </div>
    </AppLayout>
  );
}
