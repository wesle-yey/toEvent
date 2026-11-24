import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import api from '../services/api';

export default function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const [status, setStatus] = useState('loading'); // 'loading' | 'success' | 'error'
  const [message, setMessage] = useState('');
  const [resendEmail, setResendEmail] = useState('');
  const [resendStatus, setResendStatus] = useState('');
  const [resending, setResending] = useState(false);

  useEffect(() => {
    if (!token) {
      setStatus('error');
      setMessage('Nenhum código de verificação foi encontrado na URL.');
      return;
    }

    let active = true;
    api.post('/api/auth/verify-email', { token })
      .then((response) => {
        if (active) {
          const autoLogin = response.data?.autoLogin === true;
          setStatus('success');
          setMessage(
            response.data?.message ||
              (autoLogin
                ? 'E-mail verificado com sucesso! Redirecionando para seus eventos...'
                : 'E-mail confirmado! Entre com seu e-mail e senha para continuar.')
          );
          // Sem login automático (link aberto em outro navegador), o usuário entra pelo botão de login
          if (autoLogin) {
            setTimeout(() => {
              navigate('/dashboard', { replace: true });
            }, 1500);
          }
        }
      })
      .catch((err) => {
        if (active) {
          setStatus('error');
          setMessage(err.response?.data?.message || 'O link de verificação é inválido ou já expirou.');
        }
      });

    return () => {
      active = false;
    };
  }, [token, navigate]);

  async function handleResend(e) {
    e.preventDefault();
    if (!resendEmail) return;
    setResending(true);
    setResendStatus('');
    try {
      const res = await api.post('/api/auth/resend-verification', { email: resendEmail });
      setResendStatus(res.data?.message || 'Se o e-mail estiver cadastrado, um novo link foi enviado!');
    } catch (err) {
      setResendStatus(err.response?.data?.message || 'Não foi possível reenviar. Tente novamente em alguns minutos.');
    } finally {
      setResending(false);
    }
  }

  return (
    <main className="auth-page">
      <div className="auth-card">
        <header className="auth-header">
          <div className="brand">
            <span className="brand-badge" aria-hidden="true">t</span>
            <span className="brand-name">toEvent</span>
          </div>
          <h1>
            {status === 'loading' && 'Verificando e-mail...'}
            {status === 'success' && 'E-mail confirmado!'}
            {status === 'error' && 'Falha na confirmação'}
          </h1>
        </header>

        {status === 'loading' && (
          <div style={{ textAlign: 'center', padding: '2rem 0' }}>
            <p style={{ color: 'var(--text-muted, #64748b)' }}>Estamos validando seu link de confirmação...</p>
          </div>
        )}

        {status === 'success' && (
          <div style={{ textAlign: 'center', display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div className="auth-alert success" role="alert">
              {message}
            </div>
            <p style={{ color: 'var(--text-muted, #64748b)', fontSize: '0.95rem' }}>
              Sua conta agora está totalmente liberada. Clique no botão abaixo para entrar e acessar seus eventos.
            </p>
            <Link to="/login" className="btn-primary" style={{ textDecoration: 'none', display: 'block', textAlign: 'center' }}>
              Entrar na minha conta
            </Link>
          </div>
        )}

        {status === 'error' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div className="auth-alert error" role="alert">
              {message}
            </div>

            <div style={{ borderTop: '1px solid #e2e8f0', paddingTop: '1rem', marginTop: '0.5rem' }}>
              <p style={{ fontSize: '0.9rem', color: '#475569', marginBottom: '0.75rem' }}>
                Precisa de um novo link de confirmação?
              </p>
              <form onSubmit={handleResend} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {resendStatus && (
                  <div className="auth-alert success" style={{ fontSize: '0.85rem' }}>
                    {resendStatus}
                  </div>
                )}
                <input
                  type="email"
                  placeholder="Digite seu e-mail cadastrado"
                  value={resendEmail}
                  onChange={(e) => setResendEmail(e.target.value)}
                  required
                  disabled={resending}
                  style={{ padding: '0.65rem 0.75rem', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                />
                <button type="submit" className="btn-secondary" disabled={resending} style={{ padding: '0.65rem' }}>
                  {resending ? 'Enviando...' : 'Reenviar link de confirmação'}
                </button>
              </form>
            </div>

            <div style={{ textAlign: 'center', marginTop: '0.5rem' }}>
              <Link to="/login" style={{ color: '#4f46e5', textDecoration: 'none', fontSize: '0.9rem' }}>
                ← Voltar para a tela de login
              </Link>
            </div>
          </div>
        )}
      </div>
    </main>
  );
}
