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
  const navigate = useNavigate();
  const registering = mode === 'register';

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setLoading(true);
    let registrationCompleted = false;

    try {
      if (registering) {
        await api.post('/api/auth/register', { name, email, password });
        registrationCompleted = true;
      }

      const response = await api.post('/api/auth/login', { email, password });
      if (response.data?.token) {
        localStorage.setItem('token', response.data.token);
      }
      navigate('/dashboard');
    } catch (requestError) {
      setError(
        registrationCompleted
          ? 'Cadastro concluído, mas não foi possível entrar. Faça login com seus dados.'
          : requestError.response?.data?.message ||
              (registering
                ? 'Não foi possível criar sua conta. Verifique os dados e tente novamente.'
                : 'Não foi possível entrar. Confira seu email e senha e tente novamente.'),
      );
    } finally {
      setLoading(false);
    }
  }

  function changeMode(nextMode) {
    setMode(nextMode);
    setError('');
  }

  return (
    <main className="auth-page">
      <section className="auth-card" aria-labelledby="auth-title">
        <header className="auth-header">
          <Link className="brand" to="/login" aria-label="toEvent - página inicial">
            <span className="brand-mark" aria-hidden="true">t</span>
            <span>toEvent</span>
          </Link>
          <h1 id="auth-title">{registering ? 'Crie sua conta' : 'Bem-vindo de volta'}</h1>
          <p>
            {registering
              ? 'Organize os momentos importantes em um só lugar.'
              : 'Seus momentos importantes, sempre por perto.'}
          </p>
        </header>

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

          {error && <p className="form-message form-message-error" role="alert">{error}</p>}

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
      </section>
    </main>
  );
}
