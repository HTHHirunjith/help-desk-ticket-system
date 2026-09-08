import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { ticketApi, categoryApi } from '@/api/tickets';
import { extractErrorMessage } from '@/api/auth';
import type { TicketSummary, Category, TicketStatus, TicketPriority } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { PageHeader } from '@/components/layout/PageHeader';
import { TicketList } from '@/components/tickets/TicketList';
import { Card, EmptyState, InlineLoader, Button, ErrorState } from '@/components/ui';
import { PlusCircle, Inbox, Filter } from 'lucide-react';

const statuses: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
const priorities: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

export function MyTicketsPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [tickets, setTickets] = useState<TicketSummary[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters and pagination
  const [selectedStatus, setSelectedStatus] = useState<TicketStatus | ''>('');
  const [selectedPriority, setSelectedPriority] = useState<TicketPriority | ''>('');
  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Load categories for filter
  useEffect(() => {
    let isMounted = true;
    (async () => {
      try {
        const cats = await categoryApi.getCategories();
        if (isMounted) setCategories(cats);
      } catch {
        // non-blocking
      }
    })();
    return () => {
      isMounted = false;
    };
  }, []);

  const fetchTickets = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const response = await ticketApi.getTickets({
        page,
        size: 15,
        status: selectedStatus ? selectedStatus : undefined,
        priority: selectedPriority ? selectedPriority : undefined,
        categoryId: selectedCategory ? selectedCategory : undefined,
      });
      setTickets(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError(extractErrorMessage(err, 'Failed to load tickets.'));
    } finally {
      setLoading(false);
    }
  }, [user, page, selectedStatus, selectedPriority, selectedCategory]);

  useEffect(() => {
    fetchTickets();
  }, [fetchTickets]);

  const handleClearFilters = () => {
    setSelectedStatus('');
    setSelectedPriority('');
    setSelectedCategory('');
    setPage(0);
  };

  const hasFilters = selectedStatus !== '' || selectedPriority !== '' || selectedCategory !== '';

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

      {/* Filter Bar */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-4 mb-6">
        <div className="flex items-center gap-2 mb-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">
          <Filter size={14} />
          <span>Filters</span>
          {hasFilters && (
            <button
              onClick={handleClearFilters}
              className="ml-auto text-xs font-normal text-blue-600 hover:text-blue-800 lowercase tracking-normal"
            >
              Clear filters
            </button>
          )}
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <select
            value={selectedStatus}
            onChange={(e) => {
              setSelectedStatus(e.target.value as TicketStatus | '');
              setPage(0);
            }}
            className="h-9 text-xs rounded-lg border border-slate-300 bg-white px-2.5 text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
          >
            <option value="">All Statuses</option>
            {statuses.map((s) => (
              <option key={s} value={s}>
                {s === 'IN_PROGRESS' ? 'In Progress' : s.charAt(0) + s.slice(1).toLowerCase()}
              </option>
            ))}
          </select>

          <select
            value={selectedPriority}
            onChange={(e) => {
              setSelectedPriority(e.target.value as TicketPriority | '');
              setPage(0);
            }}
            className="h-9 text-xs rounded-lg border border-slate-300 bg-white px-2.5 text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
          >
            <option value="">All Priorities</option>
            {priorities.map((p) => (
              <option key={p} value={p}>
                {p.charAt(0) + p.slice(1).toLowerCase()}
              </option>
            ))}
          </select>

          <select
            value={selectedCategory}
            onChange={(e) => {
              setSelectedCategory(e.target.value);
              setPage(0);
            }}
            className="h-9 text-xs rounded-lg border border-slate-300 bg-white px-2.5 text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
          >
            <option value="">All Categories</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name.replace(/_/g, ' ')}
              </option>
            ))}
          </select>
        </div>
      </div>

      {loading ? (
        <InlineLoader message="Loading tickets..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchTickets} />
      ) : tickets.length === 0 ? (
        <Card>
          <EmptyState
            icon={<Inbox size={24} />}
            title={hasFilters ? 'No matching tickets' : 'No tickets yet'}
            description={
              hasFilters
                ? 'No tickets match your filter criteria. Try adjusting or clearing filters.'
                : 'You have not submitted any support tickets. Create your first ticket to get started.'
            }
            action={
              hasFilters ? (
                <Button variant="outline" onClick={handleClearFilters}>
                  Clear Filters
                </Button>
              ) : (
                <Button leftIcon={<PlusCircle size={18} />} onClick={() => navigate('/create-ticket')}>
                  Create Ticket
                </Button>
              )
            }
          />
        </Card>
      ) : (
        <div className="space-y-4">
          <TicketList tickets={tickets} linkPrefix="/tickets" />

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between px-2 pt-2 text-sm text-slate-600">
              <span>
                Showing {tickets.length} of {totalElements} ticket{totalElements === 1 ? '' : 's'}
              </span>
              <div className="flex items-center gap-2">
                <Button
                  size="sm"
                  variant="outline"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  Previous
                </Button>
                <span className="text-xs text-slate-500">
                  Page {page + 1} of {totalPages}
                </span>
                <Button
                  size="sm"
                  variant="outline"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next
                </Button>
              </div>
            </div>
          )}
        </div>
      )}
    </AppLayout>
  );
}
