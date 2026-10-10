import axios from 'axios';

// Override at build time with VITE_API_BASE (used when we deploy)
export const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080';

const TOKEN_KEY = 'token';
export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const setToken = (token) => localStorage.setItem(TOKEN_KEY, token);
export const clearToken = () => localStorage.removeItem(TOKEN_KEY);

export const api = axios.create({ baseURL: API_BASE });

// Attach the JWT to every request
api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Turn an axios error into a sentence a person can act on
export function errorMessage(err, fallback) {
  if (!err.response) return "Can't reach the server. Check that bpm-service is running.";
  return err.response.data?.error || fallback;
}
