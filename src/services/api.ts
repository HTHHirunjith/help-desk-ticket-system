import type {
  Ticket,
  TicketStatus,
  TicketPriority,
  CreateTicketPayload,
  UpdateTicketPayload,
  Comment,
  Agent,
  Category,
  User,
} from '@/types';
import {
  mockTickets,
  mockAgents,
  mockCategories,
  mockUsers,
  statusTransitions,
} from './mockData';

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

let tickets: Ticket[] = [...mockTickets];
const agents: Agent[] = [...mockAgents];
const categories: Category[] = [...mockCategories];
const users: User[] = [...mockUsers];

let ticketCounter = 11;

const sortByUpdatedDesc = (list: Ticket[]) =>
  [...list].sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());

export const ticketService = {
  async getTickets(): Promise<Ticket[]> {
    await delay(300);
    return sortByUpdatedDesc(tickets);
  },

  async getTicketById(id: string): Promise<Ticket | null> {
    await delay(200);
    return tickets.find((t) => t.id === id) ?? null;
  },

  async getTicketsByRequester(requesterId: string): Promise<Ticket[]> {
    await delay(300);
    return sortByUpdatedDesc(tickets.filter((t) => t.requesterId === requesterId));
  },

  async getTicketsByAgent(agentId: string): Promise<Ticket[]> {
    await delay(300);
    return sortByUpdatedDesc(tickets.filter((t) => t.assignedAgentId === agentId));
  },

  async createTicket(payload: CreateTicketPayload, requester: User): Promise<Ticket> {
    await delay(400);
    const now = new Date().toISOString();
    const ticketNumber = `TKT-2025-${String(ticketCounter++).padStart(3, '0')}`;
    const newTicket: Ticket = {
      id: `tkt-${ticketCounter}`,
      ticketNumber,
      title: payload.title,
      description: payload.description,
      category: payload.category,
      priority: payload.priority,
      status: 'OPEN',
      requesterId: requester.id,
      requesterName: `${requester.firstName} ${requester.lastName}`,
      assignedAgentId: null,
      assignedAgentName: null,
      comments: [],
      createdAt: now,
      updatedAt: now,
    };
    tickets = [newTicket, ...tickets];
    return newTicket;
  },

  async updateTicket(id: string, payload: UpdateTicketPayload): Promise<Ticket | null> {
    await delay(300);
    const idx = tickets.findIndex((t) => t.id === id);
    if (idx === -1) return null;
    const updated: Ticket = {
      ...tickets[idx],
      ...payload,
      updatedAt: new Date().toISOString(),
    };
    if (payload.assignedAgentId) {
      const agent = agents.find((a) => a.userId === payload.assignedAgentId);
      if (agent) {
        updated.assignedAgentName = `${agent.firstName} ${agent.lastName}`;
      }
    }
    tickets[idx] = updated;
    return updated;
  },

  async addComment(
    ticketId: string,
    authorId: string,
    authorName: string,
    authorRole: User['role'],
    body: string
  ): Promise<Comment | null> {
    await delay(300);
    const idx = tickets.findIndex((t) => t.id === ticketId);
    if (idx === -1) return null;
    const comment: Comment = {
      id: `cmt-${Date.now()}`,
      ticketId,
      authorId,
      authorName,
      authorRole,
      body,
      createdAt: new Date().toISOString(),
    };
    tickets[idx] = {
      ...tickets[idx],
      comments: [...tickets[idx].comments, comment],
      updatedAt: comment.createdAt,
    };
    return comment;
  },

  async getStatusTransitions(currentStatus: TicketStatus): Promise<TicketStatus[]> {
    await delay(100);
    return statusTransitions[currentStatus] ?? [];
  },

  async getStats(): Promise<{
    total: number;
    open: number;
    inProgress: number;
    resolved: number;
    closed: number;
    urgent: number;
    high: number;
    medium: number;
    low: number;
  }> {
    await delay(200);
    return {
      total: tickets.length,
      open: tickets.filter((t) => t.status === 'OPEN').length,
      inProgress: tickets.filter((t) => t.status === 'IN_PROGRESS').length,
      resolved: tickets.filter((t) => t.status === 'RESOLVED').length,
      closed: tickets.filter((t) => t.status === 'CLOSED').length,
      urgent: tickets.filter((t) => t.priority === 'URGENT').length,
      high: tickets.filter((t) => t.priority === 'HIGH').length,
      medium: tickets.filter((t) => t.priority === 'MEDIUM').length,
      low: tickets.filter((t) => t.priority === 'LOW').length,
    };
  },

  async getAgentStats(agentId: string): Promise<{
    assigned: number;
    open: number;
    inProgress: number;
    resolved: number;
    closed: number;
  }> {
    await delay(200);
    const agentTickets = tickets.filter((t) => t.assignedAgentId === agentId);
    return {
      assigned: agentTickets.length,
      open: agentTickets.filter((t) => t.status === 'OPEN').length,
      inProgress: agentTickets.filter((t) => t.status === 'IN_PROGRESS').length,
      resolved: agentTickets.filter((t) => t.status === 'RESOLVED').length,
      closed: agentTickets.filter((t) => t.status === 'CLOSED').length,
    };
  },
};

export const agentService = {
  async getAgents(): Promise<Agent[]> {
    await delay(300);
    return agents;
  },

  async getAgentById(id: string): Promise<Agent | null> {
    await delay(200);
    return agents.find((a) => a.id === id) ?? null;
  },
};

export const categoryService = {
  async getCategories(): Promise<Category[]> {
    await delay(300);
    return categories;
  },
};

export const userService = {
  async getUsers(): Promise<User[]> {
    await delay(300);
    return users;
  },
};

export type { TicketPriority };
