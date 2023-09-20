package com.ledikom.repository;

import com.ledikom.model.EventCollector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface EventCollectorRepository extends JpaRepository<EventCollector, LocalDateTime> {

}
