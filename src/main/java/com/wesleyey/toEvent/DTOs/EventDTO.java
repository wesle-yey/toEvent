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

    @NotNull
    private String userId;

    public EventDTO(@NotBlank String eventName, @NotBlank String description, @NotNull String userId,  @NotNull LocalDate eventDate) {
        this.eventName = eventName;
        this.description = description;
        this.userId= userId;
        this.eventDate = eventDate;
    }
}
