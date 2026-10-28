package com.wesleyey.toEvent.services;

import com.wesleyey.toEvent.DTOs.EventDTO;
import com.wesleyey.toEvent.DTOs.EventResponseDTO;
import com.wesleyey.toEvent.models.Event;
import com.wesleyey.toEvent.repositories.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<EventResponseDTO> getAllEventsByUser(String userId) {
        return eventRepository.getAllByUserId(userId).stream()
                .map(event -> new EventResponseDTO(
                        event.getId(),
                        event.getEventName(),
                        event.getDescription(),
                        event.getEventDate()
                ))
                .toList();
    }

    public Optional<EventResponseDTO> getEventByIdAndUser(String eventId, String userId) {
        return eventRepository.findByIdAndUserId(eventId, userId)
                .map(event -> new EventResponseDTO(
                        event.getId(),
                        event.getEventName(),
                        event.getDescription(),
                        event.getEventDate()
                ));
    }

    public EventResponseDTO addEvent(EventDTO eventDTO, String userId) {
        Event event = new Event(
                eventDTO.getEventName(),
                eventDTO.getDescription(),
                userId,
                eventDTO.getEventDate()
        );
        eventRepository.save(event);
        return new EventResponseDTO(event.getId(), event.getEventName(), event.getDescription(), event.getEventDate());
    }

    public Optional<EventResponseDTO> updateEvent(String eventId, EventDTO eventDTO, String userId) {
        Optional<Event> existing = eventRepository.findByIdAndUserId(eventId, userId);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        Event event = existing.get();
        event.setEventName(eventDTO.getEventName());
        event.setDescription(eventDTO.getDescription());
        event.setEventDate(eventDTO.getEventDate());
        eventRepository.save(event);
        return Optional.of(new EventResponseDTO(event.getId(), event.getEventName(), event.getDescription(), event.getEventDate()));
    }

    public boolean deleteEventByIdAndUser(String eventId, String userId) {
        Optional<Event> event = eventRepository.findByIdAndUserId(eventId, userId);
        if (event.isPresent()) {
            eventRepository.deleteByIdAndUserId(eventId, userId);
            return true;
        }
        return false;
    }
}
