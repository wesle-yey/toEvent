package com.wesleyey.toEvent.model.events;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Optional<EventResponseDTO> getEventById(String eventId) {
        if (eventRepository.findById(eventId).isPresent()) {
            return Optional.of(new EventResponseDTO(
                    eventRepository.findById(eventId).get().getEventName(),
                    eventRepository.findById(eventId).get().getDescription()
            ));
        } else {
        return Optional.empty();
        }
    }

    public EventResponseDTO addEvent(String eventName, String description, String userId) {
        if (eventName == null || description == null || userId == null) {
            return null;
        } else {
            eventRepository.save(new Event(eventName, description, userId));
            return new EventResponseDTO(eventName, description);
        }
    }

    public void deleteEventById(String id) {
        if (eventRepository.findById(id).isPresent()) {
            eventRepository.deleteById(id);
        }
    }

    public Optional<List<EventResponseDTO>> getAllEventsByUser(String userId) {
        if  (eventRepository.findById(userId).isPresent()) {
        Optional<List<Event>> eventsList = eventRepository.getAllByUserId(userId);
        if (eventsList.isPresent()) {
            List<EventResponseDTO> eventsResponseList = new java.util.ArrayList<>(List.of());
            eventsList.get().forEach(eventResponse -> {
                eventsResponseList.add(
                        new EventResponseDTO(
                                eventResponse.getEventName(),
                                eventResponse.getDescription()
                        )
                );
            ;});
            return Optional.of(eventsResponseList);
        }

        }
        return Optional.empty();
    }
}