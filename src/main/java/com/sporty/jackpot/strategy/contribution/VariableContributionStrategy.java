package com.sporty.jackpot.strategy.contribution;

import com.sporty.jackpot.entity.ContributionType;
import com.sporty.jackpot.entity.Jackpot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class VariableContributionStrategy implements ContributionStrategy {

    private static final BigDecimal MINIMUM_MULTIPLIER = new BigDecimal("0.1");

    @Override
    public BigDecimal calculateContribution(BigDecimal betAmount, Jackpot jackpot) {
        BigDecimal basePercentage = jackpot.getContributionPercentage();
        BigDecimal currentPool = jackpot.getCurrentPoolValue();
        BigDecimal maxPool = jackpot.getMaxPoolLimit();

        if (maxPool == null || maxPool.compareTo(BigDecimal.ZERO) <= 0) {
            return betAmount.multiply(basePercentage).setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal poolRatio = currentPool.divide(maxPool, 4, RoundingMode.HALF_UP);
        BigDecimal multiplier = BigDecimal.ONE.subtract(poolRatio);

        if (multiplier.compareTo(MINIMUM_MULTIPLIER) < 0) {
            multiplier = MINIMUM_MULTIPLIER;
        }

        return betAmount.multiply(basePercentage)
                .multiply(multiplier)
                .setScale(4, RoundingMode.HALF_UP);
    }

    @Override
    public ContributionType getType() {
        return ContributionType.VARIABLE;
    }
}
