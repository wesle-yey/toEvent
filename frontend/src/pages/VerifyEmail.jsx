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
          <Link className="brand" to="/login" aria-label="toEvent - página inicial">
            <span className="brand-mark" aria-hidden="true">t</span>
            toEvent
          </Link>
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
            <div className="form-message form-message-success" role="alert">
              {message}
            </div>
            <p style={{ color: '#778078', fontSize: '0.95rem' }}>
              Sua conta agora está totalmente liberada. Clique no botão abaixo para entrar e acessar seus eventos.
            </p>
            <Link to="/login" className="button button-primary" style={{ textDecoration: 'none' }}>
              Entrar na minha conta
            </Link>
          </div>
        )}

        {status === 'error' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div className="form-message form-message-error" role="alert">
              {message}
            </div>

            <div style={{ borderTop: '1px solid #e1e6e1', paddingTop: '1.25rem' }}>
              <form onSubmit={handleResend} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {resendStatus && (
                  <div className="form-message form-message-success">
                    {resendStatus}
                  </div>
                )}
                <label className="field">
                  <span>Precisa de um novo link de confirmação?</span>
                  <input
                    type="email"
                    autoComplete="email"
                    placeholder="Digite seu e-mail cadastrado"
                    value={resendEmail}
                    onChange={(e) => setResendEmail(e.target.value)}
                    required
                    disabled={resending}
                  />
                </label>
                <button type="submit" className="button button-quiet" disabled={resending}>
                  {resending ? 'Enviando...' : 'Reenviar link de confirmação'}
                </button>
              </form>
            </div>

            <p className="auth-switch" style={{ marginTop: 0 }}>
              <Link to="/login" className="text-button" style={{ textDecoration: 'none' }}>
                ← Voltar para a tela de login
              </Link>
            </p>
          </div>
        )}
      </div>
    </main>
  );
}
