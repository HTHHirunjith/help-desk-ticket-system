import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { ticketApi } from '@/api/tickets';
import { dashboardApi } from '@/api/dashboard';
import { extractErrorMessage } from '@/api/auth';
import type { TicketSummary, UserDashboardStats } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { PageHeader } from '@/components/layout/PageHeader';
import { StatCard, StatGrid } from '@/components/dashboard/StatCard';
import { TicketList } from '@/components/tickets/TicketList';
import { Card, CardHeader, Button, EmptyState, InlineLoader, ErrorState } from '@/components/ui';
import { StatusDistribution } from '@/components/dashboard/Distribution';
import { PlusCircle, Inbox, Clock, CheckCircle, AlertTriangle } from 'lucide-react';

export function UserDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [tickets, setTickets] = useState<TicketSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [stats, setStats] = useState<UserDashboardStats>({
    totalTickets: 0,
    openTickets: 0,
    inProgressTickets: 0,
    resolvedTickets: 0,
    closedTickets: 0,
  });

  const fetchDashboardData = async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const [statsData, ticketsResponse] = await Promise.all([
        dashboardApi.getUserDashboard(),
        ticketApi.getTickets({ page: 0, size: 5 }),
      ]);
      setStats(statsData);
      setTickets(ticketsResponse.content);
    } catch (err) {
      setError(extractErrorMessage(err, 'Failed to load dashboard data.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  if (!user) return null;

  if (loading) {
    return (
      <AppLayout>
        <InlineLoader message="Loading your dashboard..." />
      </AppLayout>
    );
  }

  if (error) {
    return (
      <AppLayout>
        <ErrorState message={error} onRetry={fetchDashboardData} />
      </AppLayout>
    );
  }

  const recentTickets = tickets.slice(0, 5);

  return (
    <AppLayout>
      <PageHeader
        title={`Welcome, ${user.firstName}`}
        description="Here is an overview of your support tickets."
        action={
          <Button leftIcon={<PlusCircle size={18} />} onClick={() => navigate('/create-ticket')}>
            New Ticket
          </Button>
        }
      />

      <StatGrid columns={4}>
        <StatCard label="Total Tickets" value={stats.totalTickets} icon={Inbox} accent="slate" />
        <StatCard label="Open" value={stats.openTickets} icon={Clock} accent="blue" />
        <StatCard label="In Progress" value={stats.inProgressTickets} icon={AlertTriangle} accent="amber" />
        <StatCard label="Resolved" value={stats.resolvedTickets} icon={CheckCircle} accent="emerald" />
      </StatGrid>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">
        <div className="lg:col-span-2">
          <Card padding={false}>
            <div className="p-5 pb-0">
              <CardHeader title="Recent Tickets" subtitle="Your most recently updated tickets" />
            </div>
            {recentTickets.length === 0 ? (
              <EmptyState
                icon={<Inbox size={24} />}
                title="No tickets yet"
                description="You have not submitted any support tickets. Create your first ticket to get started."
                action={
                  <Button leftIcon={<PlusCircle size={18} />} onClick={() => navigate('/create-ticket')}>
                    Create Ticket
                  </Button>
                }
              />
            ) : (
              <TicketList tickets={recentTickets} linkPrefix="/tickets" />
            )}
          </Card>
        </div>

        <Card>
          <CardHeader title="Ticket Status" subtitle="Distribution of your tickets" />
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
      </div>
    </AppLayout>
  );
}
