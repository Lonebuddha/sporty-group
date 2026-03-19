package com.sportygroup.assignment.eventsbetsmatching.repository;

import com.sportygroup.assignment.eventsbetsmatching.domain.SettlementAudit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementAuditRepository extends JpaRepository<SettlementAudit, Long> {

    List<SettlementAudit> findByEventIdOrderByBetId(String eventId);
}

