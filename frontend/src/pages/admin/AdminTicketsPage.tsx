import { useEffect, useState } from 'react';
import { ticketService } from '@/services';
import type { Ticket } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { PageHeader } from '@/components/layout/PageHeader';
import { TicketTable } from '@/components/tickets/TicketList';
import { Card, EmptyState, InlineLoader } from '@/components/ui';
import { Inbox } from 'lucide-react';

export function AdminTicketsPage() {
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      setLoading(true);
      const allTickets = await ticketService.getTickets();
      setTickets(allTickets);
      setLoading(false);
    })();
  }, []);

  return (
    <AppLayout>
      <PageHeader
        title="All Tickets"
        description="Manage all support tickets across the system."
      />

      {loading ? (
        <InlineLoader message="Loading tickets..." />
      ) : tickets.length === 0 ? (
        <Card>
          <EmptyState icon={<Inbox size={24} />} title="No tickets" description="There are no support tickets in the system." />
        </Card>
      ) : (
        <TicketTable tickets={tickets} linkPrefix="/admin/tickets" />
      )}
    </AppLayout>
  );
}
