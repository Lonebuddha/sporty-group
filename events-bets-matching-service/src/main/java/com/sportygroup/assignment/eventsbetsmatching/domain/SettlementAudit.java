package com.sportygroup.assignment.eventsbetsmatching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "settlement_audit")
public class SettlementAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bet_id", nullable = false)
    private Long betId;

    @Column(name = "event_id", nullable = false, length = 32)
    private String eventId;

    @Column(name = "selected_winner_id", nullable = false, length = 32)
    private String selectedWinnerId;

    @Column(name = "actual_winner_id", nullable = false, length = 32)
    private String actualWinnerId;

    @Column(name = "settlement_outcome", nullable = false, length = 16)
    private String settlementOutcome;

    @Column(name = "bet_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal betAmount;

    @Column(name = "rocketmq_message_id", nullable = false, length = 64)
    private String rocketmqMessageId;

    @Column(name = "rocketmq_send_status", nullable = false, length = 32)
    private String rocketmqSendStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SettlementAudit() {
    }

    public static SettlementAudit from(
        Bet bet,
        String actualWinnerId,
        SettlementOutcome settlementOutcome,
        String rocketmqMessageId,
        String rocketmqSendStatus,
        Instant createdAt
    ) {
        SettlementAudit audit = new SettlementAudit();
        audit.betId = bet.getBetId();
        audit.eventId = bet.getEventId();
        audit.selectedWinnerId = bet.getEventWinnerId();
        audit.actualWinnerId = actualWinnerId;
        audit.settlementOutcome = settlementOutcome.name();
        audit.betAmount = bet.getBetAmount();
        audit.rocketmqMessageId = rocketmqMessageId;
        audit.rocketmqSendStatus = rocketmqSendStatus;
        audit.createdAt = createdAt;
        return audit;
    }

    public Long getBetId() {
        return betId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getSelectedWinnerId() {
        return selectedWinnerId;
    }

    public String getActualWinnerId() {
        return actualWinnerId;
    }

    public String getSettlementOutcome() {
        return settlementOutcome;
    }

    public String getRocketmqMessageId() {
        return rocketmqMessageId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

