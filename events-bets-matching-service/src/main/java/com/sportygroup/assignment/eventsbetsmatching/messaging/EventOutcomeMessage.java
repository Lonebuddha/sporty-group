package com.sportygroup.assignment.eventsbetsmatching.messaging;

import java.time.Instant;

public record EventOutcomeMessage(
    String eventId,
    String eventName,
    String eventWinnerId,
    Instant publishedAt
) {
}

