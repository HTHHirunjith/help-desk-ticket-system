import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { ticketApi } from '@/api/tickets';
import { extractErrorMessage } from '@/api/auth';
import type { TicketSummary } from '@/types';
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
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Derive status distribution from loaded recent tickets or page
  const [stats, setStats] = useState({ total: 0, open: 0, inProgress: 0, resolved: 0, closed: 0 });

  const fetchDashboardData = async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      // Load recent tickets (backend scopes to USER's owned tickets)
      const response = await ticketApi.getTickets({ page: 0, size: 20 });
      setTickets(response.content);
      setTotalElements(response.totalElements);

      const openCount = response.content.filter((t) => t.status === 'OPEN').length;
      const inProgCount = response.content.filter((t) => t.status === 'IN_PROGRESS').length;
      const resolvedCount = response.content.filter((t) => t.status === 'RESOLVED').length;
      const closedCount = response.content.filter((t) => t.status === 'CLOSED').length;

      setStats({
        total: response.totalElements,
        open: openCount,
        inProgress: inProgCount,
        resolved: resolvedCount,
        closed: closedCount,
      });
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
        <StatCard label="Total Tickets" value={totalElements} icon={Inbox} accent="slate" />
        <StatCard label="Open" value={stats.open} icon={Clock} accent="blue" />
        <StatCard label="In Progress" value={stats.inProgress} icon={AlertTriangle} accent="amber" />
        <StatCard label="Resolved" value={stats.resolved} icon={CheckCircle} accent="emerald" />
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
          <StatusDistribution counts={stats} total={stats.total} />
        </Card>
      </div>
    </AppLayout>
  );
}
