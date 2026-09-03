import { useState, type FormEvent } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { Button, Input } from '@/components/ui';
import { LifeBuoy, Mail, Lock, AlertCircle, Eye, EyeOff } from 'lucide-react';

interface LocationState {
  from?: { pathname: string };
}

export function LoginPage() {
  const { login, isLoading, error, clearError } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const fromPath = (location.state as LocationState)?.from?.pathname;

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [validationErrors, setValidationErrors] = useState<{ email?: string; password?: string }>({});

  const validate = (): boolean => {
    const errors: typeof validationErrors = {};
    if (!email) {
      errors.email = 'Email is required.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      errors.email = 'Please enter a valid email address.';
    }
    if (!password) {
      errors.password = 'Password is required.';
    } else if (password.length < 4) {
      errors.password = 'Password must be at least 4 characters.';
    }
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    clearError();
    if (!validate()) return;
    try {
      await login({ email, password });
      navigate(fromPath || '/dashboard', { replace: true });
    } catch {
      // error is set in context
    }
  };

  return (
    <div className="min-h-screen flex flex-col lg:flex-row bg-slate-50">
      {/* Left panel — branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-slate-900 relative overflow-hidden">
        <div className="absolute inset-0 opacity-5" style={{
          backgroundImage: 'radial-gradient(circle at 25% 25%, white 1px, transparent 1px)',
          backgroundSize: '32px 32px',
        }} />
        <div className="relative z-10 flex flex-col justify-between p-12 text-white w-full">
          <div className="flex items-center gap-3">
            <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-blue-600">
              <LifeBuoy size={22} />
            </div>
            <span className="text-lg font-bold">HelpDesk</span>
          </div>
          <div className="max-w-md">
            <h1 className="text-3xl font-bold leading-tight mb-4">
              Professional support ticket management for modern teams
            </h1>
            <p className="text-slate-400 text-lg leading-relaxed">
              Submit, track, and resolve support tickets with a streamlined workflow built for users, agents, and administrators.
            </p>
          </div>
          <div className="flex items-center gap-6 text-sm text-slate-400">
            <span>Open · In Progress · Resolved · Closed</span>
          </div>
        </div>
      </div>

      {/* Right panel — form */}
      <div className="flex-1 flex items-center justify-center p-6 sm:p-12">
        <div className="w-full max-w-sm">
          <div className="lg:hidden flex items-center gap-3 mb-8">
            <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-blue-600 text-white">
              <LifeBuoy size={22} />
            </div>
            <span className="text-lg font-bold text-slate-900">HelpDesk</span>
          </div>

          <h2 className="text-2xl font-bold text-slate-900 mb-2">Welcome back</h2>
          <p className="text-sm text-slate-500 mb-8">Sign in to your account to continue</p>

          <form onSubmit={handleSubmit} className="space-y-5">
            <Input
              name="email"
              type="email"
              label="Email address"
              placeholder="you@company.com"
              leftIcon={<Mail size={16} />}
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                if (validationErrors.email) setValidationErrors((p) => ({ ...p, email: undefined }));
              }}
              error={validationErrors.email}
              autoComplete="email"
              disabled={isLoading}
            />

            <div className="relative">
              <Input
                name="password"
                type={showPassword ? 'text' : 'password'}
                label="Password"
                placeholder="Enter your password"
                leftIcon={<Lock size={16} />}
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  if (validationErrors.password) setValidationErrors((p) => ({ ...p, password: undefined }));
                }}
                error={validationErrors.password}
                autoComplete="current-password"
                disabled={isLoading}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-[38px] text-slate-400 hover:text-slate-600"
                tabIndex={-1}
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>

            {error && (
              <div className="flex items-start gap-2 p-3 rounded-lg bg-red-50 border border-red-200 text-sm text-red-700">
                <AlertCircle size={16} className="shrink-0 mt-0.5" />
                <span>{error}</span>
              </div>
            )}

            <Button type="submit" fullWidth size="lg" loading={isLoading}>
              {isLoading ? 'Signing in...' : 'Sign in'}
            </Button>
          </form>

          <div className="mt-8 p-4 rounded-lg bg-slate-100 border border-slate-200">
            <p className="text-xs font-semibold text-slate-600 mb-2">Demo Accounts</p>
            <div className="space-y-1 text-xs text-slate-500">
              <p><span className="font-medium text-slate-700">User:</span> john.doe@example.com</p>
              <p><span className="font-medium text-slate-700">Agent:</span> agent.mike@example.com</p>
              <p><span className="font-medium text-slate-700">Admin:</span> admin@helpdesk.com</p>
              <p className="text-slate-400 mt-1">Any password (4+ chars) works.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
