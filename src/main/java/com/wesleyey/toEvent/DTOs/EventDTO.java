package com.wesleyey.toEvent.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EventDTO {

    @NotBlank
    private String eventName;

    private String description;

    @NotNull
    private LocalDate eventDate;

    public EventDTO(@NotBlank String eventName, @NotBlank String description,  @NotNull LocalDate eventDate) {
        this.eventName = eventName;
        this.description = description;
        this.eventDate = eventDate;
    }
}
