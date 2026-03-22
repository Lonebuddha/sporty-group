package com.sportygroup.assignment.eventsbetsmatching.api;

import com.sportygroup.assignment.eventsbetsmatching.domain.Bet;
import com.sportygroup.assignment.eventsbetsmatching.repository.BetRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class BetQueryController {

    private final BetRepository betRepository;

    public BetQueryController(BetRepository betRepository) {
        this.betRepository = betRepository;
    }

    @GetMapping("/bets")
    public List<BetView> getBets(@RequestParam String eventId) {
        return betRepository.findByEventIdOrderByBetId(eventId)
            .stream()
            .map(BetQueryController::toBetView)
            .toList();
    }

    private static BetView toBetView(Bet bet) {
        return new BetView(
            bet.getBetId(),
            bet.getUserId(),
            bet.getEventId(),
            bet.getEventMarketId(),
            bet.getEventWinnerId(),
            bet.getBetAmount()
        );
    }
}
