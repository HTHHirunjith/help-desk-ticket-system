import { useEffect, useState, type ReactNode } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { ticketService } from '@/services';
import type { Ticket } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { Card, CardHeader, Button, InlineLoader, EmptyState, ErrorState } from '@/components/ui';
import { StatusBadge, PriorityBadge, CategoryBadge } from '@/components/ui/Badge';
import { formatDateTime, timeAgo } from '@/utils/format';
import { ArrowLeft, MessageSquare, User as UserIcon, Calendar, Tag, Clock } from 'lucide-react';

export function TicketDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (!id) return;
    (async () => {
      setLoading(true);
      setError(false);
      const t = await ticketService.getTicketById(id);
      setTicket(t);
      setLoading(false);
    })();
  }, [id]);

  if (loading) return <AppLayout><InlineLoader message="Loading ticket..." /></AppLayout>;
  if (error || !ticket) return (
    <AppLayout>
      <ErrorState title="Ticket not found" message="This ticket may have been deleted or you do not have access." onRetry={() => navigate(-1)} />
    </AppLayout>
  );

  const isOwner = user?.id === ticket.requesterId;

  return (
    <AppLayout>
      <div className="mb-6">
        <Button variant="ghost" leftIcon={<ArrowLeft size={18} />} onClick={() => navigate(-1)}>
          Back
        </Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main column */}
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <div className="flex items-center gap-2 mb-3">
              <span className="text-xs font-mono font-medium text-slate-500">{ticket.ticketNumber}</span>
              <StatusBadge status={ticket.status} />
              <PriorityBadge priority={ticket.priority} />
              <CategoryBadge category={ticket.category} />
            </div>
            <h1 className="text-xl font-bold text-slate-900 mb-3">{ticket.title}</h1>
            <p className="text-sm text-slate-600 leading-relaxed whitespace-pre-wrap">{ticket.description}</p>
            <div className="flex items-center gap-4 mt-5 pt-4 border-t border-slate-100 text-xs text-slate-400">
              <span className="flex items-center gap-1"><Calendar size={13} /> Created {formatDateTime(ticket.createdAt)}</span>
              <span className="flex items-center gap-1"><Clock size={13} /> Updated {timeAgo(ticket.updatedAt)}</span>
            </div>
          </Card>

          {/* Comments */}
          <Card>
            <CardHeader title="Comments & Activity" subtitle={`${ticket.comments.length} comment${ticket.comments.length === 1 ? '' : 's'}`} icon={<MessageSquare size={16} />} />
            {ticket.comments.length === 0 ? (
              <EmptyState title="No comments yet" description="Be the first to add a comment to this ticket." />
            ) : (
              <div className="space-y-4">
                {ticket.comments.map((comment) => (
                  <div key={comment.id} className="flex gap-3">
                    <div className="flex items-center justify-center w-8 h-8 rounded-full bg-slate-200 text-slate-700 text-xs font-semibold shrink-0">
                      {comment.authorName.split(' ').map((n) => n[0]).join('').toUpperCase()}
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <span className="text-sm font-medium text-slate-900">{comment.authorName}</span>
                        <span className="text-xs text-slate-400">{timeAgo(comment.createdAt)}</span>
                      </div>
                      <p className="text-sm text-slate-600 leading-relaxed">{comment.body}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          <Card>
            <CardHeader title="Ticket Details" />
            <div className="space-y-4">
              <DetailRow icon={<UserIcon size={15} />} label="Requester" value={ticket.requesterName} />
              <DetailRow icon={<UserIcon size={15} />} label="Assigned Agent" value={ticket.assignedAgentName || 'Unassigned'} />
              <DetailRow icon={<Tag size={15} />} label="Category" value={ticket.category.replace(/_/g, ' ')} />
              <DetailRow icon={<Calendar size={15} />} label="Created" value={formatDateTime(ticket.createdAt)} />
              <DetailRow icon={<Clock size={15} />} label="Last Updated" value={formatDateTime(ticket.updatedAt)} />
            </div>
          </Card>

          {isOwner && (
            <Card>
              <CardHeader title="Quick Actions" />
              <Button variant="outline" fullWidth onClick={() => navigate('/my-tickets')}>
                View All My Tickets
              </Button>
            </Card>
          )}
        </div>
      </div>
    </AppLayout>
  );
}

function DetailRow({ icon, label, value }: { icon: ReactNode; label: string; value: string }) {
  return (
    <div className="flex items-start gap-3">
      <div className="text-slate-400 mt-0.5">{icon}</div>
      <div className="flex-1">
        <p className="text-xs text-slate-500 mb-0.5">{label}</p>
        <p className="text-sm font-medium text-slate-900">{value}</p>
      </div>
    </div>
  );
}
