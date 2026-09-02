import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react';
import type { User, LoginCredentials, AuthResponse } from '@/types';
import { mockUsers } from '@/services/mockData';

interface AuthContextValue {
  user: User | null;
  token: string | null;
  isLoading: boolean;
  error: string | null;
  login: (credentials: LoginCredentials) => Promise<void>;
  logout: () => void;
  clearError: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const STORAGE_KEY = 'helpdesk_auth';

function loadStoredAuth(): AuthResponse | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    return JSON.parse(raw) as AuthResponse;
  } catch {
    return null;
  }
}

function storeAuth(auth: AuthResponse): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
}

function clearStoredAuth(): void {
  localStorage.removeItem(STORAGE_KEY);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isInitializing, setIsInitializing] = useState(true);

  useEffect(() => {
    const stored = loadStoredAuth();
    if (stored) {
      setUser(stored.user);
      setToken(stored.token);
    }
    setIsInitializing(false);
  }, []);

  const login = useCallback(async (credentials: LoginCredentials) => {
    setIsLoading(true);
    setError(null);
    try {
      await new Promise((resolve) => setTimeout(resolve, 600));

      const matched = mockUsers.find((u) => u.email.toLowerCase() === credentials.email.toLowerCase());
      if (!matched) {
        throw new Error('Invalid email or password.');
      }

      if (credentials.password.length < 4) {
        throw new Error('Invalid email or password.');
      }

      const authResponse: AuthResponse = {
        token: `mock-jwt-${matched.id}-${Date.now()}`,
        user: matched,
      };

      storeAuth(authResponse);
      setUser(matched);
      setToken(authResponse.token);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'An unexpected error occurred.';
      setError(message);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    clearStoredAuth();
    setUser(null);
    setToken(null);
  }, []);

  const clearError = useCallback(() => setError(null), []);

  const value: AuthContextValue = {
    user,
    token,
    isLoading,
    error,
    login,
    logout,
    clearError,
  };

  if (isInitializing) {
    return null;
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
