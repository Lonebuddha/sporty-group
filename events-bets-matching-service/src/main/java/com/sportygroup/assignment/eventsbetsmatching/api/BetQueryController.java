package com.sportygroup.assignment.eventsbetsmatching.api;

import com.sportygroup.assignment.eventsbetsmatching.domain.Bet;
import com.sportygroup.assignment.eventsbetsmatching.domain.SettlementAudit;
import com.sportygroup.assignment.eventsbetsmatching.repository.BetRepository;
import com.sportygroup.assignment.eventsbetsmatching.repository.SettlementAuditRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class BetQueryController {

    private final BetRepository betRepository;
    private final SettlementAuditRepository settlementAuditRepository;

    public BetQueryController(BetRepository betRepository, SettlementAuditRepository settlementAuditRepository) {
        this.betRepository = betRepository;
        this.settlementAuditRepository = settlementAuditRepository;
    }

    @GetMapping("/bets")
    public List<BetView> getBets(@RequestParam String eventId) {
        return betRepository.findByEventIdOrderByBetId(eventId)
            .stream()
            .map(BetQueryController::toBetView)
            .toList();
    }

    @GetMapping("/settlements")
    public List<SettlementAuditView> getSettlements(@RequestParam String eventId) {
        return settlementAuditRepository.findByEventIdOrderByBetId(eventId)
            .stream()
            .map(BetQueryController::toSettlementAuditView)
            .toList();
    }

    private static BetView toBetView(Bet bet) {
        return new BetView(
            bet.getBetId(),
            bet.getUserId(),
            bet.getEventId(),
            bet.getEventMarketId(),
            bet.getEventWinnerId(),
            bet.getBetAmount(),
            bet.isSettled(),
            bet.getSettlementStatus()
        );
    }

    private static SettlementAuditView toSettlementAuditView(SettlementAudit settlementAudit) {
        return new SettlementAuditView(
            settlementAudit.getBetId(),
            settlementAudit.getEventId(),
            settlementAudit.getSelectedWinnerId(),
            settlementAudit.getActualWinnerId(),
            settlementAudit.getSettlementOutcome(),
            settlementAudit.getRocketmqMessageId(),
            settlementAudit.getCreatedAt()
        );
    }
}

