import { useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { Button, Input } from '@/components/ui';
import { LifeBuoy, Mail, Lock, User as UserIcon, AlertCircle, Eye, EyeOff } from 'lucide-react';

export function RegisterPage() {
  const { register, isLoading, error, clearError } = useAuth();
  const navigate = useNavigate();

  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [validationErrors, setValidationErrors] = useState<{
    firstName?: string;
    lastName?: string;
    email?: string;
    password?: string;
  }>({});

  const validate = (): boolean => {
    const errors: typeof validationErrors = {};
    if (!firstName.trim()) {
      errors.firstName = 'First name is required.';
    } else if (firstName.trim().length > 100) {
      errors.firstName = 'First name must not exceed 100 characters.';
    }

    if (!lastName.trim()) {
      errors.lastName = 'Last name is required.';
    } else if (lastName.trim().length > 100) {
      errors.lastName = 'Last name must not exceed 100 characters.';
    }

    if (!email.trim()) {
      errors.email = 'Email is required.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      errors.email = 'Please enter a valid email address.';
    } else if (email.trim().length > 320) {
      errors.email = 'Email must not exceed 320 characters.';
    }

    if (!password) {
      errors.password = 'Password is required.';
    } else if (password.length < 6) {
      errors.password = 'Password must be at least 6 characters.';
    } else if (password.length > 100) {
      errors.password = 'Password must not exceed 100 characters.';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    clearError();
    if (!validate()) return;

    try {
      await register({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        password,
      });
      // On success, navigate to login with a user-friendly feedback message
      navigate('/login', {
        replace: true,
        state: {
          successMessage: 'Registration successful! Please sign in with your new account.',
        },
      });
    } catch {
      // Error is captured and exposed via useAuth().error
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
            <h1 className="text-3xl font-bold leading-tight mb-4">
              Join modern teams resolving tickets faster
            </h1>
            <p className="text-slate-400 text-lg leading-relaxed">
              Create an account to submit issues, track progress in real time, and collaborate seamlessly with support agents.
            </p>
          </div>
          <div className="flex items-center gap-6 text-sm text-slate-400">
            <span>Fast Setup · Real-time Tracking · Clean Workflows</span>
          </div>
        </div>
      </div>

      {/* Right panel — registration form */}
      <div className="flex-1 flex items-center justify-center p-6 sm:p-12">
        <div className="w-full max-w-sm">
          <div className="lg:hidden flex items-center gap-3 mb-8">
            <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-blue-600 text-white">
              <LifeBuoy size={22} />
            </div>
            <span className="text-lg font-bold text-slate-900">HelpDesk</span>
          </div>

          <h2 className="text-2xl font-bold text-slate-900 mb-2">Create an account</h2>
          <p className="text-sm text-slate-500 mb-8">Enter your details below to register as a user</p>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <Input
                name="firstName"
                type="text"
                label="First name"
                placeholder="Jane"
                leftIcon={<UserIcon size={16} />}
                value={firstName}
                onChange={(e) => {
                  setFirstName(e.target.value);
                  if (validationErrors.firstName) setValidationErrors((p) => ({ ...p, firstName: undefined }));
                }}
                error={validationErrors.firstName}
                autoComplete="given-name"
                disabled={isLoading}
              />

              <Input
                name="lastName"
                type="text"
                label="Last name"
                placeholder="Doe"
                leftIcon={<UserIcon size={16} />}
                value={lastName}
                onChange={(e) => {
                  setLastName(e.target.value);
                  if (validationErrors.lastName) setValidationErrors((p) => ({ ...p, lastName: undefined }));
                }}
                error={validationErrors.lastName}
                autoComplete="family-name"
                disabled={isLoading}
              />
            </div>

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
                placeholder="At least 6 characters"
                leftIcon={<Lock size={16} />}
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  if (validationErrors.password) setValidationErrors((p) => ({ ...p, password: undefined }));
                }}
                error={validationErrors.password}
                autoComplete="new-password"
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
              {isLoading ? 'Creating account...' : 'Create account'}
            </Button>
          </form>

          <p className="mt-8 text-center text-sm text-slate-600">
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-blue-600 hover:text-blue-500 hover:underline">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
