package com.sporty.jackpot.service;

import com.sporty.jackpot.dto.BetMessage;
import com.sporty.jackpot.dto.BetRequest;
import com.sporty.jackpot.entity.Bet;
import com.sporty.jackpot.entity.BetStatus;
import com.sporty.jackpot.exception.BetNotFoundException;
import com.sporty.jackpot.repository.BetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BetService {

    private final BetRepository betRepository;

    @Transactional
    public Bet createBet(UUID betId, BetRequest request) {
        Bet bet = Bet.builder()
                .id(betId)
                .userId(request.getUserId())
                .jackpotId(request.getJackpotId())
                .betAmount(request.getBetAmount())
                .status(BetStatus.PENDING)
                .build();

        Bet saved = betRepository.save(bet);
        log.info("Created bet {} for user {} on jackpot {}", betId, request.getUserId(), request.getJackpotId());

        return saved;
    }

    @Transactional
    public void updateStatus(UUID betId, BetStatus status) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new BetNotFoundException(betId));

        bet.setStatus(status);
        betRepository.save(bet);
        log.debug("Updated bet {} status to {}", betId, status);
    }

    @Transactional(readOnly = true)
    public Bet getBet(UUID betId) {
        return betRepository.findById(betId)
                .orElseThrow(() -> new BetNotFoundException(betId));
    }

    public BetMessage toMessage(Bet bet) {
        return BetMessage.builder()
                .betId(bet.getId())
                .userId(bet.getUserId())
                .jackpotId(bet.getJackpotId())
                .betAmount(bet.getBetAmount())
                .build();
    }
}
