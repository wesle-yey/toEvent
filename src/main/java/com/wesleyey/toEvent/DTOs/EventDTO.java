package com.wesleyey.toEvent.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class EventDTO {

    @NotBlank
    private String eventName;

    private String description;

    @NotNull
    private LocalDate eventDate;

    public EventDTO(String eventName, String description, LocalDate eventDate) {
        this.eventName = eventName;
        this.description = description;
        this.eventDate = eventDate;
    }
}
