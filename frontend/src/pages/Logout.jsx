import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../services/api';

export default function Logout() {
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    let active = true;

    localStorage.removeItem('token');
    api.post('/api/auth/logout')
      .then(() => {
        if (active) navigate('/login', { replace: true });
      })
      .catch(() => {
        if (active) setError('Não foi possível encerrar a sessão. Tente novamente.');
      });

    return () => {
      active = false;
    };
  }, [navigate]);

  if (!error) return <p role="status">Encerrando sessão...</p>;

  return (
    <main className="auth-page">
      <section className="auth-card">
        <p role="alert">{error}</p>
        <button className="button button-primary" type="button" onClick={() => window.location.reload()}>
          Tentar novamente
        </button>
        <Link to="/dashboard">Voltar</Link>
      </section>
    </main>
  );
}