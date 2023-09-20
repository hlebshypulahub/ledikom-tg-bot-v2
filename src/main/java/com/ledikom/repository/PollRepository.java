package com.ledikom.repository;

import com.ledikom.model.Poll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {

    Optional<Poll> findByQuestion(String question);

}
