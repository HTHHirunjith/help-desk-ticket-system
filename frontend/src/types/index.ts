export type UserRole = 'USER' | 'SUPPORT_AGENT' | 'ADMIN';

export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export type TicketCategory =
  | 'GENERAL'
  | 'TECHNICAL'
  | 'BILLING'
  | 'ACCOUNT'
  | 'BUG_REPORT'
  | 'FEATURE_REQUEST';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  avatarUrl?: string;
  createdAt: string;
}

export interface RegisterPayload {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

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

export interface Comment {
  id: string;
  ticketId: string;
  authorId: string;
  authorName: string;
  authorRole: UserRole;
  body: string;
  createdAt: string;
}

export interface Ticket {
  id: string;
  ticketNumber: string;
  title: string;
  description: string;
  category: TicketCategory;
  priority: TicketPriority;
  status: TicketStatus;
  requesterId: string;
  requesterName: string;
  assignedAgentId: string | null;
  assignedAgentName: string | null;
  comments: Comment[];
  createdAt: string;
  updatedAt: string;
}

export interface Category {
  id: string;
  name: TicketCategory;
  description: string;
  ticketCount: number;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface CreateTicketPayload {
  title: string;
  description: string;
  category: TicketCategory;
  priority: TicketPriority;
}

export interface UpdateTicketPayload {
  status?: TicketStatus;
  priority?: TicketPriority;
  assignedAgentId?: string | null;
}
