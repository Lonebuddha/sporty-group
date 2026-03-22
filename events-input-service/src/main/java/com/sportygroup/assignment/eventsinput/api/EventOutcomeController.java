package com.sportygroup.assignment.eventsinput.api;

import com.sportygroup.assignment.eventsinput.messaging.EventOutcomeMessage;
import com.sportygroup.assignment.eventsinput.messaging.EventOutcomePublisher;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ResponseEntity<Void> publish(@Valid @RequestBody EventOutcomeRequest request) {
        EventOutcomeMessage message = new EventOutcomeMessage(
            request.eventId(),
            request.eventName(),
            request.eventWinnerId(),
            Instant.now()
        );
        eventOutcomePublisher.publish(message);
        return ResponseEntity.accepted().build();
    }
}
