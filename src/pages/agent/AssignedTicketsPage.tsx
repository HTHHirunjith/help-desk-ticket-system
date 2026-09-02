import { useEffect, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { ticketService } from '@/services';
import type { Ticket } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { PageHeader } from '@/components/layout/PageHeader';
import { TicketList } from '@/components/tickets/TicketList';
import { Card, EmptyState, InlineLoader } from '@/components/ui';
import { Inbox } from 'lucide-react';

export function AssignedTicketsPage() {
  const { user } = useAuth();
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    (async () => {
      setLoading(true);
      const agentTickets = await ticketService.getTicketsByAgent(user.id);
      setTickets(agentTickets);
      setLoading(false);
    })();
  }, [user]);

  if (!user) return null;

  return (
    <AppLayout>
      <PageHeader
        title="Assigned Tickets"
        description="All support tickets currently assigned to you."
      />

      {loading ? (
        <InlineLoader message="Loading assigned tickets..." />
      ) : tickets.length === 0 ? (
        <Card>
          <EmptyState
            icon={<Inbox size={24} />}
            title="No assigned tickets"
            description="You have no tickets assigned to you right now. New assignments will appear here."
          />
        </Card>
      ) : (
        <TicketList tickets={tickets} linkPrefix="/agent/tickets" />
      )}
    </AppLayout>
  );
}
