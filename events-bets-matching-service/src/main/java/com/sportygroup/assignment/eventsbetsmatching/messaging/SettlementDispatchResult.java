package com.sportygroup.assignment.eventsbetsmatching.messaging;

public record SettlementDispatchResult(
    String messageId,
    String sendStatus
) {
}

