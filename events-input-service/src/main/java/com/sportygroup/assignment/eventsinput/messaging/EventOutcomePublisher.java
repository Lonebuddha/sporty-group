package com.sportygroup.assignment.eventsinput.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventOutcomePublisher {

    private static final Logger log = LoggerFactory.getLogger(EventOutcomePublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String eventOutcomesTopic;

    public EventOutcomePublisher(
        KafkaTemplate<String, String> kafkaTemplate,
        ObjectMapper objectMapper,
        @Value("${app.messaging.event-outcomes-topic}") String eventOutcomesTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.eventOutcomesTopic = eventOutcomesTopic;
    }

    public void publish(EventOutcomeMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(eventOutcomesTopic, message.eventId(), payload).get(2, TimeUnit.SECONDS);
            log.info("Published event outcome for eventId={} to topic={}", message.eventId(), eventOutcomesTopic);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize event outcome message", exception);
        } catch (Exception exception) {
            log.error("Failed to publish event outcome for eventId={} to topic={}", message.eventId(), eventOutcomesTopic, exception);
            throw new EventOutcomePublishException("Kafka is unavailable; event outcome was not published", exception);
        }
    }
}
