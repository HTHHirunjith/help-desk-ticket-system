import { useEffect, useState, type FormEvent } from 'react';
import { userApi } from '@/api/tickets';
import { extractErrorMessage } from '@/api/auth';
import type { User } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { PageHeader } from '@/components/layout/PageHeader';
import {
  Card,
  InlineLoader,
  EmptyState,
  ErrorState,
  Button,
  Input,
  Modal,
} from '@/components/ui';
import { formatDate, getInitials } from '@/utils/format';
import {
  Inbox,
  UserPlus,
  Edit2,
  CheckCircle2,
  AlertCircle,
  Power,
  Check,
  Mail,
  ShieldAlert,
} from 'lucide-react';

export function AdminAgentsPage() {
  const [agents, setAgents] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Global action notifications
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // Create Agent modal state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createFirstName, setCreateFirstName] = useState('');
  const [createLastName, setCreateLastName] = useState('');
  const [createEmail, setCreateEmail] = useState('');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  // Edit Agent modal state
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editingAgent, setEditingAgent] = useState<User | null>(null);
  const [editFirstName, setEditFirstName] = useState('');
  const [editLastName, setEditLastName] = useState('');
  const [editEmail, setEditEmail] = useState('');
  const [updating, setUpdating] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  // Deactivate/Activate confirmation state
  const [toggleAgent, setToggleAgent] = useState<User | null>(null);
  const [toggleLoading, setToggleLoading] = useState(false);

  const fetchAgents = async () => {
    setLoading(true);
    setError(null);
    try {
      const allAgents = await userApi.getUsers({ role: 'SUPPORT_AGENT' });
      setAgents(allAgents);
    } catch (err) {
      setError(extractErrorMessage(err, 'Failed to load support agents.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAgents();
  }, []);

  // Handle Create Agent
  const handleOpenCreateModal = () => {
    setCreateFirstName('');
    setCreateLastName('');
    setCreateEmail('');
    setCreateError(null);
    setCreateModalOpen(true);
  };

  const handleCreateAgent = async (e: FormEvent) => {
    e.preventDefault();
    if (!createFirstName.trim() || !createLastName.trim() || !createEmail.trim()) {
      setCreateError('First name, last name, and email are all required.');
      return;
    }

    setCreating(true);
    setCreateError(null);
    setActionSuccess(null);
    setActionError(null);

    try {
      const newAgent = await userApi.createUser({
        firstName: createFirstName.trim(),
        lastName: createLastName.trim(),
        email: createEmail.trim(),
        role: 'SUPPORT_AGENT',
      });

      setAgents((prev) => [...prev, newAgent].sort((a, b) => a.firstName.localeCompare(b.firstName)));
      setCreateModalOpen(false);
      setActionSuccess(
        `Support agent ${newAgent.firstName} ${newAgent.lastName} created successfully. Temporary credentials have been emailed to ${newAgent.email}.`
      );
    } catch (err) {
      setCreateError(extractErrorMessage(err, 'Failed to create support agent.'));
    } finally {
      setCreating(false);
    }
  };

  // Handle Edit Agent
  const handleOpenEditModal = (targetAgent: User) => {
    setEditingAgent(targetAgent);
    setEditFirstName(targetAgent.firstName);
    setEditLastName(targetAgent.lastName);
    setEditEmail(targetAgent.email);
    setEditError(null);
    setEditModalOpen(true);
  };

  const handleUpdateAgent = async (e: FormEvent) => {
    e.preventDefault();
    if (!editingAgent) return;

    if (!editFirstName.trim() || !editLastName.trim() || !editEmail.trim()) {
      setEditError('First name, last name, and email cannot be empty.');
      return;
    }

    setUpdating(true);
    setEditError(null);
    setActionSuccess(null);
    setActionError(null);

    try {
      const updated = await userApi.updateUser(editingAgent.id, {
        firstName: editFirstName.trim(),
        lastName: editLastName.trim(),
        email: editEmail.trim(),
      });

      setAgents((prev) => prev.map((a) => (a.id === updated.id ? updated : a)));
      setEditModalOpen(false);
      setActionSuccess(`Support agent ${updated.firstName} ${updated.lastName} updated successfully.`);
    } catch (err) {
      setEditError(extractErrorMessage(err, 'Failed to update support agent.'));
    } finally {
      setUpdating(false);
    }
  };

  // Handle Activate / Deactivate Toggle
  const handleOpenToggleModal = (targetAgent: User) => {
    setActionError(null);
    setActionSuccess(null);
    setToggleAgent(targetAgent);
  };

  const handleConfirmToggle = async () => {
    if (!toggleAgent) return;

    setToggleLoading(true);
    setActionError(null);
    setActionSuccess(null);

    try {
      let updated: User;
      if (toggleAgent.active !== false) {
        updated = await userApi.deactivateUser(toggleAgent.id);
        setActionSuccess(`Support agent ${updated.firstName} ${updated.lastName} has been deactivated.`);
      } else {
        updated = await userApi.activateUser(toggleAgent.id);
        setActionSuccess(`Support agent ${updated.firstName} ${updated.lastName} has been activated.`);
      }
      setAgents((prev) => prev.map((a) => (a.id === updated.id ? updated : a)));
      setToggleAgent(null);
    } catch (err) {
      const msg = extractErrorMessage(err, 'Failed to update support agent status.');
      setActionError(msg);
      setToggleAgent(null);
    } finally {
      setToggleLoading(false);
    }
  };

  return (
    <AppLayout>
      <PageHeader
        title="Support Agents"
        description="Manage support agents and their operational status."
        action={
          <Button
            variant="primary"
            leftIcon={<UserPlus size={16} />}
            onClick={handleOpenCreateModal}
          >
            Add Support Agent
          </Button>
        }
      />

      {/* Action Success Alert */}
      {actionSuccess && (
        <div className="mb-6 flex items-start gap-3 rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800 animate-in fade-in duration-200">
          <CheckCircle2 className="h-5 w-5 text-emerald-600 shrink-0 mt-0.5" />
          <div className="flex-1 font-medium">{actionSuccess}</div>
          <button
            onClick={() => setActionSuccess(null)}
            className="text-emerald-700 hover:text-emerald-900 text-xs font-semibold"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Action Error Alert */}
      {actionError && (
        <div className="mb-6 flex items-start gap-3 rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800 animate-in fade-in duration-200">
          <AlertCircle className="h-5 w-5 text-rose-600 shrink-0 mt-0.5" />
          <div className="flex-1 font-medium">{actionError}</div>
          <button
            onClick={() => setActionError(null)}
            className="text-rose-700 hover:text-rose-900 text-xs font-semibold"
          >
            Dismiss
          </button>
        </div>
      )}

      {loading ? (
        <InlineLoader message="Loading support agents..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchAgents} />
      ) : agents.length === 0 ? (
        <Card>
          <EmptyState
            icon={<Inbox size={24} />}
            title="No support agents"
            description="There are currently no support agents registered in the system."
            action={
              <Button
                variant="primary"
                leftIcon={<UserPlus size={16} />}
                onClick={handleOpenCreateModal}
              >
                Add First Agent
              </Button>
            }
          />
        </Card>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {agents.map((agent) => {
            const isActive = agent.active !== false;

            return (
              <Card key={agent.id}>
                <div className="flex items-start gap-3 mb-4">
                  <div className="flex items-center justify-center w-11 h-11 rounded-full bg-slate-200 text-slate-700 text-sm font-semibold shrink-0">
                    {getInitials(agent.firstName, agent.lastName)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-slate-900 truncate">
                      {agent.firstName} {agent.lastName}
                    </p>
                    <p className="text-xs text-slate-500 truncate">{agent.email}</p>
                  </div>
                  <span
                    className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium border shrink-0 ${
                      isActive
                        ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                        : 'bg-rose-50 text-rose-700 border border-rose-200'
                    }`}
                  >
                    <span className={`w-1.5 h-1.5 rounded-full ${isActive ? 'bg-emerald-500' : 'bg-rose-500'}`} />
                    {isActive ? 'Active' : 'Inactive'}
                  </span>
                </div>

                <div className="text-xs text-slate-500 mb-4 space-y-1">
                  <p>Role: <span className="font-medium text-slate-700">Support Agent</span></p>
                  <p>Joined: <span className="font-medium text-slate-700">{agent.createdAt ? formatDate(agent.createdAt) : '—'}</span></p>
                </div>

                <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
                  <Button
                    variant="ghost"
                    size="sm"
                    leftIcon={<Edit2 size={14} />}
                    onClick={() => handleOpenEditModal(agent)}
                    aria-label={`Edit ${agent.firstName} ${agent.lastName}`}
                  >
                    Edit
                  </Button>

                  {isActive ? (
                    <Button
                      variant="ghost"
                      size="sm"
                      leftIcon={<Power size={14} className="text-rose-500" />}
                      onClick={() => handleOpenToggleModal(agent)}
                      aria-label={`Deactivate ${agent.firstName} ${agent.lastName}`}
                    >
                      Deactivate
                    </Button>
                  ) : (
                    <Button
                      variant="ghost"
                      size="sm"
                      leftIcon={<Check size={14} className="text-emerald-600" />}
                      onClick={() => handleOpenToggleModal(agent)}
                      aria-label={`Activate ${agent.firstName} ${agent.lastName}`}
                    >
                      Activate
                    </Button>
                  )}
                </div>
              </Card>
            );
          })}
        </div>
      )}

      {/* Create Agent Modal */}
      <Modal
        open={createModalOpen}
        onClose={() => {
          if (!creating) setCreateModalOpen(false);
        }}
        title="Create Support Agent"
      >
        <form onSubmit={handleCreateAgent} className="space-y-4">
          <div className="rounded-lg bg-indigo-50 border border-indigo-100 p-3 text-xs text-indigo-800 flex items-start gap-2.5">
            <Mail className="h-4 w-4 text-indigo-600 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold mb-0.5">Secure Credentials Provisioning</p>
              <p>A cryptographically secure temporary password will be generated and delivered to the agent&apos;s email address.</p>
            </div>
          </div>

          {createError && (
            <div className="rounded-md bg-rose-50 border border-rose-200 p-3 text-xs text-rose-700 flex items-center gap-2">
              <AlertCircle size={16} className="shrink-0" />
              <span>{createError}</span>
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">First Name *</label>
              <Input
                value={createFirstName}
                onChange={(e) => setCreateFirstName(e.target.value)}
                placeholder="e.g. Alex"
                disabled={creating}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Last Name *</label>
              <Input
                value={createLastName}
                onChange={(e) => setCreateLastName(e.target.value)}
                placeholder="e.g. Mercer"
                disabled={creating}
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Email Address *</label>
            <Input
              type="email"
              value={createEmail}
              onChange={(e) => setCreateEmail(e.target.value)}
              placeholder="e.g. alex.mercer@helpdesk.dev"
              disabled={creating}
              required
            />
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setCreateModalOpen(false)}
              disabled={creating}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              loading={creating}
            >
              {creating ? 'Creating & Sending...' : 'Create Agent'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Edit Agent Profile Modal */}
      <Modal
        open={editModalOpen}
        onClose={() => {
          if (!updating) setEditModalOpen(false);
        }}
        title="Edit Support Agent Profile"
      >
        <form onSubmit={handleUpdateAgent} className="space-y-4">
          {editError && (
            <div className="rounded-md bg-rose-50 border border-rose-200 p-3 text-xs text-rose-700 flex items-center gap-2">
              <AlertCircle size={16} className="shrink-0" />
              <span>{editError}</span>
            </div>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">First Name *</label>
              <Input
                value={editFirstName}
                onChange={(e) => setEditFirstName(e.target.value)}
                disabled={updating}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Last Name *</label>
              <Input
                value={editLastName}
                onChange={(e) => setEditLastName(e.target.value)}
                disabled={updating}
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Email Address *</label>
            <Input
              type="email"
              value={editEmail}
              onChange={(e) => setEditEmail(e.target.value)}
              disabled={updating}
              required
            />
          </div>

          <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setEditModalOpen(false)}
              disabled={updating}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              loading={updating}
            >
              {updating ? 'Saving...' : 'Save Changes'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Confirmation Modal for Activate/Deactivate */}
      <Modal
        open={Boolean(toggleAgent)}
        onClose={() => {
          if (!toggleLoading) setToggleAgent(null);
        }}
        title={toggleAgent?.active !== false ? 'Deactivate Support Agent' : 'Activate Support Agent'}
      >
        {toggleAgent && (
          <div className="space-y-4">
            <div className="flex items-start gap-3">
              <div className={`p-2 rounded-full shrink-0 ${toggleAgent.active !== false ? 'bg-rose-100 text-rose-600' : 'bg-emerald-100 text-emerald-600'}`}>
                {toggleAgent.active !== false ? <ShieldAlert size={20} /> : <CheckCircle2 size={20} />}
              </div>
              <div>
                <p className="text-sm font-semibold text-slate-900">
                  {toggleAgent.active !== false
                    ? `Deactivate ${toggleAgent.firstName} ${toggleAgent.lastName}?`
                    : `Activate ${toggleAgent.firstName} ${toggleAgent.lastName}?`}
                </p>
                <p className="text-xs text-slate-600 mt-1">
                  {toggleAgent.active !== false
                    ? 'This agent will no longer be able to log in or be assigned to new tickets. Previously assigned ticket history will remain preserved.'
                    : 'This agent will be allowed to log in and work on support tickets.'}
                </p>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
              <Button
                type="button"
                variant="outline"
                onClick={() => setToggleAgent(null)}
                disabled={toggleLoading}
              >
                Cancel
              </Button>
              <Button
                type="button"
                variant={toggleAgent.active !== false ? 'danger' : 'primary'}
                loading={toggleLoading}
                onClick={handleConfirmToggle}
              >
                {toggleLoading
                  ? 'Processing...'
                  : toggleAgent.active !== false
                  ? 'Deactivate Agent'
                  : 'Activate Agent'}
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </AppLayout>
  );
}
