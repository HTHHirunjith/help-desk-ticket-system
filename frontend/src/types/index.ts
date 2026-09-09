export type UserRole = 'USER' | 'SUPPORT_AGENT' | 'ADMIN';

export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export type TicketCategoryName =
  | 'GENERAL'
  | 'TECHNICAL'
  | 'BILLING'
  | 'ACCOUNT'
  | 'BUG_REPORT'
  | 'FEATURE_REQUEST'
  | string;

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  active?: boolean;
  avatarUrl?: string | null;
  createdAt?: string;
}

export interface UserRef {
  id: string;
  name: string;
}

export interface RegisterPayload {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Category {
  id: string;
  name: TicketCategoryName;
  description: string;
  active: boolean;
  ticketCount?: number;
}

export interface TicketSummary {
  id: string;
  ticketNumber: number;
  title: string;
  category: Category;
  priority: TicketPriority;
  status: TicketStatus;
  requester: UserRef;
  assignedAgent: UserRef | null;
  createdAt: string;
  updatedAt: string;
}

export interface TicketDetail {
  id: string;
  ticketNumber: number;
  title: string;
  description: string;
  category: Category;
  priority: TicketPriority;
  status: TicketStatus;
  requester: UserRef;
  assignedAgent: UserRef | null;
  resolutionConfirmedAt: string | null;
  resolutionConfirmedBy: UserRef | null;
  reopenedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CommentAuthorRef {
  id: string;
  name: string;
  role: UserRole;
}

export interface Comment {
  id: string;
  body: string;
  author: CommentAuthorRef;
  createdAt: string;
  updatedAt: string;
}

export type AuditAction =
  | 'TICKET_CREATED'
  | 'TICKET_ASSIGNED'
  | 'TICKET_REASSIGNED'
  | 'TICKET_UNASSIGNED'
  | 'STATUS_CHANGED'
  | 'PRIORITY_CHANGED'
  | 'TICKET_REOPENED'
  | 'RESOLUTION_CONFIRMED'
  | 'TICKET_CLOSED'
  | 'COMMENT_ADDED'
  | 'CATEGORY_CHANGED';

export interface AuditActorRef {
  id: string;
  name: string;
  email: string;
  role: UserRole;
}

export interface AuditEntry {
  id: string;
  action: AuditAction;
  actor: AuditActorRef;
  details: string | null;
  createdAt: string;
}

export interface PagedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface CreateTicketPayload {
  title: string;
  description: string;
  categoryId: string;
  priority: TicketPriority;
}

export interface UpdateTicketPayload {
  title?: string;
  description?: string;
  categoryId?: string;
}

export interface ChangePriorityPayload {
  priority: TicketPriority;
}

export interface AssignTicketPayload {
  agentId: string;
}

export interface CreateCommentPayload {
  body: string;
}

export interface TicketFilterParams {
  page?: number;
  size?: number;
  status?: TicketStatus;
  priority?: TicketPriority;
  categoryId?: string;
}

export interface CreateCategoryPayload {
  name: string;
  description?: string;
}

export interface UpdateCategoryPayload {
  name?: string;
  description?: string;
}

// Retained for backward-compatibility with UI components
export type Ticket = TicketSummary;
export type TicketCategory = TicketCategoryName;

export interface Agent {
  id: string;
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  role: UserRole;
  activeTickets: number;
  resolvedTickets: number;
  status: 'AVAILABLE' | 'BUSY' | 'OFFLINE';
  createdAt: string;
}
