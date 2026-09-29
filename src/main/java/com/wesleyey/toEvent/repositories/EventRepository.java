package com.wesleyey.toEvent.repositories;

import com.wesleyey.toEvent.models.Event;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends MongoRepository<Event, String> {
    List<Event> getAllByUserId(String userId);
}
