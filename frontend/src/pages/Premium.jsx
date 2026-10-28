import { Link } from 'react-router-dom';

export default function Premium() {
  return (
    <main className="dashboard-page">
      <header className="dashboard-header">
        <Link className="brand" to="/dashboard" aria-label="toEvent - voltar ao dashboard">
          <span className="brand-mark" aria-hidden="true">t</span>
          <span>toEvent</span>
        </Link>
        <Link to="/dashboard" className="button button-quiet" style={{ textDecoration: 'none' }}>
          Voltar
        </Link>
      </header>

      <section className="events-content" style={{ display: 'grid', placeItems: 'center', minHeight: '60vh' }}>
        <div className="auth-card" style={{ textAlign: 'center' }}>
          <span className="empty-illustration" aria-hidden="true">✨</span>
          <p className="eyebrow">Recurso Exclusivo</p>
          <h1 style={{ fontSize: '24px', margin: '8px 0 12px', color: '#26352a' }}>toEvent Premium</h1>
          <p style={{ color: '#778078', fontSize: '14px', marginBottom: '24px' }}>
            Esta funcionalidade está em construção. Em breve você terá acesso a recursos avançados para organizar seus momentos.
          </p>
          <Link to="/dashboard" className="button button-primary" style={{ textDecoration: 'none' }}>
            Retornar ao Dashboard
          </Link>
        </div>
      </section>
    </main>
  );
}