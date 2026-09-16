package com.wesleyey.toEvent.model.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventDTO {

    @NotBlank
    private String eventName;

    private String description;

    @NotNull
    private String userId;

    public EventDTO(@NotBlank String eventName, @NotBlank String description, @NotNull String userId) {
        this.eventName = eventName;
        this.description = description;
        this.userId= userId;
    }
}
