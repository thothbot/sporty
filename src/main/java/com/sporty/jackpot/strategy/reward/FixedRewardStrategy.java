package com.sporty.jackpot.strategy.reward;

import com.sporty.jackpot.entity.Jackpot;
import com.sporty.jackpot.entity.RewardType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Component
public class FixedRewardStrategy implements RewardStrategy {

    private final SecureRandom random = new SecureRandom();

    @Override
    public boolean evaluateWin(Jackpot jackpot) {
        BigDecimal chancePercentage = jackpot.getRewardChancePercentage();
        double threshold = chancePercentage.doubleValue();
        double roll = random.nextDouble();
        return roll < threshold;
    }

    @Override
    public RewardType getType() {
        return RewardType.FIXED;
    }
}
