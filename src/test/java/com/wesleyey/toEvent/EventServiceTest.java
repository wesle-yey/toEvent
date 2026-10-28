package com.wesleyey.toEvent;

import com.wesleyey.toEvent.DTOs.EventDTO;
import com.wesleyey.toEvent.DTOs.EventResponseDTO;
import com.wesleyey.toEvent.models.Event;
import com.wesleyey.toEvent.repositories.EventRepository;
import com.wesleyey.toEvent.services.EventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    @DisplayName("updateEvent deve atualizar os dados do evento e retornar o DTO quando o evento existir e pertencer ao usuário")
    void updateEventSuccess() {
        String eventId = "evt-123";
        String userId = "usr-456";
        Event existingEvent = new Event("Nome Antigo", "Desc Antiga", userId, LocalDate.of(2027, 1, 1));
        existingEvent.setId(eventId);

        EventDTO updateDTO = new EventDTO("Novo Nome", "Nova Descrição", LocalDate.of(2027, 2, 1));

        when(eventRepository.findByIdAndUserId(eventId, userId)).thenReturn(Optional.of(existingEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<EventResponseDTO> result = eventService.updateEvent(eventId, updateDTO, userId);

        assertTrue(result.isPresent());
        assertEquals(eventId, result.get().id());
        assertEquals("Novo Nome", result.get().eventName());
        assertEquals("Nova Descrição", result.get().description());
        assertEquals(LocalDate.of(2027, 2, 1), result.get().eventDate());
        verify(eventRepository).save(existingEvent);
    }

    @Test
    @DisplayName("updateEvent deve retornar Optional.empty() quando o evento não existir ou não pertencer ao usuário")
    void updateEventNotFound() {
        String eventId = "evt-999";
        String userId = "usr-456";
        EventDTO updateDTO = new EventDTO("Novo Nome", "Nova Descrição", LocalDate.of(2027, 2, 1));

        when(eventRepository.findByIdAndUserId(eventId, userId)).thenReturn(Optional.empty());

        Optional<EventResponseDTO> result = eventService.updateEvent(eventId, updateDTO, userId);

        assertFalse(result.isPresent());
        verify(eventRepository, never()).save(any());
    }
}
