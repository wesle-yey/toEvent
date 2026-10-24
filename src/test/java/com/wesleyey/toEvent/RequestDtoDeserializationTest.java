package com.wesleyey.toEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wesleyey.toEvent.DTOs.EventDTO;
import com.wesleyey.toEvent.DTOs.UserDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestDtoDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void registerPayloadDeserializesNameEmailAndPassword() throws Exception {
        UserDTO user = objectMapper.readValue(
                "{\"name\":\"Ana\",\"email\":\"ana@example.com\",\"password\":\"12345678\"}",
                UserDTO.class
        );

        assertEquals("Ana", user.getName());
        assertEquals("ana@example.com", user.getEmail());
        assertEquals("12345678", user.getPassword());
    }

    @Test
    void createEventPayloadDeserializesNameDescriptionAndDate() throws Exception {
        EventDTO event = objectMapper.readValue(
                "{\"eventName\":\"Reunião\",\"description\":\"Alinhamento\",\"eventDate\":\"2026-03-26\"}",
                EventDTO.class
        );

        assertEquals("Reunião", event.getEventName());
        assertEquals("Alinhamento", event.getDescription());
        assertEquals("2026-03-26", event.getEventDate().toString());
    }
}
