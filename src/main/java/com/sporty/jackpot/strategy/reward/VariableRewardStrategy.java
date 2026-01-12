package com.sporty.jackpot.strategy.reward;

import com.sporty.jackpot.entity.Jackpot;
import com.sporty.jackpot.entity.RewardType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;

@Component
public class VariableRewardStrategy implements RewardStrategy {

    private final SecureRandom random = new SecureRandom();

    @Override
    public boolean evaluateWin(Jackpot jackpot) {
        BigDecimal baseChance = jackpot.getRewardChancePercentage();
        BigDecimal currentPool = jackpot.getCurrentPoolValue();
        BigDecimal maxPool = jackpot.getMaxPoolLimit();

        if (maxPool == null || maxPool.compareTo(BigDecimal.ZERO) <= 0) {
            double roll = random.nextDouble();
            return roll < baseChance.doubleValue();
        }

        if (currentPool.compareTo(maxPool) >= 0) {
            return true;
        }

        BigDecimal poolRatio = currentPool.divide(maxPool, 4, RoundingMode.HALF_UP);
        BigDecimal oneMinusBase = BigDecimal.ONE.subtract(baseChance);
        BigDecimal additionalChance = poolRatio.multiply(oneMinusBase);
        BigDecimal totalChance = baseChance.add(additionalChance);

        double threshold = totalChance.doubleValue();
        double roll = random.nextDouble();
        return roll < threshold;
    }

    @Override
    public RewardType getType() {
        return RewardType.VARIABLE;
    }
}
