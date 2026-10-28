package com.wesleyey.toEvent.controllers;

import com.wesleyey.toEvent.DTOs.EventDTO;
import com.wesleyey.toEvent.DTOs.EventResponseDTO;
import com.wesleyey.toEvent.models.User;
import com.wesleyey.toEvent.services.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // Retorna todos os eventos DO USUÁRIO LOGADO
    @GetMapping
    public ResponseEntity<List<EventResponseDTO>> getAllMyEvents(@AuthenticationPrincipal User user) {
        List<EventResponseDTO> events = eventService.getAllEventsByUser(user.getId());
        return ResponseEntity.ok(events);
    }

    // Retorna um evento específico se ele pertencer ao usuário logado
    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponseDTO> getEvent(
            @PathVariable String eventId,
            @AuthenticationPrincipal User user) {
        return eventService.getEventByIdAndUser(eventId, user.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Cria um evento associado automaticamente ao ID do usuário logado
    @PostMapping
    public ResponseEntity<EventResponseDTO> createEvent(
            @RequestBody @Valid EventDTO eventDTO,
            @AuthenticationPrincipal User user) {
        EventResponseDTO created = eventService.addEvent(eventDTO, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Atualiza um evento associado ao ID se pertencer ao usuário logado
    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponseDTO> updateEvent(
            @PathVariable String eventId,
            @RequestBody @Valid EventDTO eventDTO,
            @AuthenticationPrincipal User user) {
        return eventService.updateEvent(eventId, eventDTO, user.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Deleta o evento apenas se pertencer ao usuário logado
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable String eventId,
            @AuthenticationPrincipal User user) {
        boolean deleted = eventService.deleteEventByIdAndUser(eventId, user.getId());
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
