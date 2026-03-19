package com.sportygroup.assignment.eventsbetsmatching.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportygroup.assignment.eventsbetsmatching.service.EventOutcomeMatchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventOutcomeConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventOutcomeConsumer.class);

    private final ObjectMapper objectMapper;
    private final EventOutcomeMatchingService eventOutcomeMatchingService;

    public EventOutcomeConsumer(ObjectMapper objectMapper, EventOutcomeMatchingService eventOutcomeMatchingService) {
        this.objectMapper = objectMapper;
        this.eventOutcomeMatchingService = eventOutcomeMatchingService;
    }

    @KafkaListener(topics = "${app.messaging.event-outcomes-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String payload) throws Exception {
        EventOutcomeMessage eventOutcomeMessage = objectMapper.readValue(payload, EventOutcomeMessage.class);
        log.info(
            "Received event outcome from Kafka for eventId={} winnerId={}",
            eventOutcomeMessage.eventId(),
            eventOutcomeMessage.eventWinnerId()
        );
        eventOutcomeMatchingService.handle(eventOutcomeMessage);
    }
}

