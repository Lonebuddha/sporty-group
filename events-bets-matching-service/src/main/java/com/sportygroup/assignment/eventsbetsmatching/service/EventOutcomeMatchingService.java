package com.sportygroup.assignment.eventsbetsmatching.service;

import com.sportygroup.assignment.eventsbetsmatching.domain.Bet;
import com.sportygroup.assignment.eventsbetsmatching.messaging.BetSettlementMessage;
import com.sportygroup.assignment.eventsbetsmatching.messaging.EventOutcomeMessage;
import com.sportygroup.assignment.eventsbetsmatching.messaging.RocketMqBetSettlementProducer;
import com.sportygroup.assignment.eventsbetsmatching.messaging.SettlementDispatchResult;
import com.sportygroup.assignment.eventsbetsmatching.repository.BetRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EventOutcomeMatchingService {

    private static final Logger log = LoggerFactory.getLogger(EventOutcomeMatchingService.class);

    private final BetRepository betRepository;
    private final RocketMqBetSettlementProducer betSettlementProducer;

    public EventOutcomeMatchingService(
        BetRepository betRepository,
        RocketMqBetSettlementProducer betSettlementProducer
    ) {
        this.betRepository = betRepository;
        this.betSettlementProducer = betSettlementProducer;
    }

    public void handle(EventOutcomeMessage eventOutcomeMessage) {
        List<Bet> bets = betRepository.findByEventIdOrderByBetId(eventOutcomeMessage.eventId());

        if (bets.isEmpty()) {
            log.info("No bets found for eventId={}", eventOutcomeMessage.eventId());
            return;
        }

        log.info("Publishing settlement messages for {} bets for eventId={}", bets.size(), eventOutcomeMessage.eventId());
        for (Bet bet : bets) {
            publishSettlement(bet, eventOutcomeMessage);
        }
    }

    private void publishSettlement(Bet bet, EventOutcomeMessage eventOutcomeMessage) {
        Instant settledAt = Instant.now();
        String settlementOutcome = bet.getEventWinnerId().equals(eventOutcomeMessage.eventWinnerId()) ? "WON" : "LOST";

        BetSettlementMessage settlementMessage = new BetSettlementMessage(
            bet.getBetId(),
            bet.getUserId(),
            bet.getEventId(),
            eventOutcomeMessage.eventName(),
            bet.getEventMarketId(),
            bet.getEventWinnerId(),
            eventOutcomeMessage.eventWinnerId(),
            settlementOutcome,
            bet.getBetAmount(),
            settledAt
        );

        SettlementDispatchResult dispatchResult = betSettlementProducer.send(settlementMessage);
        log.info(
            "Published settlement message for eventId={} betId={} outcome={} messageId={}",
            bet.getEventId(),
            bet.getBetId(),
            settlementOutcome,
            dispatchResult.messageId()
        );
    }
}
