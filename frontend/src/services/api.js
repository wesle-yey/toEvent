import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080', // URL do seu backend Spring Boot
});

// Interceptor: Adiciona o token JWT no header de cada requisição, se ele existir
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

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