import { useEffect, useState, type FormEvent } from 'react';
import { categoryApi } from '@/api/tickets';
import { extractErrorMessage } from '@/api/auth';
import type { Category } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { PageHeader } from '@/components/layout/PageHeader';
import {
  Card,
  InlineLoader,
  EmptyState,
  ErrorState,
  Button,
  Input,
  Textarea,
  Modal,
} from '@/components/ui';
import {
  FolderCog,
  Inbox,
  PlusCircle,
  Edit2,
  CheckCircle2,
  AlertCircle,
  Power,
  Check,
} from 'lucide-react';

export function AdminCategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Global action notifications
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // Create modal state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createName, setCreateName] = useState('');
  const [createDescription, setCreateDescription] = useState('');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  // Edit modal state
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<Category | null>(null);
  const [editName, setEditName] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [updating, setUpdating] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  // Deactivate/Activate confirmation state
  const [toggleCategory, setToggleCategory] = useState<Category | null>(null);
  const [toggleLoading, setToggleLoading] = useState(false);

  const fetchCategories = async () => {
    setLoading(true);
    setError(null);
    try {
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

  // Handle Create
  const handleOpenCreateModal = () => {
    setCreateName('');
    setCreateDescription('');
    setCreateError(null);
    setCreateModalOpen(true);
  };

  const handleCreateCategory = async (e: FormEvent) => {
    e.preventDefault();
    if (!createName.trim()) {
      setCreateError('Category name is required.');
      return;
    }

    setCreating(true);
    setCreateError(null);
    setActionSuccess(null);
    setActionError(null);

    try {
      const newCategory = await categoryApi.createCategory({
        name: createName.trim(),
        description: createDescription.trim(),
      });
      setCategories((prev) => [...prev, newCategory].sort((a, b) => a.name.localeCompare(b.name)));
      setCreateModalOpen(false);
      setActionSuccess(`Category "${newCategory.name}" created successfully.`);
    } catch (err) {
      setCreateError(extractErrorMessage(err, 'Failed to create category.'));
    } finally {
      setCreating(false);
    }
  };

  // Handle Edit
  const handleOpenEditModal = (category: Category) => {
    setEditingCategory(category);
    setEditName(category.name);
    setEditDescription(category.description || '');
    setEditError(null);
    setEditModalOpen(true);
  };

  const handleUpdateCategory = async (e: FormEvent) => {
    e.preventDefault();
    if (!editingCategory) return;

    if (!editName.trim()) {
      setEditError('Category name cannot be blank.');
      return;
    }

    setUpdating(true);
    setEditError(null);
    setActionSuccess(null);
    setActionError(null);

    try {
      const updated = await categoryApi.updateCategory(editingCategory.id, {
        name: editName.trim(),
        description: editDescription.trim(),
      });
      setCategories((prev) =>
        prev.map((c) => (c.id === updated.id ? updated : c)).sort((a, b) => a.name.localeCompare(b.name))
      );
      setEditModalOpen(false);
      setActionSuccess(`Category "${updated.name}" updated successfully.`);
    } catch (err) {
      setEditError(extractErrorMessage(err, 'Failed to update category.'));
    } finally {
      setUpdating(false);
    }
  };

  // Handle Status Toggle (Activate / Deactivate)
  const handleOpenToggleModal = (category: Category) => {
    setToggleCategory(category);
    setActionSuccess(null);
    setActionError(null);
  };

  const handleConfirmToggle = async () => {
    if (!toggleCategory) return;
    setToggleLoading(true);
    setActionSuccess(null);
    setActionError(null);

    try {
      let updated: Category;
      if (toggleCategory.active) {
        updated = await categoryApi.deactivateCategory(toggleCategory.id);
        setActionSuccess(`Category "${updated.name}" deactivated.`);
      } else {
        updated = await categoryApi.activateCategory(toggleCategory.id);
        setActionSuccess(`Category "${updated.name}" activated.`);
      }
      setCategories((prev) => prev.map((c) => (c.id === updated.id ? updated : c)));
      setToggleCategory(null);
    } catch (err) {
      setActionError(extractErrorMessage(err, 'Failed to change category status.'));
      setToggleCategory(null);
    } finally {
      setToggleLoading(false);
    }
  };

  return (
    <AppLayout>
      <PageHeader
        title="Category Management"
        description="Configure and manage ticket categories available across the system."
        action={
          <Button leftIcon={<PlusCircle size={18} />} onClick={handleOpenCreateModal}>
            New Category
          </Button>
        }
      />

      {/* Global Alerts */}
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
            action={
              <Button leftIcon={<PlusCircle size={18} />} onClick={handleOpenCreateModal}>
                Create Category
              </Button>
            }
          />
        </Card>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {categories.map((cat) => (
            <Card key={cat.id} className="flex flex-col justify-between">
              <div>
                <div className="flex items-start justify-between gap-3 mb-3">
                  <div className="flex items-center gap-3">
                    <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-slate-100 text-slate-600 shrink-0">
                      <FolderCog size={20} />
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-slate-900">{cat.name.replace(/_/g, ' ')}</h3>
                      <span className="text-[11px] font-mono text-slate-400">{cat.name}</span>
                    </div>
                  </div>
                  <span
                    className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold border shrink-0 ${
                      cat.active
                        ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                        : 'bg-slate-100 text-slate-500 border-slate-200'
                    }`}
                  >
                    {cat.active ? 'Active' : 'Inactive'}
                  </span>
                </div>
                <p className="text-xs text-slate-600 leading-relaxed mb-4">
                  {cat.description || <span className="italic text-slate-400">No description provided.</span>}
                </p>
              </div>

              {/* Action Buttons */}
              <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                <Button
                  size="sm"
                  variant="outline"
                  leftIcon={<Edit2 size={14} />}
                  onClick={() => handleOpenEditModal(cat)}
                >
                  Edit
                </Button>
                {cat.active ? (
                  <Button
                    size="sm"
                    variant="outline"
                    leftIcon={<Power size={14} />}
                    onClick={() => handleOpenToggleModal(cat)}
                    className="text-amber-700 border-amber-200 hover:bg-amber-50"
                  >
                    Deactivate
                  </Button>
                ) : (
                  <Button
                    size="sm"
                    variant="outline"
                    leftIcon={<Check size={14} />}
                    onClick={() => handleOpenToggleModal(cat)}
                    className="text-emerald-700 border-emerald-200 hover:bg-emerald-50"
                  >
                    Activate
                  </Button>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Create Category Modal */}
      <Modal
        open={createModalOpen}
        onClose={() => !creating && setCreateModalOpen(false)}
        title="Create New Category"
        size="md"
      >
        <form onSubmit={handleCreateCategory} className="space-y-4">
          <Input
            label="Category Name"
            placeholder="e.g. HARDWARE, BILLING, CLOUD_SERVICES"
            value={createName}
            onChange={(e) => {
              setCreateName(e.target.value);
              setCreateError(null);
            }}
            disabled={creating}
            required
          />
          <Textarea
            label="Description"
            placeholder="Describe the types of support requests belonging to this category"
            value={createDescription}
            onChange={(e) => setCreateDescription(e.target.value)}
            disabled={creating}
          />

          {createError && (
            <div className="p-3 bg-red-50 text-xs text-red-700 rounded-lg flex items-center gap-2">
              <AlertCircle size={15} className="shrink-0 text-red-600" />
              <span>{createError}</span>
            </div>
          )}

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setCreateModalOpen(false)}
              disabled={creating}
            >
              Cancel
            </Button>
            <Button type="submit" loading={creating} disabled={!createName.trim()}>
              Create Category
            </Button>
          </div>
        </form>
      </Modal>

      {/* Edit Category Modal */}
      <Modal
        open={editModalOpen}
        onClose={() => !updating && setEditModalOpen(false)}
        title="Edit Category"
        size="md"
      >
        <form onSubmit={handleUpdateCategory} className="space-y-4">
          <Input
            label="Category Name"
            value={editName}
            onChange={(e) => {
              setEditName(e.target.value);
              setEditError(null);
            }}
            disabled={updating}
            required
          />
          <Textarea
            label="Description"
            value={editDescription}
            onChange={(e) => setEditDescription(e.target.value)}
            disabled={updating}
          />

          {editError && (
            <div className="p-3 bg-red-50 text-xs text-red-700 rounded-lg flex items-center gap-2">
              <AlertCircle size={15} className="shrink-0 text-red-600" />
              <span>{editError}</span>
            </div>
          )}

          <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
            <Button
              type="button"
              variant="outline"
              onClick={() => setEditModalOpen(false)}
              disabled={updating}
            >
              Cancel
            </Button>
            <Button type="submit" loading={updating} disabled={!editName.trim()}>
              Save Changes
            </Button>
          </div>
        </form>
      </Modal>

      {/* Status Toggle Confirmation Modal */}
      {toggleCategory && (
        <Modal
          open={!!toggleCategory}
          onClose={() => !toggleLoading && setToggleCategory(null)}
          title={toggleCategory.active ? 'Deactivate Category' : 'Activate Category'}
          size="sm"
        >
          <div className="space-y-4">
            <p className="text-sm text-slate-600">
              {toggleCategory.active ? (
                <>
                  Are you sure you want to deactivate category{' '}
                  <strong className="text-slate-900 font-semibold">{toggleCategory.name}</strong>?
                  <br />
                  <span className="text-xs text-slate-500 mt-2 block">
                    Inactive categories cannot be chosen for new tickets, but existing tickets will retain this category.
                  </span>
                </>
              ) : (
                <>
                  Are you sure you want to activate category{' '}
                  <strong className="text-slate-900 font-semibold">{toggleCategory.name}</strong>?
                  <br />
                  <span className="text-xs text-slate-500 mt-2 block">
                    This category will immediately become selectable when users create new tickets.
                  </span>
                </>
              )}
            </p>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <Button
                variant="outline"
                onClick={() => setToggleCategory(null)}
                disabled={toggleLoading}
              >
                Cancel
              </Button>
              <Button
                loading={toggleLoading}
                onClick={handleConfirmToggle}
                className={
                  toggleCategory.active
                    ? 'bg-amber-600 hover:bg-amber-700 text-white'
                    : 'bg-emerald-600 hover:bg-emerald-700 text-white'
                }
              >
                {toggleCategory.active ? 'Confirm Deactivation' : 'Confirm Activation'}
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </AppLayout>
  );
}
