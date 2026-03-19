package com.sportygroup.assignment.eventsbetsmatching.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bets")
public class Bet {

    @Id
    @Column(name = "bet_id", nullable = false)
    private Long betId;

    @Column(name = "user_id", nullable = false, length = 32)
    private String userId;

    @Column(name = "event_id", nullable = false, length = 32)
    private String eventId;

    @Column(name = "event_market_id", nullable = false, length = 32)
    private String eventMarketId;

    @Column(name = "event_winner_id", nullable = false, length = 32)
    private String eventWinnerId;

    @Column(name = "bet_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal betAmount;

    @Column(name = "settled", nullable = false)
    private boolean settled;

    @Column(name = "settlement_status", length = 16)
    private String settlementStatus;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "settlement_message_id", length = 64)
    private String settlementMessageId;

    protected Bet() {
    }

    public Long getBetId() {
        return betId;
    }

    public String getUserId() {
        return userId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventMarketId() {
        return eventMarketId;
    }

    public String getEventWinnerId() {
        return eventWinnerId;
    }

    public BigDecimal getBetAmount() {
        return betAmount;
    }

    public boolean isSettled() {
        return settled;
    }

    public String getSettlementStatus() {
        return settlementStatus;
    }

    public Instant getSettledAt() {
        return settledAt;
    }

    public String getSettlementMessageId() {
        return settlementMessageId;
    }

    public void markSettled(SettlementOutcome settlementOutcome, Instant settledTimestamp, String messageId) {
        this.settled = true;
        this.settlementStatus = settlementOutcome.name();
        this.settledAt = settledTimestamp;
        this.settlementMessageId = messageId;
    }
}

