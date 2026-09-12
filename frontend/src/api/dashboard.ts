import { http } from './http';
import type {
  UserDashboardStats,
  AgentDashboardStats,
  AdminDashboardStats,
} from '@/types';

export const dashboardApi = {
  /**
   * Get server-calculated dashboard statistics for the authenticated user.
   * Access: USER
   */
  async getUserDashboard(): Promise<UserDashboardStats> {
    const response = await http.get<UserDashboardStats>('/api/v1/dashboard/user');
    return response.data;
  },

  /**
   * Get server-calculated dashboard statistics for the authenticated support agent.
   * Scoped to tickets assigned to the agent.
   * Access: SUPPORT_AGENT
   */
  async getAgentDashboard(): Promise<AgentDashboardStats> {
    const response = await http.get<AgentDashboardStats>('/api/v1/dashboard/agent');
    return response.data;
  },

  /**
   * Get server-calculated system-wide dashboard statistics.
   * Access: ADMIN
   */
  async getAdminDashboard(): Promise<AdminDashboardStats> {
    const response = await http.get<AdminDashboardStats>('/api/v1/dashboard/admin');
    return response.data;
  },
};
