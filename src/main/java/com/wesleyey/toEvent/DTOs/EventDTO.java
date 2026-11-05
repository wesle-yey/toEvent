package com.wesleyey.toEvent.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class EventDTO {

    @NotBlank
    @Size(max = 120)
    private String eventName;

    @Size(max = 500)
    private String description;

    @NotNull
    private LocalDate eventDate;

    public EventDTO(String eventName, String description, LocalDate eventDate) {
        this.eventName = eventName;
        this.description = description;
        this.eventDate = eventDate;
    }
}
