package com.wesleyey.toEvent.controllers;

import com.wesleyey.toEvent.DTOs.EventDTO;
import com.wesleyey.toEvent.DTOs.EventResponseDTO;
import com.wesleyey.toEvent.services.EventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public Optional<EventResponseDTO> getEvent(String eventId){
        return eventService.getEventById(eventId);
    }

    @PostMapping
    public EventResponseDTO createEvent(@RequestBody @Valid EventDTO eventDTO){
        return eventService.addEvent(eventDTO.getEventName(),
        eventDTO.getDescription(),
        eventDTO.getUserId(),
        eventDTO.getEventDate());
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable String eventId){
        eventService.deleteEventById(eventId);
        return null;
    }

    @GetMapping("user")
    public List<EventResponseDTO> getAllEvents(@RequestParam String userId){
        return eventService.getAllEventsByUser(userId).orElse(new java.util.ArrayList<>());
    }
}
