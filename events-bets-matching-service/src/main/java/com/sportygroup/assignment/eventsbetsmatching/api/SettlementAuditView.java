package com.sportygroup.assignment.eventsbetsmatching.api;

import java.time.Instant;

public record SettlementAuditView(
    Long betId,
    String eventId,
    String selectedWinnerId,
    String actualWinnerId,
    String settlementOutcome,
    String rocketmqMessageId,
    Instant createdAt
) {
}

