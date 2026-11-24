import axios from 'axios';

// Em produção a API é acessada pelo mesmo domínio do frontend (proxy /api/* em vercel.json),
// então o cookie HttpOnly de sessão é first-party e o token nunca fica acessível ao JavaScript.
// Em desenvolvimento, o padrão é a API local; VITE_API_URL sobrescreve nos dois casos.
const baseURL = import.meta.env.VITE_API_URL ?? (import.meta.env.DEV ? 'http://localhost:8080' : '');

const api = axios.create({
  baseURL,
  withCredentials: true,
});

// Interceptor: Trata respostas de erro globais (ex: 429 Rate-limit)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 429 && !error.response.data?.message) {
      error.response.data = {
        ...(error.response.data || {}),
        message: 'Muitas tentativas em pouco tempo. Aguarde alguns instantes antes de tentar novamente.',
      };
    }
    return Promise.reject(error);
  }
);

export default api;