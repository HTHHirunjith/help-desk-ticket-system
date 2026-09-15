import type { UserRole } from '@/types';
import {
  LayoutDashboard,
  Ticket as TicketIcon,
  PlusCircle,
  Users,
  FolderCog,
  type LucideIcon,
} from 'lucide-react';

export interface NavItem {
  label: string;
  path: string;
  icon: LucideIcon;
}

export const navConfig: Record<UserRole, NavItem[]> = {
  USER: [
    { label: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
    { label: 'My Tickets', path: '/my-tickets', icon: TicketIcon },
    { label: 'Create Ticket', path: '/create-ticket', icon: PlusCircle },
  ],
  SUPPORT_AGENT: [
    { label: 'Dashboard', path: '/agent/dashboard', icon: LayoutDashboard },
    { label: 'Assigned Tickets', path: '/agent/tickets', icon: TicketIcon },
  ],
  ADMIN: [
    { label: 'Dashboard', path: '/admin/dashboard', icon: LayoutDashboard },
    { label: 'Tickets', path: '/admin/tickets', icon: TicketIcon },
    { label: 'Users', path: '/admin/users', icon: Users },
    { label: 'Categories', path: '/admin/categories', icon: FolderCog },
  ],
};

export const roleLabels: Record<UserRole, string> = {
  USER: 'User',
  SUPPORT_AGENT: 'Support Agent',
  ADMIN: 'Administrator',
};

export const roleHomePaths: Record<UserRole, string> = {
  USER: '/dashboard',
  SUPPORT_AGENT: '/agent/dashboard',
  ADMIN: '/admin/dashboard',
};
