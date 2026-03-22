package com.sportygroup.assignment.eventsinput.api;

public record ApiErrorResponse(
    String code,
    String message
) {
}
