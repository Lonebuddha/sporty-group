package com.sportygroup.assignment.eventsbetsmatching.repository;

import com.sportygroup.assignment.eventsbetsmatching.domain.Bet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BetRepository extends JpaRepository<Bet, Long> {

    List<Bet> findByEventIdOrderByBetId(String eventId);
}
