import { useEffect, useState, type FormEvent } from 'react';
import { userApi } from '@/api/tickets';
import { extractErrorMessage } from '@/api/auth';
import { useAuth } from '@/context/AuthContext';
import type { User, UserRole } from '@/types';
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
import { RoleBadge } from '@/components/ui/Badge';
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

export function AdminUsersPage() {
  const { user: currentUser } = useAuth();

  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Global action notifications
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // Filter state
  const [roleFilter, setRoleFilter] = useState<'ALL' | UserRole>('ALL');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');

  // Create privileged user modal state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createFirstName, setCreateFirstName] = useState('');
  const [createLastName, setCreateLastName] = useState('');
  const [createEmail, setCreateEmail] = useState('');
  const [createRole, setCreateRole] = useState<'SUPPORT_AGENT' | 'ADMIN'>('SUPPORT_AGENT');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  // Edit user profile modal state
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [editFirstName, setEditFirstName] = useState('');
  const [editLastName, setEditLastName] = useState('');
  const [editEmail, setEditEmail] = useState('');
  const [updating, setUpdating] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  // Deactivate/Activate confirmation state
  const [toggleUser, setToggleUser] = useState<User | null>(null);
  const [toggleLoading, setToggleLoading] = useState(false);

  const fetchUsers = async () => {
    setLoading(true);
    setError(null);
    try {
      const allUsers = await userApi.getUsers();
      setUsers(allUsers);
    } catch (err) {
      setError(extractErrorMessage(err, 'Failed to load users.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  // Handle Create Privileged Account
  const handleOpenCreateModal = () => {
    setCreateFirstName('');
    setCreateLastName('');
    setCreateEmail('');
    setCreateRole('SUPPORT_AGENT');
    setCreateError(null);
    setCreateModalOpen(true);
  };

  const handleCreateUser = async (e: FormEvent) => {
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
      const newUser = await userApi.createUser({
        firstName: createFirstName.trim(),
        lastName: createLastName.trim(),
        email: createEmail.trim(),
        role: createRole,
      });

      setUsers((prev) => [...prev, newUser].sort((a, b) => a.firstName.localeCompare(b.firstName)));
      setCreateModalOpen(false);
      setActionSuccess(
        `Account for ${newUser.firstName} ${newUser.lastName} created successfully. Temporary credentials have been emailed to ${newUser.email}.`
      );
    } catch (err) {
      setCreateError(extractErrorMessage(err, 'Failed to create privileged account.'));
    } finally {
      setCreating(false);
    }
  };

  // Handle Edit Profile
  const handleOpenEditModal = (targetUser: User) => {
    setEditingUser(targetUser);
    setEditFirstName(targetUser.firstName);
    setEditLastName(targetUser.lastName);
    setEditEmail(targetUser.email);
    setEditError(null);
    setEditModalOpen(true);
  };

  const handleUpdateUser = async (e: FormEvent) => {
    e.preventDefault();
    if (!editingUser) return;

    if (!editFirstName.trim() || !editLastName.trim() || !editEmail.trim()) {
      setEditError('First name, last name, and email cannot be empty.');
      return;
    }

    setUpdating(true);
    setEditError(null);
    setActionSuccess(null);
    setActionError(null);

    try {
      const updated = await userApi.updateUser(editingUser.id, {
        firstName: editFirstName.trim(),
        lastName: editLastName.trim(),
        email: editEmail.trim(),
      });

      setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
      setEditModalOpen(false);
      setActionSuccess(`Profile for ${updated.firstName} ${updated.lastName} updated successfully.`);
    } catch (err) {
      setEditError(extractErrorMessage(err, 'Failed to update user profile.'));
    } finally {
      setUpdating(false);
    }
  };

  // Handle Activate / Deactivate Toggle
  const handleOpenToggleModal = (targetUser: User) => {
    setActionError(null);
    setActionSuccess(null);
    setToggleUser(targetUser);
  };

  const handleConfirmToggle = async () => {
    if (!toggleUser) return;

    setToggleLoading(true);
    setActionError(null);
    setActionSuccess(null);

    try {
      let updated: User;
      if (toggleUser.active !== false) {
        updated = await userApi.deactivateUser(toggleUser.id);
        setActionSuccess(`User ${updated.firstName} ${updated.lastName} has been deactivated.`);
      } else {
        updated = await userApi.activateUser(toggleUser.id);
        setActionSuccess(`User ${updated.firstName} ${updated.lastName} has been activated.`);
      }
      setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
      setToggleUser(null);
    } catch (err) {
      const msg = extractErrorMessage(err, 'Failed to update user status.');
      setActionError(msg);
      setToggleUser(null);
    } finally {
      setToggleLoading(false);
    }
  };

  // Filtered users list
  const filteredUsers = users.filter((u) => {
    if (roleFilter !== 'ALL' && u.role !== roleFilter) return false;
    const isActive = u.active !== false;
    if (statusFilter === 'ACTIVE' && !isActive) return false;
    if (statusFilter === 'INACTIVE' && isActive) return false;
    return true;
  });

  return (
    <AppLayout>
      <PageHeader
        title="Users"
        description="Manage all registered users, support agents, and administrators."
        action={
          <Button
            variant="primary"
            leftIcon={<UserPlus size={16} />}
            onClick={handleOpenCreateModal}
          >
            Create Privileged Account
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

      {/* Filters Bar */}
      <div className="flex flex-col sm:flex-row gap-3 mb-6 items-start sm:items-center justify-between">
        <div className="flex flex-wrap gap-2">
          {(['ALL', 'USER', 'SUPPORT_AGENT', 'ADMIN'] as const).map((r) => (
            <button
              key={r}
              onClick={() => setRoleFilter(r)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                roleFilter === r
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
              }`}
            >
              {r === 'ALL' ? 'All Roles' : r === 'SUPPORT_AGENT' ? 'Support Agents' : r === 'ADMIN' ? 'Admins' : 'Users'}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-2">
          {(['ALL', 'ACTIVE', 'INACTIVE'] as const).map((s) => (
            <button
              key={s}
              onClick={() => setStatusFilter(s)}
              className={`px-3 py-1 rounded-md text-xs font-medium transition-colors ${
                statusFilter === s
                  ? 'bg-slate-800 text-white'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {s === 'ALL' ? 'All Status' : s === 'ACTIVE' ? 'Active' : 'Inactive'}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <InlineLoader message="Loading users..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchUsers} />
      ) : filteredUsers.length === 0 ? (
        <Card>
          <EmptyState
            icon={<Inbox size={24} />}
            title="No users found"
            description="There are no users matching your selected filters."
          />
        </Card>
      ) : (
        <Card padding={false}>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50">
                  <th className="text-xs font-semibold text-slate-500 uppercase tracking-wider px-4 py-3">User</th>
                  <th className="text-xs font-semibold text-slate-500 uppercase tracking-wider px-4 py-3 hidden sm:table-cell">Email</th>
                  <th className="text-xs font-semibold text-slate-500 uppercase tracking-wider px-4 py-3">Role</th>
                  <th className="text-xs font-semibold text-slate-500 uppercase tracking-wider px-4 py-3">Status</th>
                  <th className="text-xs font-semibold text-slate-500 uppercase tracking-wider px-4 py-3 hidden md:table-cell">Joined</th>
                  <th className="text-right text-xs font-semibold text-slate-500 uppercase tracking-wider px-4 py-3">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredUsers.map((u) => {
                  const isSelf = currentUser?.id === u.id || currentUser?.email === u.email;
                  const isActive = u.active !== false;

                  return (
                    <tr key={u.id} className="hover:bg-slate-50 transition-colors">
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-3">
                          <div className="flex items-center justify-center w-8 h-8 rounded-full bg-slate-200 text-slate-700 text-xs font-semibold shrink-0">
                            {getInitials(u.firstName, u.lastName)}
                          </div>
                          <div>
                            <div className="text-sm font-medium text-slate-900 flex items-center gap-1.5">
                              <span>{u.firstName} {u.lastName}</span>
                              {isSelf && (
                                <span className="text-[10px] bg-indigo-100 text-indigo-700 px-1.5 py-0.5 rounded font-medium">
                                  You
                                </span>
                              )}
                            </div>
                            <span className="text-xs text-slate-500 sm:hidden">{u.email}</span>
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3 hidden sm:table-cell">
                        <span className="text-sm text-slate-600">{u.email}</span>
                      </td>
                      <td className="px-4 py-3">
                        <RoleBadge role={u.role} />
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${
                            isActive
                              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                              : 'bg-rose-50 text-rose-700 border border-rose-200'
                          }`}
                        >
                          <span className={`w-1.5 h-1.5 rounded-full ${isActive ? 'bg-emerald-500' : 'bg-rose-500'}`} />
                          {isActive ? 'Active' : 'Inactive'}
                        </span>
                      </td>
                      <td className="px-4 py-3 hidden md:table-cell">
                        <span className="text-sm text-slate-400">
                          {u.createdAt ? formatDate(u.createdAt) : '—'}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            leftIcon={<Edit2 size={14} />}
                            onClick={() => handleOpenEditModal(u)}
                            aria-label={`Edit ${u.firstName} ${u.lastName}`}
                          >
                            Edit
                          </Button>

                          {isActive ? (
                            <Button
                              variant="ghost"
                              size="sm"
                              leftIcon={<Power size={14} className="text-rose-500" />}
                              onClick={() => handleOpenToggleModal(u)}
                              disabled={isSelf}
                              title={isSelf ? 'You cannot deactivate your own admin account' : 'Deactivate user'}
                              aria-label={`Deactivate ${u.firstName} ${u.lastName}`}
                            >
                              Deactivate
                            </Button>
                          ) : (
                            <Button
                              variant="ghost"
                              size="sm"
                              leftIcon={<Check size={14} className="text-emerald-600" />}
                              onClick={() => handleOpenToggleModal(u)}
                              aria-label={`Activate ${u.firstName} ${u.lastName}`}
                            >
                              Activate
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* Create Privileged Account Modal */}
      <Modal
        open={createModalOpen}
        onClose={() => {
          if (!creating) setCreateModalOpen(false);
        }}
        title="Create Privileged Account"
      >
        <form onSubmit={handleCreateUser} className="space-y-4">
          <div className="rounded-lg bg-indigo-50 border border-indigo-100 p-3 text-xs text-indigo-800 flex items-start gap-2.5">
            <Mail className="h-4 w-4 text-indigo-600 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold mb-0.5">Secure Credentials Provisioning</p>
              <p>A cryptographically secure temporary password will be generated and delivered directly to the user&apos;s email address. The user will be required to change it on their first login.</p>
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
                placeholder="e.g. Jane"
                disabled={creating}
                required
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Last Name *</label>
              <Input
                value={createLastName}
                onChange={(e) => setCreateLastName(e.target.value)}
                placeholder="e.g. Doe"
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
              placeholder="e.g. jane.doe@example.com"
              disabled={creating}
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Role *</label>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setCreateRole('SUPPORT_AGENT')}
                className={`p-3 rounded-lg border text-left transition-all ${
                  createRole === 'SUPPORT_AGENT'
                    ? 'border-indigo-600 bg-indigo-50/50 ring-2 ring-indigo-600/20'
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                <div className="font-semibold text-xs text-slate-900">Support Agent</div>
                <div className="text-[11px] text-slate-500 mt-0.5">Handles assigned tickets & work</div>
              </button>

              <button
                type="button"
                onClick={() => setCreateRole('ADMIN')}
                className={`p-3 rounded-lg border text-left transition-all ${
                  createRole === 'ADMIN'
                    ? 'border-indigo-600 bg-indigo-50/50 ring-2 ring-indigo-600/20'
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                <div className="font-semibold text-xs text-slate-900">Administrator</div>
                <div className="text-[11px] text-slate-500 mt-0.5">Full system management</div>
              </button>
            </div>
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
              {creating ? 'Creating & Sending...' : 'Create Account'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Edit Profile Modal */}
      <Modal
        open={editModalOpen}
        onClose={() => {
          if (!updating) setEditModalOpen(false);
        }}
        title="Edit User Profile"
      >
        <form onSubmit={handleUpdateUser} className="space-y-4">
          {editError && (
            <div className="rounded-md bg-rose-50 border border-rose-200 p-3 text-xs text-rose-700 flex items-center gap-2">
              <AlertCircle size={16} className="shrink-0" />
              <span>{editError}</span>
            </div>
          )}

          {editingUser && (
            <div className="rounded-md bg-slate-50 p-3 flex items-center justify-between border border-slate-200">
              <span className="text-xs text-slate-500">Account Role</span>
              <div className="flex items-center gap-2">
                <RoleBadge role={editingUser.role} />
                <span className="text-[11px] text-slate-400">(Role is immutable)</span>
              </div>
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
        open={Boolean(toggleUser)}
        onClose={() => {
          if (!toggleLoading) setToggleUser(null);
        }}
        title={toggleUser?.active !== false ? 'Deactivate User Account' : 'Activate User Account'}
      >
        {toggleUser && (
          <div className="space-y-4">
            <div className="flex items-start gap-3">
              <div className={`p-2 rounded-full shrink-0 ${toggleUser.active !== false ? 'bg-rose-100 text-rose-600' : 'bg-emerald-100 text-emerald-600'}`}>
                {toggleUser.active !== false ? <ShieldAlert size={20} /> : <CheckCircle2 size={20} />}
              </div>
              <div>
                <p className="text-sm font-semibold text-slate-900">
                  {toggleUser.active !== false
                    ? `Deactivate ${toggleUser.firstName} ${toggleUser.lastName}?`
                    : `Activate ${toggleUser.firstName} ${toggleUser.lastName}?`}
                </p>
                <p className="text-xs text-slate-600 mt-1">
                  {toggleUser.active !== false
                    ? 'This user will no longer be able to log in to the system. Existing tickets and historical audit records will remain preserved.'
                    : 'This user will be allowed to log in and access the help desk system according to their assigned role.'}
                </p>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
              <Button
                type="button"
                variant="outline"
                onClick={() => setToggleUser(null)}
                disabled={toggleLoading}
              >
                Cancel
              </Button>
              <Button
                type="button"
                variant={toggleUser.active !== false ? 'danger' : 'primary'}
                loading={toggleLoading}
                onClick={handleConfirmToggle}
              >
                {toggleLoading
                  ? 'Processing...'
                  : toggleUser.active !== false
                  ? 'Deactivate User'
                  : 'Activate User'}
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </AppLayout>
  );
}
