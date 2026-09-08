import type { User, Agent } from '@/types';
import { mockAgents, mockUsers } from './mockData';

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

const agents: Agent[] = [...mockAgents];
const users: User[] = [...mockUsers];

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

export const userService = {
  async getUsers(): Promise<User[]> {
    await delay(300);
    return users;
  },
};
