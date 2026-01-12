package com.sporty.jackpot.strategy.contribution;

import com.sporty.jackpot.entity.ContributionType;
import com.sporty.jackpot.entity.Jackpot;

import java.math.BigDecimal;

public interface ContributionStrategy {
    BigDecimal calculateContribution(BigDecimal betAmount, Jackpot jackpot);
    ContributionType getType();
}
