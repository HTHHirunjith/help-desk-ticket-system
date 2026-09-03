import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { ticketService } from '@/services';
import type { Ticket } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { PageHeader } from '@/components/layout/PageHeader';
import { TicketList } from '@/components/tickets/TicketList';
import { Card, EmptyState, InlineLoader, Button } from '@/components/ui';
import { PlusCircle, Inbox } from 'lucide-react';

export function MyTicketsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    (async () => {
      setLoading(true);
      const userTickets = await ticketService.getTicketsByRequester(user.id);
      setTickets(userTickets);
      setLoading(false);
    })();
  }, [user]);

  if (!user) return null;

  return (
    <AppLayout>
      <PageHeader
        title="My Tickets"
        description="All support tickets you have submitted."
        action={
          <Button leftIcon={<PlusCircle size={18} />} onClick={() => navigate('/create-ticket')}>
            New Ticket
          </Button>
        }
      />

      {loading ? (
        <InlineLoader message="Loading tickets..." />
      ) : tickets.length === 0 ? (
        <Card>
          <EmptyState
            icon={<Inbox size={24} />}
            title="No tickets yet"
            description="You have not submitted any support tickets. Create your first ticket to get started."
            action={<Button leftIcon={<PlusCircle size={18} />} onClick={() => navigate('/create-ticket')}>Create Ticket</Button>}
          />
        </Card>
      ) : (
        <TicketList tickets={tickets} linkPrefix="/tickets" />
      )}
    </AppLayout>
  );
}
