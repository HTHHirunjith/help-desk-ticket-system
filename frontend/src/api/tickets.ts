import { http } from './http';
import type {
  Category,
  TicketSummary,
  TicketDetail,
  PagedResponse,
  CreateTicketPayload,
  UpdateTicketPayload,
  ChangePriorityPayload,
  AssignTicketPayload,
  Comment,
  CreateCommentPayload,
  AuditEntry,
  TicketFilterParams,
  User,
  UserRole,
} from '@/types';

export const ticketApi = {
  /**
   * List tickets with optional filters and pagination.
   * Scoped by backend role (USER -> own, AGENT -> assigned, ADMIN -> all).
   * Default ordering: updatedAt DESC.
   */
  async getTickets(params?: TicketFilterParams): Promise<PagedResponse<TicketSummary>> {
    const response = await http.get<PagedResponse<TicketSummary>>('/api/v1/tickets', {
      params,
    });
    return response.data;
  },

  /**
   * Get ticket details by ID.
   * Access: USER (own), AGENT (assigned), ADMIN (all). Non-accessible returns 404.
   */
  async getTicketById(ticketId: string): Promise<TicketDetail> {
    const response = await http.get<TicketDetail>(`/api/v1/tickets/${ticketId}`);
    return response.data;
  },

  /**
   * Create a ticket. USER only.
   */
  async createTicket(payload: CreateTicketPayload): Promise<TicketDetail> {
    const response = await http.post<TicketDetail>('/api/v1/tickets', payload);
    return response.data;
  },

  /**
   * Update an OPEN ticket. USER (requester) only.
   */
  async updateOpenTicket(ticketId: string, payload: UpdateTicketPayload): Promise<TicketDetail> {
    const response = await http.patch<TicketDetail>(`/api/v1/tickets/${ticketId}`, payload);
    return response.data;
  },

  /**
   * Change priority. SUPPORT_AGENT (assigned) or ADMIN.
   */
  async changePriority(ticketId: string, payload: ChangePriorityPayload): Promise<TicketDetail> {
    const response = await http.patch<TicketDetail>(`/api/v1/tickets/${ticketId}/priority`, payload);
    return response.data;
  },

  /**
   * Assign or reassign ticket. ADMIN only.
   */
  async assignTicket(ticketId: string, payload: AssignTicketPayload): Promise<TicketDetail> {
    const response = await http.put<TicketDetail>(`/api/v1/tickets/${ticketId}/assignment`, payload);
    return response.data;
  },

  /**
   * Unassign ticket. ADMIN only.
   */
  async unassignTicket(ticketId: string): Promise<void> {
    await http.delete(`/api/v1/tickets/${ticketId}/assignment`);
  },

  /**
   * Start work: OPEN -> IN_PROGRESS. Assigned SUPPORT_AGENT only.
   */
  async startWork(ticketId: string): Promise<TicketDetail> {
    const response = await http.post<TicketDetail>(`/api/v1/tickets/${ticketId}/start`);
    return response.data;
  },

  /**
   * Resolve ticket: IN_PROGRESS -> RESOLVED. Assigned SUPPORT_AGENT only.
   */
  async resolveTicket(ticketId: string): Promise<TicketDetail> {
    const response = await http.post<TicketDetail>(`/api/v1/tickets/${ticketId}/resolve`);
    return response.data;
  },

  /**
   * Confirm resolution: RESOLVED -> confirmed RESOLVED. Requester (USER) only.
   */
  async confirmResolution(ticketId: string): Promise<TicketDetail> {
    const response = await http.post<TicketDetail>(`/api/v1/tickets/${ticketId}/confirm-resolution`);
    return response.data;
  },

  /**
   * Reject resolution: RESOLVED -> OPEN (reopened). Requester (USER) only.
   */
  async rejectResolution(ticketId: string): Promise<TicketDetail> {
    const response = await http.post<TicketDetail>(`/api/v1/tickets/${ticketId}/reject-resolution`);
    return response.data;
  },

  /**
   * Close ticket: confirmed RESOLVED -> CLOSED. ADMIN only.
   */
  async closeTicket(ticketId: string): Promise<TicketDetail> {
    const response = await http.post<TicketDetail>(`/api/v1/tickets/${ticketId}/close`);
    return response.data;
  },
};

export const categoryApi = {
  /**
   * List categories. Optional active filter.
   */
  async getCategories(active?: boolean): Promise<Category[]> {
    const params = active !== undefined ? { active } : undefined;
    const response = await http.get<Category[]>('/api/v1/categories', { params });
    return response.data;
  },
};

export const commentApi = {
  /**
   * List comments on a ticket. Ordered createdAt ASC.
   * Access: USER (own), AGENT (assigned), ADMIN (all).
   */
  async getComments(ticketId: string): Promise<Comment[]> {
    const response = await http.get<Comment[]>(`/api/v1/tickets/${ticketId}/comments`);
    return response.data;
  },

  /**
   * Add a comment to a ticket.
   * Access: USER (own), AGENT (assigned), ADMIN (all).
   * Author is derived from authenticated principal.
   */
  async addComment(ticketId: string, payload: CreateCommentPayload): Promise<Comment> {
    const response = await http.post<Comment>(`/api/v1/tickets/${ticketId}/comments`, payload);
    return response.data;
  },
};

export const auditApi = {
  /**
   * Get audit timeline for a ticket. ADMIN only.
   * Ordered createdAt DESC.
   */
  async getAuditHistory(ticketId: string): Promise<AuditEntry[]> {
    const response = await http.get<AuditEntry[]>(`/api/v1/tickets/${ticketId}/audit`);
    return response.data;
  },
};

export const userApi = {
  /**
   * List users. Optional role and active status filters. ADMIN only.
   */
  async getUsers(params?: { role?: UserRole; active?: boolean }): Promise<User[]> {
    const response = await http.get<User[]>('/api/v1/users', { params });
    return response.data;
  },
};

