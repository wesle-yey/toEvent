package com.wesleyey.toEvent.repositories;

import com.wesleyey.toEvent.models.Event;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends MongoRepository<Event, String> {
    List<Event> getAllByUserId(String userId);

    // Garante que só encontra o evento se o id E o userId baterem
    Optional<Event> findByIdAndUserId(String id, String userId);

    // Permite deletar apenas se for do próprio usuário
    void deleteByIdAndUserId(String id, String userId);

    long countByUserId(String userId);
}
