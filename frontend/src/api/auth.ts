import { http } from './http';
import type { LoginCredentials, AuthResponse, RegisterPayload, User } from '@/types';
import axios from 'axios';

export function extractErrorMessage(error: unknown, fallback = 'An unexpected error occurred.'): string {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status;
    const data = error.response?.data;

    if (status === 401) {
      return 'Invalid email or password.';
    }

    if (status === 403) {
      return 'You do not have permission to perform this action.';
    }

    if (status === 409) {
      if (data && typeof data === 'object' && 'message' in data && typeof data.message === 'string' && data.message.trim()) {
        return data.message;
      }
      return 'An account with this email address already exists.';
    }

    if (status && status >= 400 && status < 500) {
      if (data && typeof data === 'object' && 'message' in data && typeof data.message === 'string' && data.message.trim()) {
        return data.message;
      }
    }

    if (status && status >= 500) {
      return 'A server error occurred. Please try again later.';
    }

    if (error.code === 'ECONNABORTED' || (error.message && error.message.includes('Network Error'))) {
      return 'Unable to connect to the server. Please check your network connection.';
    }
  }

  if (error instanceof Error) {
    return error.message;
  }

  return fallback;
}

export async function loginApi(credentials: LoginCredentials): Promise<AuthResponse> {
  const response = await http.post<AuthResponse>('/api/v1/auth/login', credentials);
  return response.data;
}

export async function registerApi(payload: RegisterPayload): Promise<User> {
  const response = await http.post<User>('/api/v1/auth/register', payload);
  return response.data;
}

export async function getMeApi(): Promise<User> {
  const response = await http.get<User>('/api/v1/auth/me');
  return response.data;
}
