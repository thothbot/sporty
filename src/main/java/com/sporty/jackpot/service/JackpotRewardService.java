package com.sporty.jackpot.service;

import com.sporty.jackpot.entity.Jackpot;
import com.sporty.jackpot.entity.JackpotContribution;
import com.sporty.jackpot.entity.JackpotReward;
import com.sporty.jackpot.exception.ContributionNotFoundException;
import com.sporty.jackpot.exception.JackpotNotFoundException;
import com.sporty.jackpot.repository.JackpotContributionRepository;
import com.sporty.jackpot.repository.JackpotRepository;
import com.sporty.jackpot.repository.JackpotRewardRepository;
import com.sporty.jackpot.strategy.reward.RewardStrategy;
import com.sporty.jackpot.strategy.reward.RewardStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class JackpotRewardService {

    private final JackpotRepository jackpotRepository;
    private final JackpotContributionRepository contributionRepository;
    private final JackpotRewardRepository rewardRepository;
    private final RewardStrategyFactory strategyFactory;

    @Transactional
    public Optional<JackpotReward> evaluateReward(UUID betId, UUID userId, UUID jackpotId) {
        Optional<JackpotReward> existingReward = rewardRepository.findByBetId(betId);
        if (existingReward.isPresent()) {
            log.debug("Reward already evaluated for bet {}", betId);
            return existingReward;
        }

        List<JackpotContribution> contributions = contributionRepository.findByBetId(betId);
        if (contributions.isEmpty()) {
            throw new ContributionNotFoundException(betId);
        }

        Jackpot jackpot = jackpotRepository.findByIdWithLock(jackpotId)
                .orElseThrow(() -> new JackpotNotFoundException(jackpotId));

        RewardStrategy strategy = strategyFactory.getStrategy(jackpot.getRewardType());

        boolean won = strategy.evaluateWin(jackpot);

        if (!won) {
            log.debug("Bet {} did not win jackpot {}", betId, jackpotId);
            return Optional.empty();
        }

        BigDecimal rewardAmount = jackpot.getCurrentPoolValue();

        JackpotReward reward = JackpotReward.builder()
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .rewardAmount(rewardAmount)
                .build();

        jackpot.setCurrentPoolValue(jackpot.getInitialPoolValue());
        jackpotRepository.save(jackpot);

        JackpotReward savedReward = rewardRepository.save(reward);

        log.info("Bet {} won jackpot {}! Reward: {}", betId, jackpotId, rewardAmount);

        return Optional.of(savedReward);
    }
}
