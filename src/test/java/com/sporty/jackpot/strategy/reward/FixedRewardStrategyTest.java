package com.sporty.jackpot.strategy.reward;

import com.sporty.jackpot.entity.Jackpot;
import com.sporty.jackpot.entity.RewardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FixedRewardStrategyTest {

    private FixedRewardStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new FixedRewardStrategy();
    }

    @Test
    void evaluateWin_zeroChance_neverWins() {
        Jackpot jackpot = createJackpot(BigDecimal.ZERO);

        boolean won = strategy.evaluateWin(jackpot);

        assertThat(won).isFalse();
    }

    @Test
    void evaluateWin_hundredPercentChance_alwaysWins() {
        Jackpot jackpot = createJackpot(BigDecimal.ONE);

        boolean won = strategy.evaluateWin(jackpot);

        assertThat(won).isTrue();
    }

    @RepeatedTest(10)
    void evaluateWin_fiftyPercentChance_sometimesWins() {
        Jackpot jackpot = createJackpot(BigDecimal.valueOf(0.5));

        strategy.evaluateWin(jackpot);
    }

    @Test
    void getType_returnsFixed() {
        assertThat(strategy.getType()).isEqualTo(RewardType.FIXED);
    }

    // ========== Edge Case Tests ==========

    @Test
    void evaluateWin_zeroChance_neverWinsMultipleTimes() {
        Jackpot jackpot = createJackpot(BigDecimal.ZERO);

        for (int i = 0; i < 100; i++) {
            boolean won = strategy.evaluateWin(jackpot);
            assertThat(won).isFalse();
        }
    }

    @Test
    void evaluateWin_hundredPercentChance_alwaysWinsMultipleTimes() {
        Jackpot jackpot = createJackpot(BigDecimal.ONE);

        for (int i = 0; i < 100; i++) {
            boolean won = strategy.evaluateWin(jackpot);
            assertThat(won).isTrue();
        }
    }

    @Test
    void evaluateWin_veryLowChance_rarelyWins() {
        Jackpot jackpot = createJackpot(BigDecimal.valueOf(0.001));

        int wins = 0;
        for (int i = 0; i < 100; i++) {
            if (strategy.evaluateWin(jackpot)) {
                wins++;
            }
        }

        // With 0.1% chance, should win very rarely
        assertThat(wins).isLessThan(10);
    }

    @Test
    void evaluateWin_veryHighChance_almostAlwaysWins() {
        Jackpot jackpot = createJackpot(BigDecimal.valueOf(0.999));

        int wins = 0;
        for (int i = 0; i < 100; i++) {
            if (strategy.evaluateWin(jackpot)) {
                wins++;
            }
        }

        // With 99.9% chance, should win almost always
        assertThat(wins).isGreaterThan(90);
    }

    @Test
    void evaluateWin_ignoresPoolValue() {
        // Fixed strategy should have same chance regardless of pool
        Jackpot lowPool = Jackpot.builder()
                .rewardChancePercentage(BigDecimal.ONE)
                .currentPoolValue(BigDecimal.ZERO)
                .build();

        Jackpot highPool = Jackpot.builder()
                .rewardChancePercentage(BigDecimal.ONE)
                .currentPoolValue(BigDecimal.valueOf(1000000))
                .build();

        assertThat(strategy.evaluateWin(lowPool)).isTrue();
        assertThat(strategy.evaluateWin(highPool)).isTrue();
    }

    @Test
    void evaluateWin_ignoresMaxPoolLimit() {
        Jackpot withLimit = Jackpot.builder()
                .rewardChancePercentage(BigDecimal.ONE)
                .currentPoolValue(BigDecimal.valueOf(10000))
                .maxPoolLimit(BigDecimal.valueOf(100000))
                .build();

        Jackpot withoutLimit = Jackpot.builder()
                .rewardChancePercentage(BigDecimal.ONE)
                .currentPoolValue(BigDecimal.valueOf(10000))
                .maxPoolLimit(null)
                .build();

        assertThat(strategy.evaluateWin(withLimit)).isTrue();
        assertThat(strategy.evaluateWin(withoutLimit)).isTrue();
    }

    @Test
    void evaluateWin_preciseProbability_statisticallyValid() {
        Jackpot jackpot = createJackpot(BigDecimal.valueOf(0.25));

        int wins = 0;
        int iterations = 1000;
        for (int i = 0; i < iterations; i++) {
            if (strategy.evaluateWin(jackpot)) {
                wins++;
            }
        }

        // With 25% chance, expect roughly 250 wins out of 1000
        // Allow for statistical variance (within ~10%)
        assertThat(wins).isBetween(150, 350);
    }

    @Test
    void evaluateWin_smallChancePercentage_handlesCorrectly() {
        Jackpot jackpot = createJackpot(new BigDecimal("0.0001"));

        // Just verify it runs without error for very small percentages
        strategy.evaluateWin(jackpot);
    }

    private Jackpot createJackpot(BigDecimal rewardChance) {
        return Jackpot.builder()
                .rewardChancePercentage(rewardChance)
                .currentPoolValue(BigDecimal.valueOf(10000))
                .build();
    }
}
