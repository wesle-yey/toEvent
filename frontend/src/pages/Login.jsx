import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      // Ajuste a rota '/auth/login' de acordo com o seu backend Spring Boot
      const response = await api.post('/api/auth/login', { email, password });
      
      // Salva o token JWT retornado pela API
      localStorage.setItem('token', response.data.token);
      
      // Redireciona para o Dashboard privado
      navigate('/dashboard');
    } catch (err) {
      setError('Credenciais inválidas ou erro no servidor.');
    }
  };

  return (
    <div className="flex h-screen items-center justify-center bg-gray-100">
      <form onSubmit={handleLogin} className="w-full max-w-md rounded bg-white p-8 shadow-md">
        <h2 className="mb-6 text-2xl font-bold text-gray-800">Login ao toEvent</h2>
        {error && <p className="mb-4 text-red-500">{error}</p>}
        
        <div className="mb-4">
          <label className="mb-2 block text-sm font-bold text-gray-700">E-mail</label>
          <input
            type="email"
            className="w-full rounded border px-3 py-2"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>

        <div className="mb-6">
          <label className="mb-2 block text-sm font-bold text-gray-700">Senha</label>
          <input
            type="password"
            className="w-full rounded border px-3 py-2"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>

        <button
          type="submit"
          className="w-full rounded bg-blue-600 py-2 font-bold text-white hover:bg-blue-700"
        >
          Entrar
        </button>
      </form>
    </div>
  );
}