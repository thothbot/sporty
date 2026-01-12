package com.sporty.jackpot.strategy.contribution;

import com.sporty.jackpot.entity.ContributionType;
import com.sporty.jackpot.entity.Jackpot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FixedContributionStrategy implements ContributionStrategy {

    @Override
    public BigDecimal calculateContribution(BigDecimal betAmount, Jackpot jackpot) {
        return betAmount.multiply(jackpot.getContributionPercentage())
                .setScale(4, RoundingMode.HALF_UP);
    }

    @Override
    public ContributionType getType() {
        return ContributionType.FIXED;
    }
}
