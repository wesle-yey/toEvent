package com.wesleyey.toEvent.model.events;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
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

    @CreatedDate
    private LocalDate eventDate;

    private String userId;

    public Event(String eventName, String description, String userId) {
        this.eventName = eventName;
        this.description = description;
        this.userId = userId;
        this.eventDate = LocalDate.now();
    }
}
