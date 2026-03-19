package com.sportygroup.assignment.eventsinput.api;

public record EventOutcomeResponse(
    String eventId,
    String status,
    String message
) {
}

