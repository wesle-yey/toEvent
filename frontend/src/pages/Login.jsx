import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../services/api';

export default function Login() {
  const [mode, setMode] = useState('login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Estados para fluxo de confirmação de e-mail
  const [registrationSuccess, setRegistrationSuccess] = useState(false);
  const [registeredEmail, setRegisteredEmail] = useState('');
  const [isUnverified, setIsUnverified] = useState(false);
  const [resending, setResending] = useState(false);
  const [resendMessage, setResendMessage] = useState('');
  const navigate = useNavigate();
  const registering = mode === 'register';

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setIsUnverified(false);
    setResendMessage('');
    setLoading(true);

    try {
      if (registering) {
        await api.post('/api/auth/register', { name, email, password });
        setRegisteredEmail(email);
        setRegistrationSuccess(true);
        return;
      }

      const response = await api.post('/api/auth/login', { email, password });
      if (response.data?.token) {
        localStorage.setItem('token', response.data.token);
      }
      navigate('/dashboard');
    } catch (requestError) {
      const errorData = requestError.response?.data;
      if (errorData?.error === 'EMAIL_NOT_VERIFIED') {
        setIsUnverified(true);
      }
      setError(
        errorData?.message ||
          (registering
            ? 'Não foi possível criar sua conta. Verifique os dados e tente novamente.'
            : 'Não foi possível entrar. Confira seu email e senha e tente novamente.')
      );
    } finally {
      setLoading(false);
    }
  }

  async function handleResendVerification() {
    const targetEmail = email || registeredEmail;
    if (!targetEmail) return;

    setResending(true);
    setResendMessage('');
    try {
      const res = await api.post('/api/auth/resend-verification', { email: targetEmail });
      setResendMessage(res.data?.message || 'Link de confirmação reenviado! Verifique sua caixa de entrada.');
    } catch (err) {
      setResendMessage(err.response?.data?.message || 'Não foi possível reenviar agora. Tente novamente em alguns segundos.');
    } finally {
      setResending(false);
    }
  }

  function changeMode(nextMode) {
    setMode(nextMode);
    setError('');
    setIsUnverified(false);
    setResendMessage('');
    setRegistrationSuccess(false);
  }

  return (
    <main className="auth-page">
      <section className="auth-card" aria-labelledby="auth-title">
        <header className="auth-header">
          <Link className="brand" to="/login" aria-label="toEvent - página inicial">
            <span className="brand-mark" aria-hidden="true">t</span>
            <span>toEvent</span>
          </Link>
          <h1 id="auth-title">
            {registrationSuccess
              ? 'Confirme seu e-mail'
              : registering
              ? 'Crie sua conta'
              : 'Bem-vindo de volta'}
          </h1>
          <p>
            {registrationSuccess
              ? `Enviamos um link de ativação para ${registeredEmail}`
              : registering
              ? 'Organize os momentos importantes em um só lugar.'
              : 'Seus momentos importantes, sempre por perto.'}
          </p>
        </header>

        {registrationSuccess ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem', textAlign: 'center' }}>
            <div
              style={{
                background: '#dcfce7',
                color: '#166534',
                border: '1px solid #bbf7d0',
                padding: '0.85rem 1rem',
                borderRadius: '8px',
                textAlign: 'left',
                fontSize: '0.92rem',
                lineHeight: '1.5',
              }}
            >
              Conta criada com sucesso! Enviamos um link de confirmação para <strong>{registeredEmail}</strong>.
              Por favor, abra sua caixa de entrada e clique no link para ativar seu acesso.
            </div>

            <p style={{ color: '#4d6152', fontSize: '0.9rem', lineHeight: '1.5' }}>
              Não encontrou o e-mail? Verifique sua caixa de spam ou promoções.
            </p>

            {resendMessage && (
              <p style={{ fontSize: '0.85rem', color: '#166534', background: '#dcfce7', padding: '0.5rem', borderRadius: '6px' }}>
                {resendMessage}
              </p>
            )}

            <button
              type="button"
              onClick={handleResendVerification}
              disabled={resending}
              className="button"
              style={{ padding: '0.75rem', fontSize: '0.95rem' }}
            >
              {resending ? 'Reenviando...' : 'Reenviar e-mail de confirmação'}
            </button>

            <button
              type="button"
              onClick={() => changeMode('login')}
              className="button button-primary"
              style={{ padding: '0.75rem', fontSize: '0.95rem' }}
            >
              Ir para o Login
            </button>
          </div>
        ) : (
          <>
            <div className="auth-tabs" role="tablist" aria-label="Acesso à conta">
          <button
            id="login-tab"
            className={`auth-tab ${!registering ? 'is-active' : ''}`}
            type="button"
            role="tab"
            aria-selected={!registering}
            aria-controls="auth-panel"
            onClick={() => changeMode('login')}
            disabled={loading}
          >
            Entrar
          </button>
          <button
            id="register-tab"
            className={`auth-tab ${registering ? 'is-active' : ''}`}
            type="button"
            role="tab"
            aria-selected={registering}
            aria-controls="auth-panel"
            onClick={() => changeMode('register')}
            disabled={loading}
          >
            Cadastro
          </button>
        </div>

        <form id="auth-panel" className="auth-form" onSubmit={handleSubmit}>
          {registering && (
            <label className="field">
              <span>Nome</span>
              <input
                autoComplete="name"
                type="text"
                value={name}
                onChange={(event) => setName(event.target.value)}
                placeholder="Como podemos te chamar?"
                minLength={3}
                required
              />
            </label>
          )}

          <label className="field">
            <span>Email</span>
            <input
              autoComplete="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="voce@exemplo.com"
              required
            />
          </label>

          <label className="field">
            <span>Senha</span>
            <input
              autoComplete={registering ? 'new-password' : 'current-password'}
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Sua senha"
              minLength={registering ? 8 : undefined}
              required
            />
            {registering && <small className="field-hint">Use pelo menos 8 caracteres.</small>}
          </label>

          {error && (
            <div className="form-message form-message-error" role="alert">
              <div>{error}</div>
              {isUnverified && (
                <div style={{ marginTop: '0.6rem', borderTop: '1px solid #fecaca', paddingTop: '0.5rem' }}>
                  <button
                    type="button"
                    onClick={handleResendVerification}
                    disabled={resending}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: '#264832',
                      fontWeight: 650,
                      cursor: 'pointer',
                      padding: 0,
                      textDecoration: 'underline',
                      fontSize: '0.85rem',
                    }}
                  >
                    {resending ? 'Reenviando...' : 'Clique aqui para reenviar o link de confirmação'}
                  </button>
                  {resendMessage && (
                    <div style={{ marginTop: '0.4rem', color: '#166534', fontSize: '0.85rem' }}>
                      {resendMessage}
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          <button className="button button-primary auth-submit" type="submit" disabled={loading}>
            {loading ? 'Aguarde...' : registering ? 'Criar conta' : 'Entrar'}
          </button>
        </form>

        <p className="auth-switch">
          {registering ? 'Já tem uma conta?' : 'Ainda não tem uma conta?'}{' '}
          <button
            className="text-button"
            type="button"
            onClick={() => changeMode(registering ? 'login' : 'register')}
            disabled={loading}
          >
            {registering ? 'Entrar' : 'Cadastre-se'}
          </button>
        </p>
        </>
        )}
      </section>
    </main>
  );
}
