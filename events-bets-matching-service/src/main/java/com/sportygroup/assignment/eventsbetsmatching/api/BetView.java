package com.sportygroup.assignment.eventsbetsmatching.api;

import java.math.BigDecimal;

public record BetView(
    Long betId,
    String userId,
    String eventId,
    String eventMarketId,
    String eventWinnerId,
    BigDecimal betAmount,
    boolean settled,
    String settlementStatus
) {
}

