package com.wesleyey.toEvent.services;

import com.wesleyey.toEvent.DTOs.EventDTO;
import com.wesleyey.toEvent.DTOs.EventResponseDTO;
import com.wesleyey.toEvent.exception.EventLimitExceededException;
import com.wesleyey.toEvent.models.Event;
import com.wesleyey.toEvent.models.User;
import com.wesleyey.toEvent.repositories.EventRepository;
import com.wesleyey.toEvent.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventService eventService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Deve retornar todos os eventos do usuário")
    void getAllEventsByUser_Success() {
        Event event1 = new Event("Show", "Show de rock", "user1", LocalDate.now());
        Event event2 = new Event("Teatro", "Peça de teatro", "user1", LocalDate.now());

        when(eventRepository.getAllByUserId("user1")).thenReturn(List.of(event1, event2));

        List<EventResponseDTO> events = eventService.getAllEventsByUser("user1");

        assertEquals(2, events.size());
        assertEquals("Show", events.get(0).eventName());
        verify(eventRepository, times(1)).getAllByUserId("user1");
    }

    @Test
    @DisplayName("Deve criar um evento com sucesso quando limite não foi atingido")
    void addEvent_Success() {
        User user = new User("Wesley", "wesley@test.com", "123456");
        EventDTO dto = new EventDTO("Show", "Show de rock", LocalDate.now());
        Event saved = new Event("Show", "Show de rock", "user1", LocalDate.now());
        saved.setId("event1");

        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        when(eventRepository.countByUserId("user1")).thenReturn(2L);
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        EventResponseDTO result = eventService.addEvent(dto, "user1");

        assertNotNull(result);
        assertEquals("Show", result.eventName());
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    @DisplayName("Deve lançar EventLimitExceededException quando usuário gratuito atingir limite de 5 eventos")
    void addEvent_ThrowsException_WhenLimitExceeded() {
        User user = new User("Wesley", "wesley@test.com", "123456");
        user.setPremium(false);
        EventDTO dto = new EventDTO("Show", "Show de rock", LocalDate.now());

        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        when(eventRepository.countByUserId("user1")).thenReturn(5L);

        assertThrows(EventLimitExceededException.class, () -> eventService.addEvent(dto, "user1"));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("Deve permitir criar evento se usuário atingiu 5 eventos mas é Premium")
    void addEvent_Success_WhenPremiumUserReachesLimit() {
        User user = new User("Wesley", "wesley@test.com", "123456");
        user.setPremium(true);
        EventDTO dto = new EventDTO("Show", "Show de rock", LocalDate.now());
        Event saved = new Event("Show", "Show de rock", "user1", LocalDate.now());
        saved.setId("event1");

        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        EventResponseDTO result = eventService.addEvent(dto, "user1");

        assertNotNull(result);
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    @DisplayName("Deve atualizar um evento se pertencer ao usuário")
    void updateEvent_Success() {
        Event existing = new Event("Show", "Show antigo", "user1", LocalDate.now());
        existing.setId("event1");
        EventDTO dto = new EventDTO("Show Novo", "Show atualizado", LocalDate.now());

        when(eventRepository.findByIdAndUserId("event1", "user1")).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenReturn(existing);

        Optional<EventResponseDTO> result = eventService.updateEvent("event1", dto, "user1");

        assertTrue(result.isPresent());
        assertEquals("Show Novo", result.get().eventName());
    }

    @Test
    @DisplayName("Não deve atualizar evento se não pertencer ao usuário")
    void updateEvent_NotFound() {
        EventDTO dto = new EventDTO("Show", "Desc", LocalDate.now());
        when(eventRepository.findByIdAndUserId("event1", "user1")).thenReturn(Optional.empty());

        Optional<EventResponseDTO> result = eventService.updateEvent("event1", dto, "user1");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Deve deletar evento se pertencer ao usuário")
    void deleteEvent_Success() {
        Event event = new Event("Show", "Desc", "user1", LocalDate.now());
        when(eventRepository.findByIdAndUserId("event1", "user1")).thenReturn(Optional.of(event));

        boolean deleted = eventService.deleteEventByIdAndUser("event1", "user1");

        assertTrue(deleted);
        verify(eventRepository, times(1)).deleteByIdAndUserId("event1", "user1");
    }
}
