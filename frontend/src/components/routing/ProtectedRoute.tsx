import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import type { UserRole } from '@/types';
import { roleHomePaths } from '@/config/navigation';
import { FullPageLoader } from '@/components/ui';

interface ProtectedRouteProps {
  children: ReactNode;
  allowedRoles?: UserRole[];
}

export function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
  const { user, token } = useAuth();
  const location = useLocation();

  if (!user || !token) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to={roleHomePaths[user.role]} replace />;
  }

  return <>{children}</>;
}

export function PublicOnlyRoute({ children }: { children: ReactNode }) {
  const { user, token } = useAuth();

  if (user && token) {
    return <Navigate to={roleHomePaths[user.role]} replace />;
  }

  return <>{children}</>;
}

export function AuthGate({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  if (!user) return <FullPageLoader />;
  return <>{children}</>;
}
