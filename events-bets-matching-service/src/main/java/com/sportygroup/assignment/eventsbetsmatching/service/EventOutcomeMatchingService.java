package com.sportygroup.assignment.eventsbetsmatching.service;

import com.sportygroup.assignment.eventsbetsmatching.domain.Bet;
import com.sportygroup.assignment.eventsbetsmatching.domain.SettlementAudit;
import com.sportygroup.assignment.eventsbetsmatching.domain.SettlementOutcome;
import com.sportygroup.assignment.eventsbetsmatching.messaging.BetSettlementMessage;
import com.sportygroup.assignment.eventsbetsmatching.messaging.EventOutcomeMessage;
import com.sportygroup.assignment.eventsbetsmatching.messaging.RocketMqBetSettlementProducer;
import com.sportygroup.assignment.eventsbetsmatching.messaging.SettlementDispatchResult;
import com.sportygroup.assignment.eventsbetsmatching.repository.BetRepository;
import com.sportygroup.assignment.eventsbetsmatching.repository.SettlementAuditRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EventOutcomeMatchingService {

    private static final Logger log = LoggerFactory.getLogger(EventOutcomeMatchingService.class);

    private final BetRepository betRepository;
    private final SettlementAuditRepository settlementAuditRepository;
    private final RocketMqBetSettlementProducer betSettlementProducer;

    public EventOutcomeMatchingService(
        BetRepository betRepository,
        SettlementAuditRepository settlementAuditRepository,
        RocketMqBetSettlementProducer betSettlementProducer
    ) {
        this.betRepository = betRepository;
        this.settlementAuditRepository = settlementAuditRepository;
        this.betSettlementProducer = betSettlementProducer;
    }

    public void handle(EventOutcomeMessage eventOutcomeMessage) {
        List<Bet> bets = betRepository.findByEventIdAndSettledFalseOrderByBetId(eventOutcomeMessage.eventId());

        if (bets.isEmpty()) {
            log.info("No unsettled bets found for eventId={}", eventOutcomeMessage.eventId());
            return;
        }

        log.info("Matching {} unsettled bets for eventId={}", bets.size(), eventOutcomeMessage.eventId());
        for (Bet bet : bets) {
            settleBet(bet, eventOutcomeMessage);
        }
    }

    private void settleBet(Bet bet, EventOutcomeMessage eventOutcomeMessage) {
        Instant settledAt = Instant.now();
        SettlementOutcome settlementOutcome = bet.getEventWinnerId().equals(eventOutcomeMessage.eventWinnerId())
            ? SettlementOutcome.WON
            : SettlementOutcome.LOST;

        BetSettlementMessage settlementMessage = new BetSettlementMessage(
            bet.getBetId(),
            bet.getUserId(),
            bet.getEventId(),
            eventOutcomeMessage.eventName(),
            bet.getEventMarketId(),
            bet.getEventWinnerId(),
            eventOutcomeMessage.eventWinnerId(),
            settlementOutcome.name(),
            bet.getBetAmount(),
            settledAt
        );

        SettlementDispatchResult dispatchResult = betSettlementProducer.send(settlementMessage);
        bet.markSettled(settlementOutcome, settledAt, dispatchResult.messageId());
        betRepository.save(bet);

        SettlementAudit settlementAudit = SettlementAudit.from(
            bet,
            eventOutcomeMessage.eventWinnerId(),
            settlementOutcome,
            dispatchResult.messageId(),
            dispatchResult.sendStatus(),
            settledAt
        );
        settlementAuditRepository.save(settlementAudit);

        log.info(
            "Settled betId={} for eventId={} with outcome={} and RocketMQ messageId={}",
            bet.getBetId(),
            bet.getEventId(),
            settlementOutcome,
            dispatchResult.messageId()
        );
    }
}

