import type { User, Agent } from '@/types';

export const mockUsers: User[] = [
  {
    id: 'usr-001',
    email: 'john.doe@example.com',
    firstName: 'John',
    lastName: 'Doe',
    role: 'USER',
    createdAt: '2025-01-15T09:00:00Z',
  },
  {
    id: 'usr-002',
    email: 'jane.smith@example.com',
    firstName: 'Jane',
    lastName: 'Smith',
    role: 'USER',
    createdAt: '2025-02-20T09:00:00Z',
  },
  {
    id: 'usr-003',
    email: 'agent.mike@example.com',
    firstName: 'Mike',
    lastName: 'Johnson',
    role: 'SUPPORT_AGENT',
    createdAt: '2024-11-10T09:00:00Z',
  },
  {
    id: 'usr-004',
    email: 'agent.sarah@example.com',
    firstName: 'Sarah',
    lastName: 'Williams',
    role: 'SUPPORT_AGENT',
    createdAt: '2024-12-05T09:00:00Z',
  },
  {
    id: 'usr-005',
    email: 'admin@helpdesk.com',
    firstName: 'Admin',
    lastName: 'Root',
    role: 'ADMIN',
    createdAt: '2024-10-01T09:00:00Z',
  },
];

export const mockAgents: Agent[] = [
  {
    id: 'agt-001',
    userId: 'usr-003',
    firstName: 'Mike',
    lastName: 'Johnson',
    email: 'agent.mike@example.com',
    role: 'SUPPORT_AGENT',
    activeTickets: 4,
    resolvedTickets: 127,
    status: 'BUSY',
    createdAt: '2024-11-10T09:00:00Z',
  },
  {
    id: 'agt-002',
    userId: 'usr-004',
    firstName: 'Sarah',
    lastName: 'Williams',
    email: 'agent.sarah@example.com',
    role: 'SUPPORT_AGENT',
    activeTickets: 2,
    resolvedTickets: 89,
    status: 'AVAILABLE',
    createdAt: '2024-12-05T09:00:00Z',
  },
];
