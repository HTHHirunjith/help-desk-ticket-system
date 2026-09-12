import { useEffect, useState } from 'react';
import { ticketApi } from '@/api/tickets';
import { dashboardApi } from '@/api/dashboard';
import { extractErrorMessage } from '@/api/auth';
import type { TicketSummary, AdminDashboardStats } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { PageHeader } from '@/components/layout/PageHeader';
import { StatCard } from '@/components/dashboard/StatCard';
import { TicketList } from '@/components/tickets/TicketList';
import { Card, CardHeader, InlineLoader, ErrorState } from '@/components/ui';
import { StatusDistribution, PriorityDistribution, CategoryDistribution } from '@/components/dashboard/Distribution';
import { Inbox, Clock, AlertTriangle, CheckCircle, Activity, HelpCircle } from 'lucide-react';

export function AdminDashboard() {
  const [tickets, setTickets] = useState<TicketSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [stats, setStats] = useState<AdminDashboardStats>({
    totalTickets: 0,
    openTickets: 0,
    inProgressTickets: 0,
    resolvedTickets: 0,
    closedTickets: 0,
    unassignedTickets: 0,
    priorityDistribution: {
      LOW: 0,
      MEDIUM: 0,
      HIGH: 0,
      URGENT: 0,
    },
    categoryDistribution: [],
  });

  const fetchAdminDashboard = async () => {
    setLoading(true);
    setError(null);
    try {
      const [statsData, ticketsResponse] = await Promise.all([
        dashboardApi.getAdminDashboard(),
        ticketApi.getTickets({ page: 0, size: 6 }),
      ]);
      setStats(statsData);
      setTickets(ticketsResponse.content);
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

      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
        <StatCard label="Total Tickets" value={stats.totalTickets} icon={Inbox} accent="slate" />
        <StatCard label="Open" value={stats.openTickets} icon={Clock} accent="blue" />
        <StatCard label="In Progress" value={stats.inProgressTickets} icon={AlertTriangle} accent="amber" />
        <StatCard label="Resolved" value={stats.resolvedTickets} icon={CheckCircle} accent="emerald" />
        <StatCard label="Unassigned" value={stats.unassignedTickets} icon={HelpCircle} accent="red" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">
        <div className="lg:col-span-2 space-y-6">
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

          <Card>
            <CardHeader title="Category Breakdown" subtitle="Ticket distribution across categories" />
            <CategoryDistribution categories={stats.categoryDistribution} total={stats.totalTickets} />
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader title="Status Distribution" subtitle="Tickets by status" />
            <StatusDistribution
              counts={{
                open: stats.openTickets,
                inProgress: stats.inProgressTickets,
                resolved: stats.resolvedTickets,
                closed: stats.closedTickets,
              }}
              total={stats.totalTickets}
            />
          </Card>
          <Card>
            <CardHeader title="Priority Overview" subtitle="Tickets by priority" />
            <PriorityDistribution
              counts={{
                urgent: stats.priorityDistribution.URGENT || 0,
                high: stats.priorityDistribution.HIGH || 0,
                medium: stats.priorityDistribution.MEDIUM || 0,
                low: stats.priorityDistribution.LOW || 0,
              }}
              total={stats.totalTickets}
            />
          </Card>
        </div>
      </div>
    </AppLayout>
  );
}
