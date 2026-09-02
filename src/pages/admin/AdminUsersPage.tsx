import { useEffect, useState } from 'react';
import { userService } from '@/services';
import type { User } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, InlineLoader, EmptyState } from '@/components/ui';
import { RoleBadge } from '@/components/ui/Badge';
import { formatDate } from '@/utils/format';
import { Inbox } from 'lucide-react';

export function AdminUsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      setLoading(true);
      const allUsers = await userService.getUsers();
      setUsers(allUsers);
      setLoading(false);
    })();
  }, []);

  return (
    <AppLayout>
      <PageHeader
        title="Users"
        description="Manage all registered users in the system."
      />

      {loading ? (
        <InlineLoader message="Loading users..." />
      ) : users.length === 0 ? (
        <Card>
          <EmptyState icon={<Inbox size={24} />} title="No users" description="There are no registered users in the system." />
        </Card>
      ) : (
        <Card padding={false}>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50">
                  <th className="text-left text-xs font-semibold text-slate-500 uppercase tracking-wider px-4 py-3">Name</th>
                  <th className="text-left text-xs font-semibold text-slate-500 uppercase tracking-wider px-4 py-3 hidden sm:table-cell">Email</th>
                  <th className="text-left text-xs font-semibold text-slate-500 uppercase tracking-wider px-4 py-3">Role</th>
                  <th className="text-left text-xs font-semibold text-slate-500 uppercase tracking-wider px-4 py-3 hidden md:table-cell">Joined</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id} className="border-b border-slate-100 last:border-0 hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <div className="flex items-center justify-center w-8 h-8 rounded-full bg-slate-200 text-slate-700 text-xs font-semibold shrink-0">
                          {u.firstName.charAt(0)}{u.lastName.charAt(0)}
                        </div>
                        <span className="text-sm font-medium text-slate-900">{u.firstName} {u.lastName}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 hidden sm:table-cell">
                      <span className="text-sm text-slate-500">{u.email}</span>
                    </td>
                    <td className="px-4 py-3">
                      <RoleBadge role={u.role} />
                    </td>
                    <td className="px-4 py-3 hidden md:table-cell">
                      <span className="text-sm text-slate-400">{formatDate(u.createdAt)}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </AppLayout>
  );
}
