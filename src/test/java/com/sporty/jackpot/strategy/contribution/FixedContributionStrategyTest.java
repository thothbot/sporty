package com.sporty.jackpot.strategy.contribution;

import com.sporty.jackpot.entity.ContributionType;
import com.sporty.jackpot.entity.Jackpot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FixedContributionStrategyTest {

    private FixedContributionStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new FixedContributionStrategy();
    }

    @ParameterizedTest
    @CsvSource({
        "100.00, 0.05, 5.0000",
        "1000.00, 0.10, 100.0000",
        "50.00, 0.02, 1.0000",
        "0.00, 0.05, 0.0000",
        "1.00, 0.01, 0.0100"
    })
    void calculateContribution_variousAmounts_returnsCorrectContribution(
            String betAmount, String percentage, String expected) {
        Jackpot jackpot = createJackpot(new BigDecimal(percentage));

        BigDecimal result = strategy.calculateContribution(new BigDecimal(betAmount), jackpot);

        assertThat(result).isEqualByComparingTo(new BigDecimal(expected));
    }

    @Test
    void getType_returnsFixed() {
        assertThat(strategy.getType()).isEqualTo(ContributionType.FIXED);
    }

    // ========== Edge Case Tests ==========

    @Test
    void calculateContribution_veryLargeBetAmount_handlesCorrectly() {
        Jackpot jackpot = createJackpot(BigDecimal.valueOf(0.05));

        BigDecimal result = strategy.calculateContribution(
                new BigDecimal("99999999999999.9999"), jackpot);

        // 99999999999999.9999 * 0.05 = 4999999999999.99995, rounds to 5000000000000.0000
        assertThat(result).isEqualByComparingTo(new BigDecimal("5000000000000.0000"));
    }

    @Test
    void calculateContribution_verySmallBetAmount_handlesCorrectly() {
        Jackpot jackpot = createJackpot(BigDecimal.valueOf(0.05));

        BigDecimal result = strategy.calculateContribution(new BigDecimal("0.0001"), jackpot);

        assertThat(result).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void calculateContribution_zeroContributionPercentage_returnsZero() {
        Jackpot jackpot = createJackpot(BigDecimal.ZERO);

        BigDecimal result = strategy.calculateContribution(BigDecimal.valueOf(100), jackpot);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculateContribution_fullContributionPercentage_returnsFullAmount() {
        Jackpot jackpot = createJackpot(BigDecimal.ONE);

        BigDecimal result = strategy.calculateContribution(BigDecimal.valueOf(100), jackpot);

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void calculateContribution_highPrecisionPercentage_maintainsPrecision() {
        Jackpot jackpot = createJackpot(new BigDecimal("0.0333"));

        BigDecimal result = strategy.calculateContribution(BigDecimal.valueOf(100), jackpot);

        assertThat(result).isEqualByComparingTo(new BigDecimal("3.33"));
    }

    @Test
    void calculateContribution_ignoresPoolValues() {
        // Fixed strategy should not be affected by current pool
        Jackpot lowPool = Jackpot.builder()
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .currentPoolValue(BigDecimal.ZERO)
                .build();

        Jackpot highPool = Jackpot.builder()
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .currentPoolValue(BigDecimal.valueOf(1000000))
                .build();

        BigDecimal resultLow = strategy.calculateContribution(BigDecimal.valueOf(100), lowPool);
        BigDecimal resultHigh = strategy.calculateContribution(BigDecimal.valueOf(100), highPool);

        assertThat(resultLow).isEqualByComparingTo(resultHigh);
        assertThat(resultLow).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void calculateContribution_ignoresMaxPoolLimit() {
        // Fixed strategy should not be affected by max pool limit
        Jackpot withLimit = Jackpot.builder()
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .currentPoolValue(BigDecimal.valueOf(1000))
                .maxPoolLimit(BigDecimal.valueOf(10000))
                .build();

        Jackpot withoutLimit = Jackpot.builder()
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .currentPoolValue(BigDecimal.valueOf(1000))
                .maxPoolLimit(null)
                .build();

        BigDecimal resultWith = strategy.calculateContribution(BigDecimal.valueOf(100), withLimit);
        BigDecimal resultWithout = strategy.calculateContribution(BigDecimal.valueOf(100), withoutLimit);

        assertThat(resultWith).isEqualByComparingTo(resultWithout);
    }

    private Jackpot createJackpot(BigDecimal contributionPercentage) {
        return Jackpot.builder()
                .contributionPercentage(contributionPercentage)
                .currentPoolValue(BigDecimal.valueOf(1000))
                .build();
    }
}
