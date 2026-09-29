package com.wesleyey.toEvent.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Document(collection = "events")
public class Event {
    @Id
    private String id;

    private String eventName;

    private String description;

    private LocalDate eventDate;

    private String userId;

        public Event(String eventName, String description, String userId, LocalDate eventDate) {
        this.eventName = eventName;
        this.description = description;
        this.userId = userId;
        this.eventDate = eventDate;
    }
}
