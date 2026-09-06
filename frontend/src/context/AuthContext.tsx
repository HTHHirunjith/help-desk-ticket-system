import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react';
import type { User, LoginCredentials, RegisterPayload } from '@/types';
import { loginApi, registerApi, getMeApi, extractErrorMessage } from '@/api/auth';
import { TOKEN_STORAGE_KEY } from '@/api/http';
import { FullPageLoader } from '@/components/ui';
import axios from 'axios';

interface AuthContextValue {
  user: User | null;
  token: string | null;
  isLoading: boolean;
  error: string | null;
  login: (credentials: LoginCredentials) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<User>;
  logout: () => void;
  clearError: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_STORAGE_KEY));
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isInitializing, setIsInitializing] = useState(true);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    setUser(null);
    setToken(null);
  }, []);

  // Initialize and restore session from backend /auth/me
  useEffect(() => {
    let isMounted = true;
    const restoreSession = async () => {
      const storedToken = localStorage.getItem(TOKEN_STORAGE_KEY);
      if (!storedToken) {
        if (isMounted) setIsInitializing(false);
        return;
      }

      try {
        const currentUser = await getMeApi();
        if (isMounted) {
          setUser(currentUser);
          setToken(storedToken);
        }
      } catch (err) {
        // Only clear the token if the backend explicitly rejected it with 401 Unauthorized
        if (axios.isAxiosError(err) && err.response?.status === 401) {
          if (isMounted) {
            localStorage.removeItem(TOKEN_STORAGE_KEY);
            setUser(null);
            setToken(null);
          }
        } else {
          // Network error, timeout, 5xx: do NOT delete the token.
          // Keep unauthenticated in memory for now or leave user null without clearing storage.
          if (isMounted) {
            setUser(null);
          }
        }
      } finally {
        if (isMounted) {
          setIsInitializing(false);
        }
      }
    };

    restoreSession();

    // Listen for unauthorized events emitted by http response interceptor
    const handleUnauthorized = () => {
      logout();
    };
    window.addEventListener('auth:unauthorized', handleUnauthorized);

    return () => {
      isMounted = false;
      window.removeEventListener('auth:unauthorized', handleUnauthorized);
    };
  }, [logout]);

  const login = useCallback(async (credentials: LoginCredentials) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await loginApi(credentials);
      localStorage.setItem(TOKEN_STORAGE_KEY, response.token);
      setToken(response.token);
      setUser(response.user);
    } catch (err) {
      const message = extractErrorMessage(err, 'Invalid email or password.');
      setError(message);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const register = useCallback(async (payload: RegisterPayload) => {
    setIsLoading(true);
    setError(null);
    try {
      const createdUser = await registerApi(payload);
      return createdUser;
    } catch (err) {
      const message = extractErrorMessage(err, 'Failed to register.');
      setError(message);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const clearError = useCallback(() => setError(null), []);

  const value: AuthContextValue = {
    user,
    token,
    isLoading,
    error,
    login,
    register,
    logout,
    clearError,
  };

  if (isInitializing) {
    return <FullPageLoader message="Restoring session..." />;
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
