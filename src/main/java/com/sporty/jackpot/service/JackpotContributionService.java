package com.sporty.jackpot.service;

import com.sporty.jackpot.dto.BetMessage;
import com.sporty.jackpot.entity.Jackpot;
import com.sporty.jackpot.entity.JackpotContribution;
import com.sporty.jackpot.exception.JackpotNotFoundException;
import com.sporty.jackpot.repository.JackpotContributionRepository;
import com.sporty.jackpot.repository.JackpotRepository;
import com.sporty.jackpot.strategy.contribution.ContributionStrategy;
import com.sporty.jackpot.strategy.contribution.ContributionStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class JackpotContributionService {

    private final JackpotRepository jackpotRepository;
    private final JackpotContributionRepository contributionRepository;
    private final ContributionStrategyFactory strategyFactory;

    @Transactional
    public List<JackpotContribution> processBatch(List<BetMessage> bets) {
        log.debug("Processing batch of {} bets", bets.size());

        Map<UUID, List<BetMessage>> betsByJackpot = bets.stream()
                .collect(Collectors.groupingBy(BetMessage::getJackpotId));

        List<JackpotContribution> allContributions = new ArrayList<>();

        for (var entry : betsByJackpot.entrySet()) {
            UUID jackpotId = entry.getKey();
            List<BetMessage> jackpotBets = entry.getValue();

            List<JackpotContribution> contributions = processJackpotBets(jackpotId, jackpotBets);
            allContributions.addAll(contributions);
        }

        contributionRepository.saveAll(allContributions);
        log.debug("Saved {} contributions", allContributions.size());

        return allContributions;
    }

    private List<JackpotContribution> processJackpotBets(UUID jackpotId, List<BetMessage> bets) {
        Jackpot jackpot = jackpotRepository.findByIdWithLock(jackpotId)
                .orElseThrow(() -> new JackpotNotFoundException(jackpotId));

        ContributionStrategy strategy = strategyFactory.getStrategy(jackpot.getContributionType());

        List<JackpotContribution> contributions = new ArrayList<>();

        for (BetMessage bet : bets) {
            BigDecimal contribution = strategy.calculateContribution(bet.getBetAmount(), jackpot);

            jackpot.setCurrentPoolValue(jackpot.getCurrentPoolValue().add(contribution));

            contributions.add(JackpotContribution.builder()
                    .betId(bet.getBetId())
                    .userId(bet.getUserId())
                    .jackpotId(jackpotId)
                    .stakeAmount(bet.getBetAmount())
                    .contributionAmount(contribution)
                    .currentJackpotAmount(jackpot.getCurrentPoolValue())
                    .build());
        }

        jackpotRepository.save(jackpot);

        return contributions;
    }

    @Transactional
    public JackpotContribution processContribution(BetMessage bet) {
        List<JackpotContribution> contributions = processBatch(List.of(bet));
        return contributions.isEmpty() ? null : contributions.get(0);
    }
}
