import { useEffect, useState } from "react";
import axios from "axios";

export default function Dashboard() {
    const [events, setEvents] = useState(null);
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState(null);

    useEffect(() => {
        async function fetchEvents() {
            try {
                setLoading(true);
                const response = await axios.get('/api/events');
                if (!response.ok) {
                    throw new Error('Failed to fetch events');
                }
                const data = await response.json();
                setEvents(data);
                setError(null);
            } catch (err) {
                setError(err.message);
                setEvents(null);
            } finally {
                setLoading(false);
            }
        }

        fetchEvents();
    }, [events]);

    if (loading) {
        return (
            <h2>Loading...</h2>
        )
    }
    if (events == null) {
        return (
            <h2>Looks like you have no events yet...</h2>
        )
    }
    return (
        <ul>
            {events.map((event) => (
                <li key={event.id}>{event.eventName}</li>
            ))}
        </ul>
    );
}