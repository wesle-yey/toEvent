package com.wesleyey.toEvent.DTOs;

import java.time.LocalDate;

public record EventResponseDTO(
        String id,
        String eventName,
        String description,
        LocalDate eventDate
) {
}
