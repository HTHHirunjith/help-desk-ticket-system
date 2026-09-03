import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { ticketService } from '@/services';
import type { TicketCategory, TicketPriority } from '@/types';
import { AppLayout } from '@/components/layout/AppLayout';
import { PageHeader } from '@/components/layout/PageHeader';
import { Button, Input, Textarea, Select, Card } from '@/components/ui';
import { PriorityBadge } from '@/components/ui/Badge';
import { Send, ArrowLeft } from 'lucide-react';

const categories: TicketCategory[] = ['GENERAL', 'TECHNICAL', 'BILLING', 'ACCOUNT', 'BUG_REPORT', 'FEATURE_REQUEST'];
const priorities: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

export function CreateTicketPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState<TicketCategory>('GENERAL');
  const [priority, setPriority] = useState<TicketPriority>('MEDIUM');
  const [errors, setErrors] = useState<{ title?: string; description?: string }>({});

  const validate = (): boolean => {
    const e: typeof errors = {};
    if (!title.trim()) e.title = 'Title is required.';
    else if (title.trim().length < 5) e.title = 'Title must be at least 5 characters.';
    if (!description.trim()) e.description = 'Description is required.';
    else if (description.trim().length < 20) e.description = 'Description must be at least 20 characters.';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!validate() || !user) return;
    setSubmitting(true);
    try {
      const ticket = await ticketService.createTicket(
        { title: title.trim(), description: description.trim(), category, priority },
        user
      );
      navigate(`/tickets/${ticket.id}`);
    } catch {
      setError('Failed to create ticket. Please try again.');
      setSubmitting(false);
    }
  };

  return (
    <AppLayout>
      <PageHeader
        title="Create Ticket"
        description="Submit a new support ticket and our team will get back to you."
        action={
          <Button variant="ghost" leftIcon={<ArrowLeft size={18} />} onClick={() => navigate('/my-tickets')}>
            Back to Tickets
          </Button>
        }
      />

      <div className="max-w-2xl">
        <Card>
          <form onSubmit={handleSubmit} className="space-y-5">
            <Input
              name="title"
              label="Title"
              placeholder="Briefly describe the issue"
              value={title}
              onChange={(e) => {
                setTitle(e.target.value);
                if (errors.title) setErrors((p) => ({ ...p, title: undefined }));
              }}
              error={errors.title}
              disabled={submitting}
            />

            <Textarea
              name="description"
              label="Description"
              placeholder="Provide a detailed description of your issue. Include any error messages, steps to reproduce, and what you expected to happen."
              value={description}
              onChange={(e) => {
                setDescription(e.target.value);
                if (errors.description) setErrors((p) => ({ ...p, description: undefined }));
              }}
              error={errors.description}
              disabled={submitting}
            />

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
              <Select
                name="category"
                label="Category"
                value={category}
                onChange={(e) => setCategory(e.target.value as TicketCategory)}
                disabled={submitting}
              >
                {categories.map((c) => (
                  <option key={c} value={c}>{c.replace(/_/g, ' ')}</option>
                ))}
              </Select>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Priority</label>
                <div className="flex flex-wrap gap-2">
                  {priorities.map((p) => (
                    <button
                      key={p}
                      type="button"
                      onClick={() => setPriority(p)}
                      disabled={submitting}
                      className={`rounded-full border px-3 py-1 text-xs font-medium transition-all ${
                        priority === p
                          ? 'ring-2 ring-blue-500 ring-offset-1'
                          : 'opacity-60 hover:opacity-100'
                      }`}
                    >
                      <PriorityBadge priority={p} />
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {error && (
              <div className="p-3 rounded-lg bg-red-50 border border-red-200 text-sm text-red-700">
                {error}
              </div>
            )}

            <div className="flex items-center justify-end gap-3 pt-2">
              <Button type="button" variant="outline" onClick={() => navigate('/my-tickets')} disabled={submitting}>
                Cancel
              </Button>
              <Button type="submit" loading={submitting} leftIcon={<Send size={16} />}>
                {submitting ? 'Creating...' : 'Submit Ticket'}
              </Button>
            </div>
          </form>
        </Card>
      </div>
    </AppLayout>
  );
}
