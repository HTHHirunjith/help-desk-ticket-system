import { useEffect, useState, type ReactNode } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { ticketApi, commentApi, categoryApi } from '@/api/tickets';
import { extractErrorMessage } from '@/api/auth';
import type { TicketDetail, Comment, Category } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import {
  Card,
  CardHeader,
  Button,
  InlineLoader,
  EmptyState,
  ErrorState,
  Textarea,
  Input,
  Select,
  Modal,
} from '@/components/ui';
import { StatusBadge, PriorityBadge, CategoryBadge } from '@/components/ui/Badge';
import { formatDateTime, timeAgo } from '@/utils/format';
import {
  ArrowLeft,
  MessageSquare,
  User as UserIcon,
  Calendar,
  Tag,
  Clock,
  Send,
  Edit,
  CheckCircle2,
  XCircle,
  AlertCircle,
} from 'lucide-react';

export function TicketDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [ticket, setTicket] = useState<TicketDetail | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Comment state
  const [commentBody, setCommentBody] = useState('');
  const [submittingComment, setSubmittingComment] = useState(false);
  const [commentError, setCommentError] = useState<string | null>(null);

  // Workflow actions state
  const [actionInProgress, setActionInProgress] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Edit ticket modal state (only when OPEN and requester is user)
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editCategoryId, setEditCategoryId] = useState('');
  const [availableCategories, setAvailableCategories] = useState<Category[]>([]);
  const [updatingTicket, setUpdatingTicket] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  const fetchTicketAndComments = async (ticketId: string) => {
    try {
      setLoading(true);
      setError(null);
      const [ticketData, commentsData] = await Promise.all([
        ticketApi.getTicketById(ticketId),
        commentApi.getComments(ticketId),
      ]);
      setTicket(ticketData);
      setComments(commentsData);
    } catch (err) {
      setError(extractErrorMessage(err, 'Failed to load ticket.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!id) return;
    fetchTicketAndComments(id);
  }, [id]);

  // Handle adding comment
  const handleAddComment = async () => {
    if (!id || !commentBody.trim()) return;
    setSubmittingComment(true);
    setCommentError(null);
    try {
      const newComment = await commentApi.addComment(id, { body: commentBody.trim() });
      setComments((prev) => [...prev, newComment]);
      setCommentBody('');
    } catch (err) {
      setCommentError(extractErrorMessage(err, 'Failed to add comment.'));
    } finally {
      setSubmittingComment(false);
    }
  };

  // Workflow handlers (confirm & reject resolution)
  const handleConfirmResolution = async () => {
    if (!id) return;
    setActionInProgress(true);
    setActionError(null);
    setActionSuccess(null);
    try {
      const updated = await ticketApi.confirmResolution(id);
      setTicket(updated);
      setActionSuccess('Resolution confirmed. Thank you!');
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to confirm resolution.'));
    } finally {
      setActionInProgress(false);
    }
  };

  const handleRejectResolution = async () => {
    if (!id) return;
    setActionInProgress(true);
    setActionError(null);
    setActionSuccess(null);
    try {
      const updated = await ticketApi.rejectResolution(id);
      setTicket(updated);
      setActionSuccess('Ticket reopened and returned to Open status.');
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to reject resolution.'));
    } finally {
      setActionInProgress(false);
    }
  };

  // Open edit modal
  const openEditModal = async () => {
    if (!ticket) return;
    setEditTitle(ticket.title);
    setEditDescription(ticket.description);
    setEditCategoryId(ticket.category.id);
    setEditError(null);
    setEditModalOpen(true);

    try {
      const cats = await categoryApi.getCategories(true);
      setAvailableCategories(cats);
    } catch {
      // non-blocking fallback
    }
  };

  const handleSaveEdit = async () => {
    if (!id) return;
    if (!editTitle.trim()) {
      setEditError('Title is required');
      return;
    }
    if (!editDescription.trim()) {
      setEditError('Description is required');
      return;
    }

    setUpdatingTicket(true);
    setEditError(null);
    try {
      const updated = await ticketApi.updateOpenTicket(id, {
        title: editTitle.trim(),
        description: editDescription.trim(),
        categoryId: editCategoryId,
      });
      setTicket(updated);
      setEditModalOpen(false);
    } catch (err) {
      setEditError(extractErrorMessage(err, 'Failed to update ticket.'));
    } finally {
      setUpdatingTicket(false);
    }
  };

  if (loading) {
    return (
      <AppLayout>
        <InlineLoader message="Loading ticket details..." />
      </AppLayout>
    );
  }

  if (error || !ticket) {
    return (
      <AppLayout>
        <ErrorState
          title="Ticket not found"
          message={error || 'This ticket may not exist or you do not have permission to view it.'}
          onRetry={() => navigate(-1)}
        />
      </AppLayout>
    );
  }

  const isOwner = user?.id === ticket.requester.id;
  const isEditable = isOwner && ticket.status === 'OPEN';
  const isResolved = ticket.status === 'RESOLVED';
  const isConfirmed = ticket.resolutionConfirmedAt !== null;

  return (
    <AppLayout>
      <div className="mb-6 flex items-center justify-between">
        <Button variant="ghost" leftIcon={<ArrowLeft size={18} />} onClick={() => navigate('/my-tickets')}>
          Back to Tickets
        </Button>

        {isEditable && (
          <Button variant="outline" size="sm" leftIcon={<Edit size={16} />} onClick={openEditModal}>
            Edit Ticket
          </Button>
        )}
      </div>

      {/* Action alert / banner if any */}
      {actionSuccess && (
        <div className="mb-6 p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-sm text-emerald-800 flex items-center gap-2">
          <CheckCircle2 size={18} className="text-emerald-600 shrink-0" />
          <span>{actionSuccess}</span>
        </div>
      )}
      {actionError && (
        <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-sm text-red-800 flex items-center gap-2">
          <AlertCircle size={18} className="text-red-600 shrink-0" />
          <span>{actionError}</span>
        </div>
      )}

      {/* Reopened Banner Notice */}
      {ticket.status === 'OPEN' && ticket.assignedAgent && (
        <div className="mb-6 p-4 rounded-xl bg-amber-50 border border-amber-200 text-sm text-amber-900 flex items-center gap-2">
          <AlertCircle size={18} className="text-amber-600 shrink-0" />
          <div>
            <span className="font-semibold">Reopened Ticket: </span>
            <span>
              This ticket was reopened and is assigned to {ticket.assignedAgent.name}. Work will resume shortly.
            </span>
          </div>
        </div>
      )}

      {/* Resolution Confirmation Card for Requester */}
      {isOwner && isResolved && !isConfirmed && (
        <div className="mb-6 p-5 rounded-xl bg-blue-50 border border-blue-200">
          <h3 className="text-base font-semibold text-blue-900 mb-1">Has your issue been resolved?</h3>
          <p className="text-sm text-blue-700 mb-4">
            The support agent has marked this ticket as resolved. Please review the resolution and confirm or reopen.
          </p>
          <div className="flex items-center gap-3">
            <Button
              size="sm"
              loading={actionInProgress}
              leftIcon={<CheckCircle2 size={16} />}
              onClick={handleConfirmResolution}
              className="bg-emerald-600 hover:bg-emerald-700 text-white"
            >
              Confirm Resolution
            </Button>
            <Button
              size="sm"
              variant="outline"
              loading={actionInProgress}
              leftIcon={<XCircle size={16} />}
              onClick={handleRejectResolution}
              className="border-red-300 text-red-700 hover:bg-red-50"
            >
              Reject & Reopen
            </Button>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Column */}
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <div className="flex items-center gap-2 mb-3">
              <span className="text-xs font-mono font-medium text-slate-500">#{ticket.ticketNumber}</span>
              <StatusBadge status={ticket.status} />
              <PriorityBadge priority={ticket.priority} />
              <CategoryBadge category={ticket.category.name} />
              {isConfirmed && (
                <span className="inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
                  <CheckCircle2 size={12} /> Confirmed
                </span>
              )}
            </div>
            <h1 className="text-xl font-bold text-slate-900 mb-3">{ticket.title}</h1>
            <p className="text-sm text-slate-600 leading-relaxed whitespace-pre-wrap">{ticket.description}</p>
            <div className="flex items-center gap-4 mt-5 pt-4 border-t border-slate-100 text-xs text-slate-400">
              <span className="flex items-center gap-1">
                <Calendar size={13} /> Created {formatDateTime(ticket.createdAt)}
              </span>
              <span className="flex items-center gap-1">
                <Clock size={13} /> Updated {timeAgo(ticket.updatedAt)}
              </span>
            </div>
          </Card>

          {/* Comments Section */}
          <Card>
            <CardHeader
              title="Comments & Activity"
              subtitle={`${comments.length} comment${comments.length === 1 ? '' : 's'}`}
              icon={<MessageSquare size={16} />}
            />
            {comments.length === 0 ? (
              <EmptyState title="No comments yet" description="Be the first to add a comment to this ticket." />
            ) : (
              <div className="space-y-4">
                {comments.map((comment) => (
                  <div key={comment.id} className="flex gap-3">
                    <div className="flex items-center justify-center w-8 h-8 rounded-full bg-slate-200 text-slate-700 text-xs font-semibold shrink-0">
                      {comment.author.name
                        .split(' ')
                        .map((n) => n[0])
                        .join('')
                        .toUpperCase()}
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <span className="text-sm font-medium text-slate-900">{comment.author.name}</span>
                        <span className="text-xs text-slate-400">{timeAgo(comment.createdAt)}</span>
                      </div>
                      <p className="text-sm text-slate-600 leading-relaxed whitespace-pre-wrap">{comment.body}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Add Comment Input */}
            <div className="mt-5 pt-5 border-t border-slate-100">
              <Textarea
                name="comment"
                placeholder="Type your comment..."
                value={commentBody}
                onChange={(e) => setCommentBody(e.target.value)}
                disabled={submittingComment}
              />
              {commentError && <p className="mt-2 text-xs text-red-600">{commentError}</p>}
              <div className="flex justify-end mt-3">
                <Button
                  size="sm"
                  leftIcon={<Send size={15} />}
                  loading={submittingComment}
                  onClick={handleAddComment}
                  disabled={!commentBody.trim()}
                >
                  Add Comment
                </Button>
              </div>
            </div>
          </Card>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          <Card>
            <CardHeader title="Ticket Details" />
            <div className="space-y-4">
              <DetailRow icon={<UserIcon size={15} />} label="Requester" value={ticket.requester.name} />
              <DetailRow
                icon={<UserIcon size={15} />}
                label="Assigned Agent"
                value={ticket.assignedAgent?.name || 'Unassigned'}
              />
              <DetailRow icon={<Tag size={15} />} label="Category" value={ticket.category.name.replace(/_/g, ' ')} />
              <DetailRow icon={<Calendar size={15} />} label="Created" value={formatDateTime(ticket.createdAt)} />
              <DetailRow icon={<Clock size={15} />} label="Last Updated" value={formatDateTime(ticket.updatedAt)} />
              {ticket.resolutionConfirmedAt && (
                <DetailRow
                  icon={<CheckCircle2 size={15} />}
                  label="Resolution Confirmed"
                  value={formatDateTime(ticket.resolutionConfirmedAt)}
                />
              )}
            </div>
          </Card>
        </div>
      </div>

      {/* Edit Ticket Modal */}
      <Modal open={editModalOpen} onClose={() => setEditModalOpen(false)} title="Edit Ticket" size="md">
        <div className="space-y-4">
          <Input
            label="Title"
            name="title"
            value={editTitle}
            onChange={(e) => setEditTitle(e.target.value)}
            disabled={updatingTicket}
          />
          <Textarea
            label="Description"
            name="description"
            value={editDescription}
            onChange={(e) => setEditDescription(e.target.value)}
            disabled={updatingTicket}
          />
          <Select
            label="Category"
            name="category"
            value={editCategoryId}
            onChange={(e) => setEditCategoryId(e.target.value)}
            disabled={updatingTicket}
          >
            {availableCategories.map((cat) => (
              <option key={cat.id} value={cat.id}>
                {cat.name.replace(/_/g, ' ')}
              </option>
            ))}
          </Select>
          {editError && <div className="p-3 bg-red-50 text-xs text-red-700 rounded-lg">{editError}</div>}
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="outline" onClick={() => setEditModalOpen(false)} disabled={updatingTicket}>
              Cancel
            </Button>
            <Button loading={updatingTicket} onClick={handleSaveEdit}>
              Save Changes
            </Button>
          </div>
        </div>
      </Modal>
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
