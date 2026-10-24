import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../services/api';

function formatEventDate(value) {
  if (!value) return 'Data não informada';

  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  }).format(date);
}

export default function Dashboard() {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadAttempt, setLoadAttempt] = useState(0);
  const [error, setError] = useState('');
  const [showEventForm, setShowEventForm] = useState(false);
  const [eventName, setEventName] = useState('');
  const [eventDate, setEventDate] = useState('');
  const [description, setDescription] = useState('');
  const [saving, setSaving] = useState(false);
  const [deletingEventId, setDeletingEventId] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    let active = true;

    async function fetchEvents() {
      try {
        const response = await api.get('/api/events');
        if (!Array.isArray(response.data)) {
          throw new Error('A resposta da API de eventos está em um formato inválido.');
        }
        if (active) {
          setEvents(response.data);
          setError('');
        }
      } catch (requestError) {
        if (active) {
          setError(
            requestError.response?.data?.message ||
              'Não foi possível carregar seus eventos. Tente novamente.',
          );
        }
      } finally {
        if (active) setLoading(false);
      }
    }

    fetchEvents();
    return () => {
      active = false;
    };
  }, [loadAttempt]);

  async function handleCreateEvent(event) {
    event.preventDefault();
    setError('');
    setSaving(true);

    try {
      const response = await api.post('/api/events', {
        eventName,
        description,
        eventDate,
      });
      setEvents((currentEvents) => [...currentEvents, response.data]);
      setEventName('');
      setEventDate('');
      setDescription('');
      setShowEventForm(false);
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
          'Não foi possível criar o evento. Verifique os dados e tente novamente.',
      );
    } finally {
      setSaving(false);
    }
  }

  async function handleDeleteEvent(eventId) {
    const confirmed = window.confirm('Deseja excluir este evento?');
    if (!confirmed) return;

    setError('');
    setDeletingEventId(eventId);
    try {
      await api.delete(`/api/events/${encodeURIComponent(eventId)}`);
      setEvents((currentEvents) => currentEvents.filter((item) => item.id !== eventId));
    } catch (requestError) {
      setError(
        requestError.response?.data?.message ||
          'Não foi possível excluir o evento. Tente novamente.',
      );
    } finally {
      setDeletingEventId('');
    }
  }

  function handleLogout() {
    localStorage.removeItem('token');
    navigate('/login');
  }

  return (
    <main className="dashboard-page">
      <header className="dashboard-header">
        <Link className="brand" to="/dashboard" aria-label="toEvent - meus eventos">
          <span className="brand-mark" aria-hidden="true">t</span>
          <span>toEvent</span>
        </Link>
        <button className="button button-quiet" type="button" onClick={handleLogout}>
          Sair
        </button>
      </header>

      <section className="events-content" aria-labelledby="events-title">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Seus momentos, organizados</p>
            <h1 id="events-title">Meus eventos</h1>
          </div>
          {!loading && events.length > 0 && (
            <button
              className="button button-primary"
              type="button"
              onClick={() => setShowEventForm((current) => !current)}
            >
              <span aria-hidden="true">+</span> Criar evento
            </button>
          )}
        </div>

        {error && (
          <div className="form-message form-message-error dashboard-error" role="alert">
            <span>{error}</span>
            {!loading && (
              <button
                className="retry-button"
                type="button"
                onClick={() => {
                  setLoading(true);
                  setLoadAttempt((attempt) => attempt + 1);
                }}
              >
                Tentar novamente
              </button>
            )}
          </div>
        )}

        {showEventForm && (
          <form className="event-form-card" onSubmit={handleCreateEvent}>
            <div className="event-form-heading">
              <div>
                <p className="eyebrow">Um novo momento</p>
                <h2>Criar evento</h2>
              </div>
              <button
                className="icon-button"
                type="button"
                onClick={() => setShowEventForm(false)}
                aria-label="Fechar formulário"
              >
                ×
              </button>
            </div>
            <label className="field">
              <span>Nome do evento</span>
              <input
                autoFocus
                type="text"
                value={eventName}
                onChange={(event) => setEventName(event.target.value)}
                placeholder="Ex.: Reunião de alinhamento"
                maxLength={120}
                required
              />
            </label>
            <label className="field">
              <span>Data</span>
              <input
                type="date"
                value={eventDate}
                onChange={(event) => setEventDate(event.target.value)}
                required
              />
            </label>
            <label className="field">
              <span>Descrição <small>(opcional)</small></span>
              <textarea
                rows="3"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                placeholder="Adicione alguns detalhes..."
                maxLength={500}
              />
            </label>
            <div className="event-form-actions">
              <button
                className="button button-quiet"
                type="button"
                onClick={() => setShowEventForm(false)}
                disabled={saving}
              >
                Cancelar
              </button>
              <button className="button button-primary" type="submit" disabled={saving}>
                {saving ? 'Salvando...' : 'Salvar evento'}
              </button>
            </div>
          </form>
        )}

        {loading ? (
          <div className="status-card" role="status">
            <span className="loading-indicator" aria-hidden="true" />
            <p>Carregando seus eventos...</p>
          </div>
        ) : events.length === 0 ? (
          <div className="empty-state">
            <span className="empty-illustration" aria-hidden="true">✳</span>
            <h2>Todo grande momento começa com um plano.</h2>
            <p>Você ainda não adicionou nenhum evento. Crie o primeiro e deixe tudo organizado.</p>
            {!showEventForm && (
              <button
                className="button button-primary"
                type="button"
                onClick={() => setShowEventForm(true)}
              >
                Criar meu primeiro evento
              </button>
            )}
          </div>
        ) : (
          <ul className="event-list">
            {events.map((item) => (
              <li className="event-card" key={item.id}>
                <div className="event-date" aria-label={formatEventDate(item.eventDate)}>
                  <span>{item.eventDate?.slice(8, 10) || '--'}</span>
                  <small>
                    {item.eventDate
                      ? new Intl.DateTimeFormat('pt-BR', { month: 'short' })
                          .format(new Date(`${item.eventDate}T00:00:00`))
                          .replace('.', '')
                      : '---'}
                  </small>
                </div>
                <div className="event-details">
                  <h2>{item.eventName}</h2>
                  <p className="event-full-date">{formatEventDate(item.eventDate)}</p>
                  {item.description && <p className="event-description">{item.description}</p>}
                </div>
                <button
                  className="button button-danger"
                  type="button"
                  onClick={() => handleDeleteEvent(item.id)}
                  disabled={deletingEventId === item.id}
                >
                  {deletingEventId === item.id ? 'Excluindo...' : 'Excluir'}
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}
