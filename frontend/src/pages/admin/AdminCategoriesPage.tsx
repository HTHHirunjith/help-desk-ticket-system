import { useEffect, useState } from 'react';
import { categoryApi } from '@/api/tickets';
import { extractErrorMessage } from '@/api/auth';
import type { Category } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, InlineLoader, EmptyState, ErrorState } from '@/components/ui';
import { FolderCog, Inbox } from 'lucide-react';

export function AdminCategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchCategories = async () => {
    setLoading(true);
    setError(null);
    try {
      // Load all categories from backend
      const allCategories = await categoryApi.getCategories();
      setCategories(allCategories);
    } catch (err) {
      setError(extractErrorMessage(err, 'Failed to load categories.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  return (
    <AppLayout>
      <PageHeader
        title="Categories"
        description="Ticket categories configured in the system."
      />

      {loading ? (
        <InlineLoader message="Loading categories..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchCategories} />
      ) : categories.length === 0 ? (
        <Card>
          <EmptyState
            icon={<Inbox size={24} />}
            title="No categories"
            description="There are no ticket categories configured."
          />
        </Card>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {categories.map((cat) => (
            <Card key={cat.id}>
              <div className="flex items-start gap-3">
                <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-slate-100 text-slate-600 shrink-0">
                  <FolderCog size={18} />
                </div>
                <div className="flex-1">
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-semibold text-slate-900">{cat.name.replace(/_/g, ' ')}</p>
                    <span
                      className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium border ${
                        cat.active
                          ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                          : 'bg-slate-100 text-slate-500 border-slate-200'
                      }`}
                    >
                      {cat.active ? 'Active' : 'Inactive'}
                    </span>
                  </div>
                  <p className="text-xs text-slate-500 mt-1">{cat.description}</p>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </AppLayout>
  );
}
