import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from '@/context/AuthContext';
import { ProtectedRoute, PublicOnlyRoute } from '@/components/routing/ProtectedRoute';
import { LoginPage } from '@/pages/auth/LoginPage';
import { RegisterPage } from '@/pages/auth/RegisterPage';
import { ChangePasswordPage } from '@/pages/auth/ChangePasswordPage';
import { UserDashboard } from '@/pages/user/UserDashboard';
import { MyTicketsPage } from '@/pages/user/MyTicketsPage';
import { CreateTicketPage } from '@/pages/user/CreateTicketPage';
import { TicketDetailPage } from '@/pages/user/TicketDetailPage';
import { AgentDashboard } from '@/pages/agent/AgentDashboard';
import { AssignedTicketsPage } from '@/pages/agent/AssignedTicketsPage';
import { TicketWorkspacePage } from '@/pages/agent/TicketWorkspacePage';
import { AdminDashboard } from '@/pages/admin/AdminDashboard';
import { AdminTicketsPage } from '@/pages/admin/AdminTicketsPage';
import { AdminUsersPage } from '@/pages/admin/AdminUsersPage';
import { AdminCategoriesPage } from '@/pages/admin/AdminCategoriesPage';
import { useAuth } from '@/context/AuthContext';
import { roleHomePaths } from '@/config/navigation';

/**
 * Route guard for /change-password:
 * - Unauthenticated users → /login
 * - Authenticated users who no longer need a password change → their home dashboard
 * - Authenticated users with mustChangePassword=true → render the page
 */
function ChangePasswordRoute() {
  const { user, token } = useAuth();
  if (!user || !token) {
    return <Navigate to="/login" replace />;
  }
  if (!user.mustChangePassword) {
    return <Navigate to={roleHomePaths[user.role]} replace />;
  }
  return <ChangePasswordPage />;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public */}
          <Route path="/login" element={<PublicOnlyRoute><LoginPage /></PublicOnlyRoute>} />
          <Route path="/register" element={<PublicOnlyRoute><RegisterPage /></PublicOnlyRoute>} />

          {/* Forced password change — accessible to authenticated users with mustChangePassword=true */}
          <Route path="/change-password" element={<ChangePasswordRoute />} />

          {/* USER routes */}
          <Route path="/dashboard" element={<ProtectedRoute allowedRoles={['USER']}><UserDashboard /></ProtectedRoute>} />
          <Route path="/my-tickets" element={<ProtectedRoute allowedRoles={['USER']}><MyTicketsPage /></ProtectedRoute>} />
          <Route path="/create-ticket" element={<ProtectedRoute allowedRoles={['USER']}><CreateTicketPage /></ProtectedRoute>} />
          <Route path="/tickets/:id" element={<ProtectedRoute allowedRoles={['USER']}><TicketDetailPage /></ProtectedRoute>} />

          {/* AGENT routes */}
          <Route path="/agent/dashboard" element={<ProtectedRoute allowedRoles={['SUPPORT_AGENT']}><AgentDashboard /></ProtectedRoute>} />
          <Route path="/agent/tickets" element={<ProtectedRoute allowedRoles={['SUPPORT_AGENT']}><AssignedTicketsPage /></ProtectedRoute>} />
          <Route path="/agent/tickets/:id" element={<ProtectedRoute allowedRoles={['SUPPORT_AGENT']}><TicketWorkspacePage /></ProtectedRoute>} />

          {/* ADMIN routes */}
          <Route path="/admin/dashboard" element={<ProtectedRoute allowedRoles={['ADMIN']}><AdminDashboard /></ProtectedRoute>} />
          <Route path="/admin/tickets" element={<ProtectedRoute allowedRoles={['ADMIN']}><AdminTicketsPage /></ProtectedRoute>} />
          <Route path="/admin/tickets/:id" element={<ProtectedRoute allowedRoles={['ADMIN', 'SUPPORT_AGENT']}><TicketWorkspacePage /></ProtectedRoute>} />
          <Route path="/admin/users" element={<ProtectedRoute allowedRoles={['ADMIN']}><AdminUsersPage /></ProtectedRoute>} />
          <Route path="/admin/agents" element={<Navigate to="/admin/users" replace />} />
          <Route path="/admin/categories" element={<ProtectedRoute allowedRoles={['ADMIN']}><AdminCategoriesPage /></ProtectedRoute>} />

          {/* Fallback */}
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
