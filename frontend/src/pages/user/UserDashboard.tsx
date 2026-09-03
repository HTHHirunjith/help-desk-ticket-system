import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { ticketService } from '@/services';
import type { Ticket } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { PageHeader } from '@/components/layout/PageHeader';
import { StatCard, StatGrid } from '@/components/dashboard/StatCard';
import { TicketList } from '@/components/tickets/TicketList';
import { Card, CardHeader, Button, EmptyState, InlineLoader } from '@/components/ui';
import { StatusDistribution } from '@/components/dashboard/Distribution';
import { PlusCircle, Inbox, Clock, CheckCircle, AlertTriangle } from 'lucide-react';

export function UserDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({ total: 0, open: 0, inProgress: 0, resolved: 0, closed: 0 });

  useEffect(() => {
    if (!user) return;
    (async () => {
      setLoading(true);
      const [userTickets] = await Promise.all([ticketService.getTicketsByRequester(user.id), ticketService.getStats()]);
      setTickets(userTickets);
      setStats({ total: userTickets.length, open: userTickets.filter((t) => t.status === 'OPEN').length, inProgress: userTickets.filter((t) => t.status === 'IN_PROGRESS').length, resolved: userTickets.filter((t) => t.status === 'RESOLVED').length, closed: userTickets.filter((t) => t.status === 'CLOSED').length });
      setLoading(false);
    })();
  }, [user]);

  if (!user) return null;
  if (loading) return <AppLayout><InlineLoader message="Loading your dashboard..." /></AppLayout>;
  const recentTickets = tickets.slice(0, 5);

  return <AppLayout><PageHeader title={`Welcome, ${user.firstName}`} description="Here is an overview of your support tickets." action={<Button leftIcon={<PlusCircle size={18} />} onClick={() => navigate('/create-ticket')}>New Ticket</Button>} />
    <StatGrid columns={4}><StatCard label="Total Tickets" value={stats.total} icon={Inbox} accent="slate" /><StatCard label="Open" value={stats.open} icon={Clock} accent="blue" /><StatCard label="In Progress" value={stats.inProgress} icon={AlertTriangle} accent="amber" /><StatCard label="Resolved" value={stats.resolved} icon={CheckCircle} accent="emerald" /></StatGrid>
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6"><div className="lg:col-span-2"><Card padding={false}><div className="p-5 pb-0"><CardHeader title="Recent Tickets" subtitle="Your most recently updated tickets" /></div>{recentTickets.length === 0 ? <EmptyState icon={<Inbox size={24} />} title="No tickets yet" description="You have not submitted any support tickets. Create your first ticket to get started." action={<Button leftIcon={<PlusCircle size={18} />} onClick={() => navigate('/create-ticket')}>Create Ticket</Button>} /> : <TicketList tickets={recentTickets} linkPrefix="/tickets" />}</Card></div><Card><CardHeader title="Ticket Status" subtitle="Distribution of your tickets" /><StatusDistribution counts={stats} total={stats.total} /></Card></div>
  </AppLayout>;
}
