import { useEffect, useState } from 'react';
import { categoryService } from '@/services';
import type { Category } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { PageHeader } from '@/components/layout/PageHeader';
import { Card, InlineLoader, EmptyState } from '@/components/ui';
import { FolderCog, Inbox } from 'lucide-react';

export function AdminCategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      setLoading(true);
      const allCategories = await categoryService.getCategories();
      setCategories(allCategories);
      setLoading(false);
    })();
  }, []);

  return (
    <AppLayout>
      <PageHeader
        title="Categories"
        description="Manage support ticket categories."
      />

      {loading ? (
        <InlineLoader message="Loading categories..." />
      ) : categories.length === 0 ? (
        <Card>
          <EmptyState icon={<Inbox size={24} />} title="No categories" description="There are no ticket categories configured." />
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
                  <p className="text-sm font-semibold text-slate-900">{cat.name.replace(/_/g, ' ')}</p>
                  <p className="text-xs text-slate-500 mt-0.5">{cat.description}</p>
                  <p className="text-xs text-slate-400 mt-2">{cat.ticketCount} tickets</p>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </AppLayout>
  );
}
