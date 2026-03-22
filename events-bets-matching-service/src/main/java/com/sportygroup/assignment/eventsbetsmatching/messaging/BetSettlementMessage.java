package com.sportygroup.assignment.eventsbetsmatching.messaging;

import java.math.BigDecimal;
import java.time.Instant;

public record BetSettlementMessage(
    Long betId,
    String userId,
    String eventId,
    String eventName,
    String eventMarketId,
    String selectedWinnerId,
    String actualWinnerId,
    String settlementOutcome,
    BigDecimal betAmount,
    Instant settledAt
) {
}

