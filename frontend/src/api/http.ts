import axios, { type AxiosError } from 'axios';

export const TOKEN_STORAGE_KEY = 'helpdesk_token';

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: attach Bearer token if present
http.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: handle 401 on authenticated requests
http.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    // If request was to login or register, do not trigger session-expiration event
    const url = error.config?.url || '';
    const isAuthEndpoint = url.includes('/api/v1/auth/login') || url.includes('/api/v1/auth/register');

    if (error.response?.status === 401 && !isAuthEndpoint) {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      window.dispatchEvent(new CustomEvent('auth:unauthorized'));
    }

    return Promise.reject(error);
  }
);
