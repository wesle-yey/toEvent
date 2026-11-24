import { useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import api from '../services/api';

export function ProtectedRoute({ children }) {
  const [status, setStatus] = useState('checking');

  useEffect(() => {
    let active = true;

    api.get('/api/users')
      .then(() => {
        if (active) setStatus('authenticated');
      })
      .catch((error) => {
        if (active) {
          setStatus(error.response?.status === 401 ? 'unauthenticated' : 'error');
        }
      });

    return () => {
      active = false;
    };
  }, []);

  if (status === 'checking') {
    return <p role="status">Verificando sessão...</p>;
  }

  if (status === 'error') {
    return (
      <main className="auth-page">
        <section className="auth-card">
          <p role="alert">Não foi possível verificar sua sessão. Confira sua conexão e tente novamente.</p>
          <Link className="button button-primary" to="/login">Ir para o login</Link>
        </section>
      </main>
    );
  }

  if (status === 'unauthenticated') {
    return <Navigate to="/login" replace />;
  }

  return children;
}