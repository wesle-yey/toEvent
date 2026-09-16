package com.wesleyey.toEvent.model.events;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public Optional<EventResponseDTO> getEvent(String eventId){
        return eventService.getEventById(eventId);
    }

    @PostMapping
    public EventResponseDTO createEvent(@RequestBody @Valid EventDTO eventDTO){
        return eventService.addEvent(eventDTO.getEventName(), eventDTO.getDescription(), eventDTO.getUserId());
    }

    @DeleteMapping
    public void deleteEvent(String eventId){
        eventService.deleteEventById(eventId);
    }

    @GetMapping("events")
    public Optional<List<EventResponseDTO>> getAllEvents(@RequestParam String userId){
        return eventService.getAllEventsByUser(userId);
    }
}
