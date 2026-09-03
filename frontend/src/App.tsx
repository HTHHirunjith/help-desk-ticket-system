import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from '@/context/AuthContext';
import { ProtectedRoute, PublicOnlyRoute } from '@/components/routing/ProtectedRoute';
import { LoginPage } from '@/pages/auth/LoginPage';
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
import { AdminAgentsPage } from '@/pages/admin/AdminAgentsPage';
import { AdminCategoriesPage } from '@/pages/admin/AdminCategoriesPage';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public */}
          <Route path="/login" element={<PublicOnlyRoute><LoginPage /></PublicOnlyRoute>} />

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
          <Route path="/admin/agents" element={<ProtectedRoute allowedRoles={['ADMIN']}><AdminAgentsPage /></ProtectedRoute>} />
          <Route path="/admin/categories" element={<ProtectedRoute allowedRoles={['ADMIN']}><AdminCategoriesPage /></ProtectedRoute>} />

          {/* Fallback */}
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
