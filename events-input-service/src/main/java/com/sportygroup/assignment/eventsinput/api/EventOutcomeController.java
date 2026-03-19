package com.sportygroup.assignment.eventsinput.api;

import com.sportygroup.assignment.eventsinput.messaging.EventOutcomeMessage;
import com.sportygroup.assignment.eventsinput.messaging.EventOutcomePublisher;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/event-outcomes")
public class EventOutcomeController {

    private final EventOutcomePublisher eventOutcomePublisher;

    public EventOutcomeController(EventOutcomePublisher eventOutcomePublisher) {
        this.eventOutcomePublisher = eventOutcomePublisher;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EventOutcomeResponse publish(@Valid @RequestBody EventOutcomeRequest request) {
        EventOutcomeMessage message = new EventOutcomeMessage(
            request.eventId(),
            request.eventName(),
            request.eventWinnerId(),
            Instant.now()
        );
        eventOutcomePublisher.publish(message);
        return new EventOutcomeResponse(
            message.eventId(),
            "ACCEPTED",
            "Event outcome published to Kafka"
        );
    }
}

