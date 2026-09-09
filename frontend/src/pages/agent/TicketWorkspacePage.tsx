import { useEffect, useState, type ReactNode } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { ticketApi, commentApi, auditApi, userApi } from '@/api/tickets';
import { extractErrorMessage } from '@/api/auth';
import type { TicketDetail, TicketPriority, Comment, AuditEntry, User } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import {
  Card,
  CardHeader,
  Button,
  InlineLoader,
  EmptyState,
  ErrorState,
  Select,
  Textarea,
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
  Play,
  CheckCircle,
  Lock,
  History,
  AlertCircle,
  CheckCircle2,
  UserPlus,
  UserMinus,
} from 'lucide-react';

const allPriorities: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

export function TicketWorkspacePage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [ticket, setTicket] = useState<TicketDetail | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [audits, setAudits] = useState<AuditEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Comment state
  const [commentBody, setCommentBody] = useState('');
  const [submittingComment, setSubmittingComment] = useState(false);
  const [commentError, setCommentError] = useState<string | null>(null);

  // Priority state
  const [changingPriority, setChangingPriority] = useState(false);

  // Workflow action state
  const [workflowActionRunning, setWorkflowActionRunning] = useState(false);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // Assignment modal state (ADMIN only)
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [targetAgentId, setTargetAgentId] = useState('');
  const [assigning, setAssigning] = useState(false);
  const [assignError, setAssignError] = useState<string | null>(null);
  const [agents, setAgents] = useState<User[]>([]);
  const [loadingAgents, setLoadingAgents] = useState(false);
  const [loadAgentsError, setLoadAgentsError] = useState<string | null>(null);

  const isAdmin = user?.role === 'ADMIN';
  const isAgent = user?.role === 'SUPPORT_AGENT';

  const fetchAgents = async () => {
    setLoadingAgents(true);
    setLoadAgentsError(null);
    try {
      const data = await userApi.getUsers({ role: 'SUPPORT_AGENT', active: true });
      setAgents(data);
    } catch (err) {
      setLoadAgentsError(extractErrorMessage(err, 'Failed to load support agents.'));
    } finally {
      setLoadingAgents(false);
    }
  };

  const handleOpenAssignModal = () => {
    setAssignModalOpen(true);
    setAssignError(null);
    setTargetAgentId('');
    fetchAgents();
  };

  const fetchTicketData = async (ticketId: string) => {
    try {
      setLoading(true);
      setError(null);

      const promises: [Promise<TicketDetail>, Promise<Comment[]>, Promise<AuditEntry[]>?] = [
        ticketApi.getTicketById(ticketId),
        commentApi.getComments(ticketId),
      ];

      if (user?.role === 'ADMIN') {
        promises.push(auditApi.getAuditHistory(ticketId));
      }

      const [ticketData, commentsData, auditData] = await Promise.all(promises);
      setTicket(ticketData);
      setComments(commentsData);
      if (auditData) {
        setAudits(auditData);
      }
    } catch (err) {
      setError(extractErrorMessage(err, 'Failed to load ticket workspace.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!id) return;
    fetchTicketData(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, user?.role]);

  // Handle priority change (SUPPORT_AGENT assigned or ADMIN)
  const handlePriorityChange = async (newPriority: TicketPriority) => {
    if (!id || !ticket || ticket.priority === newPriority) return;
    setChangingPriority(true);
    setActionError(null);
    try {
      const updated = await ticketApi.changePriority(id, { priority: newPriority });
      setTicket(updated);
      setActionSuccess(`Priority changed to ${newPriority}`);
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to change priority.'));
    } finally {
      setChangingPriority(false);
    }
  };

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

  // Workflow actions
  const handleStartWork = async () => {
    if (!id) return;
    setWorkflowActionRunning(true);
    setActionError(null);
    try {
      const updated = await ticketApi.startWork(id);
      setTicket(updated);
      setActionSuccess('Work started on this ticket (status is now In Progress).');
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to start work on ticket.'));
    } finally {
      setWorkflowActionRunning(false);
    }
  };

  const handleResolveTicket = async () => {
    if (!id) return;
    setWorkflowActionRunning(true);
    setActionError(null);
    try {
      const updated = await ticketApi.resolveTicket(id);
      setTicket(updated);
      setActionSuccess('Ticket marked as Resolved. Requester can now confirm resolution.');
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to resolve ticket.'));
    } finally {
      setWorkflowActionRunning(false);
    }
  };

  const handleCloseTicket = async () => {
    if (!id) return;
    setWorkflowActionRunning(true);
    setActionError(null);
    try {
      const updated = await ticketApi.closeTicket(id);
      setTicket(updated);
      setActionSuccess('Ticket closed permanently.');
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to close ticket.'));
    } finally {
      setWorkflowActionRunning(false);
    }
  };

  // Assignment actions (ADMIN only)
  const handleAssignTicket = async () => {
    if (!id || !targetAgentId.trim()) return;
    setAssigning(true);
    setAssignError(null);
    try {
      const updated = await ticketApi.assignTicket(id, { agentId: targetAgentId.trim() });
      setTicket(updated);
      setAssignModalOpen(false);
      setTargetAgentId('');
      setActionSuccess('Ticket assigned successfully.');
      // Refresh audits if admin
      if (isAdmin) {
        const updatedAudits = await auditApi.getAuditHistory(id);
        setAudits(updatedAudits);
      }
    } catch (err) {
      setAssignError(extractErrorMessage(err, 'Failed to assign ticket.'));
    } finally {
      setAssigning(false);
    }
  };

  const handleUnassignTicket = async () => {
    if (!id) return;
    setAssigning(true);
    setActionError(null);
    try {
      await ticketApi.unassignTicket(id);
      // Re-fetch ticket to get updated state
      const updated = await ticketApi.getTicketById(id);
      setTicket(updated);
      setActionSuccess('Ticket unassigned successfully.');
      if (isAdmin) {
        const updatedAudits = await auditApi.getAuditHistory(id);
        setAudits(updatedAudits);
      }
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to unassign ticket.'));
    } finally {
      setAssigning(false);
    }
  };

  if (loading) {
    return (
      <AppLayout>
        <InlineLoader message="Loading ticket workspace..." />
      </AppLayout>
    );
  }

  if (error || !ticket) {
    return (
      <AppLayout>
        <ErrorState
          title="Ticket not found"
          message={error || 'This ticket may have been deleted or you do not have permission to view it.'}
          onRetry={() => navigate(-1)}
        />
      </AppLayout>
    );
  }

  const isAssignedAgent = isAgent && ticket.assignedAgent?.id === user?.id;
  const canStart = isAssignedAgent && ticket.status === 'OPEN';
  const canResolve = isAssignedAgent && ticket.status === 'IN_PROGRESS';
  const canClose = isAdmin && ticket.status === 'RESOLVED' && ticket.resolutionConfirmedAt !== null;

  return (
    <AppLayout>
      <div className="mb-6">
        <Button variant="ghost" leftIcon={<ArrowLeft size={18} />} onClick={() => navigate(-1)}>
          Back
        </Button>
      </div>

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
      {ticket.reopenedAt != null && (
        <div className="mb-6 p-4 rounded-xl bg-amber-50 border border-amber-200 text-sm text-amber-900 flex items-center gap-2">
          <AlertCircle size={18} className="text-amber-600 shrink-0" />
          <div>
            <span className="font-semibold">Reopened Ticket: </span>
            <span>
              The requester rejected the resolution and this ticket was returned to Open. Please review and start work.
            </span>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main column */}
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <div className="flex items-center gap-2 mb-3">
              <span className="text-xs font-mono font-medium text-slate-500">#{ticket.ticketNumber}</span>
              <StatusBadge status={ticket.status} />
              <PriorityBadge priority={ticket.priority} />
              <CategoryBadge category={ticket.category.name} />
              {ticket.resolutionConfirmedAt && (
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

          {/* Comments */}
          <Card>
            <CardHeader
              title="Comments & Activity"
              subtitle={`${comments.length} comment${comments.length === 1 ? '' : 's'}`}
              icon={<MessageSquare size={16} />}
            />
            {comments.length === 0 ? (
              <EmptyState title="No comments yet" description="Add a comment to start the conversation on this ticket." />
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

            {/* Add comment */}
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

          {/* Audit History (ADMIN Only) */}
          {isAdmin && (
            <Card>
              <CardHeader
                title="Audit Timeline"
                subtitle={`Recorded system events (${audits.length})`}
                icon={<History size={16} />}
              />
              {audits.length === 0 ? (
                <EmptyState title="No audit entries" description="No system audit logs found for this ticket." />
              ) : (
                <div className="space-y-3">
                  {audits.map((a) => (
                    <div
                      key={a.id}
                      className="p-3 rounded-lg border border-slate-100 bg-slate-50 text-xs text-slate-700 flex flex-col gap-1"
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-slate-900 font-mono">{a.action}</span>
                        <span className="text-slate-400">{formatDateTime(a.createdAt)}</span>
                      </div>
                      <div>
                        <span className="text-slate-500">Actor: </span>
                        <span className="font-medium text-slate-800">
                          {a.actor ? `${a.actor.name} (${a.actor.role})` : 'System'}
                        </span>
                      </div>
                      {a.details && (
                        <div className="mt-1 font-mono text-[11px] text-slate-600 bg-white p-2 rounded border border-slate-200 overflow-x-auto">
                          {a.details}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </Card>
          )}
        </div>

        {/* Sidebar — Controls */}
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

          {/* Workflow & Priority Controls */}
          {(isAssignedAgent || isAdmin) && (
            <Card>
              <CardHeader
                title="Management Controls"
                subtitle={isAdmin ? 'Administrative actions' : 'Agent workflow'}
              />
              <div className="space-y-4">
                {/* Priority Selector */}
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1.5">Change Priority</label>
                  <Select
                    name="priority"
                    value={ticket.priority}
                    onChange={(e) => handlePriorityChange(e.target.value as TicketPriority)}
                    disabled={changingPriority}
                  >
                    {allPriorities.map((p) => (
                      <option key={p} value={p}>
                        {p.charAt(0) + p.slice(1).toLowerCase()}
                      </option>
                    ))}
                  </Select>
                </div>

                {/* Workflow Transitions */}
                <div className="pt-2 border-t border-slate-100 space-y-2">
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Workflow Actions</label>

                  {canStart && (
                    <Button
                      fullWidth
                      size="sm"
                      leftIcon={<Play size={15} />}
                      loading={workflowActionRunning}
                      onClick={handleStartWork}
                    >
                      Start Work
                    </Button>
                  )}

                  {canResolve && (
                    <Button
                      fullWidth
                      size="sm"
                      leftIcon={<CheckCircle size={15} />}
                      loading={workflowActionRunning}
                      onClick={handleResolveTicket}
                      className="bg-emerald-600 hover:bg-emerald-700 text-white"
                    >
                      Resolve Ticket
                    </Button>
                  )}

                  {canClose && (
                    <Button
                      fullWidth
                      size="sm"
                      leftIcon={<Lock size={15} />}
                      loading={workflowActionRunning}
                      onClick={handleCloseTicket}
                      className="bg-slate-900 hover:bg-black text-white"
                    >
                      Close Ticket
                    </Button>
                  )}

                  {!canStart && !canResolve && !canClose && (
                    <p className="text-xs text-slate-400 italic">No workflow transitions available in this state.</p>
                  )}
                </div>

                {/* ADMIN Assignment Controls */}
                {isAdmin && (
                  <div className="pt-3 border-t border-slate-100 space-y-2">
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Assignment</label>
                    <Button
                      fullWidth
                      size="sm"
                      variant="outline"
                      leftIcon={<UserPlus size={15} />}
                      onClick={handleOpenAssignModal}
                    >
                      {ticket.assignedAgent ? 'Reassign Agent' : 'Assign Agent'}
                    </Button>

                    {ticket.assignedAgent && (
                      <Button
                        fullWidth
                        size="sm"
                        variant="outline"
                        leftIcon={<UserMinus size={15} />}
                        loading={assigning}
                        onClick={handleUnassignTicket}
                        className="text-red-700 border-red-200 hover:bg-red-50"
                      >
                        Unassign Ticket
                      </Button>
                    )}
                  </div>
                )}
              </div>
            </Card>
          )}
        </div>
      </div>

      {/* Admin Assign Modal */}
      {isAdmin && (
        <Modal
          open={assignModalOpen}
          onClose={() => setAssignModalOpen(false)}
          title={ticket.assignedAgent ? 'Reassign Ticket' : 'Assign Ticket'}
          size="sm"
        >
          <div className="space-y-4">
            <p className="text-xs text-slate-500">
              Select an active Support Agent to {ticket.assignedAgent ? 'reassign' : 'assign'} this ticket to:
            </p>

            {loadingAgents ? (
              <InlineLoader message="Loading support agents..." />
            ) : loadAgentsError ? (
              <div className="space-y-2">
                <div className="p-3 bg-red-50 text-xs text-red-700 rounded-lg">{loadAgentsError}</div>
                <Button size="sm" variant="outline" onClick={fetchAgents}>
                  Retry
                </Button>
              </div>
            ) : agents.length === 0 ? (
              <div className="p-3 bg-amber-50 border border-amber-200 text-xs text-amber-800 rounded-lg">
                No active support agents available for assignment.
              </div>
            ) : (
              <Select
                name="agentId"
                label="Support Agent"
                value={targetAgentId}
                onChange={(e) => {
                  setTargetAgentId(e.target.value);
                  setAssignError(null);
                }}
                disabled={assigning}
              >
                <option value="">Select Support Agent</option>
                {agents.map((agent) => (
                  <option
                    key={agent.id}
                    value={agent.id}
                    disabled={ticket.assignedAgent?.id === agent.id}
                  >
                    {agent.firstName} {agent.lastName} ({agent.email})
                    {ticket.assignedAgent?.id === agent.id ? ' — Currently Assigned' : ''}
                  </option>
                ))}
              </Select>
            )}

            {assignError && <div className="p-3 bg-red-50 text-xs text-red-700 rounded-lg">{assignError}</div>}

            <div className="flex justify-end gap-2 pt-2">
              <Button variant="outline" onClick={() => setAssignModalOpen(false)} disabled={assigning}>
                Cancel
              </Button>
              <Button
                loading={assigning}
                onClick={handleAssignTicket}
                disabled={!targetAgentId.trim() || loadingAgents || agents.length === 0}
              >
                {ticket.assignedAgent ? 'Confirm Reassignment' : 'Confirm Assignment'}
              </Button>
            </div>
          </div>
        </Modal>
      )}
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
