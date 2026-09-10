import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { changePasswordApi, extractErrorMessage } from '@/api/auth';
import { Button, Input } from '@/components/ui';
import { LifeBuoy, Lock, AlertCircle, Eye, EyeOff, ShieldCheck } from 'lucide-react';

export function ChangePasswordPage() {
  const { user, logout, refreshUser } = useAuth();
  const navigate = useNavigate();

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<{
    currentPassword?: string;
    newPassword?: string;
    confirmPassword?: string;
  }>({});

  const validate = (): boolean => {
    const errors: typeof validationErrors = {};
    if (!currentPassword) {
      errors.currentPassword = 'Current password is required.';
    }
    if (!newPassword) {
      errors.newPassword = 'New password is required.';
    } else if (newPassword.length < 8) {
      errors.newPassword = 'New password must be at least 8 characters.';
    } else if (newPassword === currentPassword) {
      errors.newPassword = 'New password must be different from the current password.';
    }
    if (!confirmPassword) {
      errors.confirmPassword = 'Please confirm your new password.';
    } else if (newPassword !== confirmPassword) {
      errors.confirmPassword = 'Passwords do not match.';
    }
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!validate()) return;

    setIsSubmitting(true);
    try {
      await changePasswordApi({ currentPassword, newPassword });
      // Refresh the user in context so mustChangePassword becomes false
      await refreshUser();
      // Navigate to the appropriate dashboard
      const role = user?.role;
      if (role === 'ADMIN') {
        navigate('/admin/dashboard', { replace: true });
      } else if (role === 'SUPPORT_AGENT') {
        navigate('/agent/dashboard', { replace: true });
      } else {
        navigate('/dashboard', { replace: true });
      }
    } catch (err) {
      setError(extractErrorMessage(err, 'Failed to change password. Please try again.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col lg:flex-row bg-slate-50">
      {/* Left panel — branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-slate-900 relative overflow-hidden">
        <div
          className="absolute inset-0 opacity-5"
          style={{
            backgroundImage: 'radial-gradient(circle at 25% 25%, white 1px, transparent 1px)',
            backgroundSize: '32px 32px',
          }}
        />
        <div className="relative z-10 flex flex-col justify-between p-12 text-white w-full">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-blue-600">
              <LifeBuoy size={22} />
            </div>
            <span className="text-lg font-bold">HelpDesk</span>
          </div>
          <div className="max-w-md">
            <ShieldCheck size={48} className="text-blue-400 mb-6" />
            <h1 className="text-3xl font-bold leading-tight mb-4">Secure your account</h1>
            <p className="text-slate-400 text-lg leading-relaxed">
              Your account was provisioned with a temporary password. Choose a strong, unique password
              to protect your account before continuing.
            </p>
          </div>
          <div className="flex items-center gap-6 text-sm text-slate-400">
            <span>Use at least 8 characters · Mix letters, numbers, and symbols</span>
          </div>
        </div>
      </div>

      {/* Right panel — form */}
      <div className="flex-1 flex items-center justify-center p-6 sm:p-12">
        <div className="w-full max-w-sm">
          {/* Mobile logo */}
          <div className="lg:hidden flex items-center gap-3 mb-8">
            <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-blue-600 text-white">
              <LifeBuoy size={22} />
            </div>
            <span className="text-lg font-bold text-slate-900">HelpDesk</span>
          </div>

          {/* Required-change banner */}
          <div className="flex items-start gap-2 p-3 mb-6 rounded-lg bg-amber-50 border border-amber-200 text-sm text-amber-800">
            <ShieldCheck size={16} className="shrink-0 mt-0.5 text-amber-600" />
            <div>
              <p className="font-semibold mb-0.5">Password change required</p>
              <p className="text-xs text-amber-700">
                Your account was provisioned with a temporary password. You must set a new password
                before you can access the application.
              </p>
            </div>
          </div>

          <h2 className="text-2xl font-bold text-slate-900 mb-1">Set a new password</h2>
          {user && (
            <p className="text-sm text-slate-500 mb-6">
              Signed in as <span className="font-medium text-slate-700">{user.email}</span>
            </p>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Current password */}
            <div className="relative">
              <Input
                name="currentPassword"
                type={showCurrent ? 'text' : 'password'}
                label="Temporary / Current password"
                placeholder="Enter your temporary password"
                leftIcon={<Lock size={16} />}
                value={currentPassword}
                onChange={(e) => {
                  setCurrentPassword(e.target.value);
                  if (validationErrors.currentPassword)
                    setValidationErrors((p) => ({ ...p, currentPassword: undefined }));
                }}
                error={validationErrors.currentPassword}
                autoComplete="current-password"
                disabled={isSubmitting}
              />
              <button
                type="button"
                onClick={() => setShowCurrent(!showCurrent)}
                className="absolute right-3 top-[38px] text-slate-400 hover:text-slate-600"
                tabIndex={-1}
              >
                {showCurrent ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>

            {/* New password */}
            <div className="relative">
              <Input
                name="newPassword"
                type={showNew ? 'text' : 'password'}
                label="New password"
                placeholder="At least 8 characters"
                leftIcon={<Lock size={16} />}
                value={newPassword}
                onChange={(e) => {
                  setNewPassword(e.target.value);
                  if (validationErrors.newPassword)
                    setValidationErrors((p) => ({ ...p, newPassword: undefined }));
                }}
                error={validationErrors.newPassword}
                autoComplete="new-password"
                disabled={isSubmitting}
              />
              <button
                type="button"
                onClick={() => setShowNew(!showNew)}
                className="absolute right-3 top-[38px] text-slate-400 hover:text-slate-600"
                tabIndex={-1}
              >
                {showNew ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>

            {/* Confirm password */}
            <div className="relative">
              <Input
                name="confirmPassword"
                type={showConfirm ? 'text' : 'password'}
                label="Confirm new password"
                placeholder="Re-enter your new password"
                leftIcon={<Lock size={16} />}
                value={confirmPassword}
                onChange={(e) => {
                  setConfirmPassword(e.target.value);
                  if (validationErrors.confirmPassword)
                    setValidationErrors((p) => ({ ...p, confirmPassword: undefined }));
                }}
                error={validationErrors.confirmPassword}
                autoComplete="new-password"
                disabled={isSubmitting}
              />
              <button
                type="button"
                onClick={() => setShowConfirm(!showConfirm)}
                className="absolute right-3 top-[38px] text-slate-400 hover:text-slate-600"
                tabIndex={-1}
              >
                {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>

            {error && (
              <div className="flex items-start gap-2 p-3 rounded-lg bg-red-50 border border-red-200 text-sm text-red-700">
                <AlertCircle size={16} className="shrink-0 mt-0.5" />
                <span>{error}</span>
              </div>
            )}

            <Button type="submit" fullWidth size="lg" loading={isSubmitting}>
              {isSubmitting ? 'Changing password...' : 'Set new password'}
            </Button>
          </form>

          <button
            type="button"
            onClick={logout}
            className="mt-6 w-full text-center text-xs text-slate-400 hover:text-slate-600 transition-colors"
          >
            Sign out and return to login
          </button>
        </div>
      </div>
    </div>
  );
}
