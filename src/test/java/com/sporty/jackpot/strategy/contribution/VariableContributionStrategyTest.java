package com.sporty.jackpot.strategy.contribution;

import com.sporty.jackpot.entity.ContributionType;
import com.sporty.jackpot.entity.Jackpot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class VariableContributionStrategyTest {

    private VariableContributionStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new VariableContributionStrategy();
    }

    @Test
    void calculateContribution_poolAtZero_returnsMaxContribution() {
        Jackpot jackpot = createJackpot(BigDecimal.ZERO, BigDecimal.valueOf(10000));

        BigDecimal result = strategy.calculateContribution(BigDecimal.valueOf(100), jackpot);

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(10));
    }

    @Test
    void calculateContribution_poolAtMax_returnsMinContribution() {
        Jackpot jackpot = createJackpot(BigDecimal.valueOf(10000), BigDecimal.valueOf(10000));

        BigDecimal result = strategy.calculateContribution(BigDecimal.valueOf(100), jackpot);

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(1));
    }

    @Test
    void calculateContribution_poolAtHalfMax_returnsHalfContribution() {
        Jackpot jackpot = createJackpot(BigDecimal.valueOf(5000), BigDecimal.valueOf(10000));

        BigDecimal result = strategy.calculateContribution(BigDecimal.valueOf(100), jackpot);

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void calculateContribution_noMaxLimit_usesBasePercentage() {
        Jackpot jackpot = Jackpot.builder()
                .contributionPercentage(BigDecimal.valueOf(0.10))
                .currentPoolValue(BigDecimal.valueOf(5000))
                .maxPoolLimit(null)
                .build();

        BigDecimal result = strategy.calculateContribution(BigDecimal.valueOf(100), jackpot);

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(10));
    }

    @Test
    void getType_returnsVariable() {
        assertThat(strategy.getType()).isEqualTo(ContributionType.VARIABLE);
    }

    // ========== Edge Case Tests ==========

    @Test
    void calculateContribution_zeroBetAmount_returnsZero() {
        Jackpot jackpot = createJackpot(BigDecimal.valueOf(5000), BigDecimal.valueOf(10000));

        BigDecimal result = strategy.calculateContribution(BigDecimal.ZERO, jackpot);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculateContribution_poolExceedsMax_usesMinimumMultiplier() {
        Jackpot jackpot = createJackpot(BigDecimal.valueOf(15000), BigDecimal.valueOf(10000));

        BigDecimal result = strategy.calculateContribution(BigDecimal.valueOf(100), jackpot);

        // When pool exceeds max, multiplier would be negative, but minimum is 0.1
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(1));
    }

    @Test
    void calculateContribution_zeroMaxLimit_usesBasePercentage() {
        Jackpot jackpot = Jackpot.builder()
                .contributionPercentage(BigDecimal.valueOf(0.10))
                .currentPoolValue(BigDecimal.valueOf(5000))
                .maxPoolLimit(BigDecimal.ZERO)
                .build();

        BigDecimal result = strategy.calculateContribution(BigDecimal.valueOf(100), jackpot);

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(10));
    }

    @Test
    void calculateContribution_veryLargeBetAmount_handlesCorrectly() {
        Jackpot jackpot = createJackpot(BigDecimal.valueOf(5000), BigDecimal.valueOf(10000));

        BigDecimal result = strategy.calculateContribution(new BigDecimal("1000000000"), jackpot);

        // 1,000,000,000 * 0.10 * 0.5 = 50,000,000
        assertThat(result).isEqualByComparingTo(new BigDecimal("50000000"));
    }

    @Test
    void calculateContribution_verySmallBetAmount_handlesCorrectly() {
        Jackpot jackpot = createJackpot(BigDecimal.ZERO, BigDecimal.valueOf(10000));

        BigDecimal result = strategy.calculateContribution(new BigDecimal("0.0001"), jackpot);

        // 0.0001 * 0.10 * 1.0 = 0.00001, rounds to 0 with scale 4
        assertThat(result).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void calculateContribution_zeroContributionPercentage_returnsZero() {
        Jackpot jackpot = Jackpot.builder()
                .contributionPercentage(BigDecimal.ZERO)
                .currentPoolValue(BigDecimal.valueOf(5000))
                .maxPoolLimit(BigDecimal.valueOf(10000))
                .build();

        BigDecimal result = strategy.calculateContribution(BigDecimal.valueOf(100), jackpot);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculateContribution_poolAt90PercentOfMax_returnsReducedContribution() {
        Jackpot jackpot = createJackpot(BigDecimal.valueOf(9000), BigDecimal.valueOf(10000));

        BigDecimal result = strategy.calculateContribution(BigDecimal.valueOf(100), jackpot);

        // Pool at 90% of max, multiplier = 0.1 (minimum)
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(1));
    }

    @Test
    void calculateContribution_poolAt25PercentOfMax_returnsHigherContribution() {
        Jackpot jackpot = createJackpot(BigDecimal.valueOf(2500), BigDecimal.valueOf(10000));

        BigDecimal result = strategy.calculateContribution(BigDecimal.valueOf(100), jackpot);

        // Pool at 25% of max, multiplier = 0.75
        // 100 * 0.10 * 0.75 = 7.5
        assertThat(result).isEqualByComparingTo(new BigDecimal("7.5"));
    }

    @Test
    void calculateContribution_negativeMaxPoolLimit_usesBasePercentage() {
        Jackpot jackpot = Jackpot.builder()
                .contributionPercentage(BigDecimal.valueOf(0.10))
                .currentPoolValue(BigDecimal.valueOf(5000))
                .maxPoolLimit(BigDecimal.valueOf(-1000))
                .build();

        BigDecimal result = strategy.calculateContribution(BigDecimal.valueOf(100), jackpot);

        // Negative max limit should be treated same as no limit
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(10));
    }

    @Test
    void calculateContribution_veryHighContributionPercentage_calculatesCorrectly() {
        Jackpot jackpot = Jackpot.builder()
                .contributionPercentage(BigDecimal.valueOf(0.50))
                .currentPoolValue(BigDecimal.ZERO)
                .maxPoolLimit(BigDecimal.valueOf(10000))
                .build();

        BigDecimal result = strategy.calculateContribution(BigDecimal.valueOf(100), jackpot);

        // 100 * 0.50 * 1.0 = 50
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    private Jackpot createJackpot(BigDecimal currentPool, BigDecimal maxPool) {
        return Jackpot.builder()
                .contributionPercentage(BigDecimal.valueOf(0.10))
                .currentPoolValue(currentPool)
                .maxPoolLimit(maxPool)
                .build();
    }
}
