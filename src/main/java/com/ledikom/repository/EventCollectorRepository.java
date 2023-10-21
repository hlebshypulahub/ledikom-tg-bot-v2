package com.ledikom.repository;

import com.ledikom.model.EventCollector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface EventCollectorRepository extends JpaRepository<EventCollector, LocalDateTime> {

    List<EventCollector> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);

}
