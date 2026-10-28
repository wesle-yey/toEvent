
import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <main className="auth-page">
      <section className="auth-card" style={{ textAlign: 'center' }}>
        <header className="auth-header">
          <Link className="brand" to="/dashboard" aria-label="toEvent - voltar ao dashboard">
            <span className="brand-mark" aria-hidden="true">t</span>
            <span>toEvent</span>
          </Link>
          <p className="eyebrow" style={{ marginTop: '12px' }}>Erro 404</p>
          <h1>Página não encontrada</h1>
          <p>
            O endereço que você tentou acessar não existe ou não está disponível.
          </p>
        </header>

        <Link to="/dashboard" className="button button-primary" style={{ textDecoration: 'none' }}>
          Voltar ao Dashboard
        </Link>
      </section>
    </main>
  );
}