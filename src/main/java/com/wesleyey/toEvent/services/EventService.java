package com.wesleyey.toEvent.services;

import com.wesleyey.toEvent.models.Event;
import com.wesleyey.toEvent.repositories.EventRepository;
import com.wesleyey.toEvent.DTOs.EventResponseDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Optional<EventResponseDTO> getEventById(String eventId) {
    return eventRepository.findById(eventId)
            .map(event -> new EventResponseDTO(
                    event.getEventName(),
                    event.getDescription(),
                    event.getEventDate()
            ));
}

    public EventResponseDTO addEvent(String eventName, String description, String userId, LocalDate eventDate) {
        if (eventName == null || description == null || userId == null) {
            return null;
        } else {
            eventRepository.save(new Event(eventName, description, userId, eventDate));
            return new EventResponseDTO(eventName, description, eventDate);
        }
    }

    public void deleteEventById(String id) {
        if (eventRepository.findById(id).isPresent()) {
            eventRepository.deleteById(id);
        }
    }

    public Optional<List<EventResponseDTO>> getAllEventsByUser(String userId) {
        List<Event> eventsList = eventRepository.getAllByUserId(userId);
        List<EventResponseDTO> eventsResponseList = new java.util.ArrayList<>(List.of());
        eventsList.forEach(eventResponse -> eventsResponseList.add(
                new EventResponseDTO(
                        eventResponse.getEventName(),
                        eventResponse.getDescription(),
                        eventResponse.getEventDate()
                )
            ));
        return Optional.of(eventsResponseList);
    }
}