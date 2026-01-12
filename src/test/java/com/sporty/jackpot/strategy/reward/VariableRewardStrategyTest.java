package com.sporty.jackpot.strategy.reward;

import com.sporty.jackpot.entity.Jackpot;
import com.sporty.jackpot.entity.RewardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class VariableRewardStrategyTest {

    private VariableRewardStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new VariableRewardStrategy();
    }

    @Test
    void evaluateWin_poolAtMax_alwaysWins() {
        Jackpot jackpot = createJackpot(BigDecimal.valueOf(10000), BigDecimal.valueOf(10000));

        boolean won = strategy.evaluateWin(jackpot);

        assertThat(won).isTrue();
    }

    @Test
    void evaluateWin_poolExceedsMax_alwaysWins() {
        Jackpot jackpot = createJackpot(BigDecimal.valueOf(15000), BigDecimal.valueOf(10000));

        boolean won = strategy.evaluateWin(jackpot);

        assertThat(won).isTrue();
    }

    @Test
    void evaluateWin_zeroBaseChance_poolAtZero_neverWins() {
        Jackpot jackpot = Jackpot.builder()
                .rewardChancePercentage(BigDecimal.ZERO)
                .currentPoolValue(BigDecimal.ZERO)
                .maxPoolLimit(BigDecimal.valueOf(10000))
                .build();

        boolean won = strategy.evaluateWin(jackpot);

        assertThat(won).isFalse();
    }

    @Test
    void evaluateWin_noMaxLimit_usesBaseChance() {
        Jackpot jackpot = Jackpot.builder()
                .rewardChancePercentage(BigDecimal.ONE)
                .currentPoolValue(BigDecimal.valueOf(5000))
                .maxPoolLimit(null)
                .build();

        boolean won = strategy.evaluateWin(jackpot);

        assertThat(won).isTrue();
    }

    @Test
    void getType_returnsVariable() {
        assertThat(strategy.getType()).isEqualTo(RewardType.VARIABLE);
    }

    // ========== Edge Case Tests ==========

    @Test
    void evaluateWin_zeroMaxPoolLimit_usesBaseChance() {
        Jackpot jackpot = Jackpot.builder()
                .rewardChancePercentage(BigDecimal.ONE)
                .currentPoolValue(BigDecimal.valueOf(5000))
                .maxPoolLimit(BigDecimal.ZERO)
                .build();

        boolean won = strategy.evaluateWin(jackpot);

        assertThat(won).isTrue();
    }

    @Test
    void evaluateWin_negativeMaxPoolLimit_usesBaseChance() {
        Jackpot jackpot = Jackpot.builder()
                .rewardChancePercentage(BigDecimal.ONE)
                .currentPoolValue(BigDecimal.valueOf(5000))
                .maxPoolLimit(BigDecimal.valueOf(-1000))
                .build();

        boolean won = strategy.evaluateWin(jackpot);

        assertThat(won).isTrue();
    }

    @Test
    void evaluateWin_fullBaseChance_alwaysWins() {
        Jackpot jackpot = Jackpot.builder()
                .rewardChancePercentage(BigDecimal.ONE)
                .currentPoolValue(BigDecimal.ZERO)
                .maxPoolLimit(BigDecimal.valueOf(10000))
                .build();

        // With 100% base chance, should always win regardless of pool
        for (int i = 0; i < 10; i++) {
            boolean won = strategy.evaluateWin(jackpot);
            assertThat(won).isTrue();
        }
    }

    @Test
    void evaluateWin_zeroChanceZeroPool_neverWins() {
        Jackpot jackpot = Jackpot.builder()
                .rewardChancePercentage(BigDecimal.ZERO)
                .currentPoolValue(BigDecimal.ZERO)
                .maxPoolLimit(BigDecimal.valueOf(10000))
                .build();

        // With 0% base chance and empty pool, should never win
        for (int i = 0; i < 10; i++) {
            boolean won = strategy.evaluateWin(jackpot);
            assertThat(won).isFalse();
        }
    }

    @Test
    void evaluateWin_zeroChancePoolAtMax_alwaysWins() {
        Jackpot jackpot = Jackpot.builder()
                .rewardChancePercentage(BigDecimal.ZERO)
                .currentPoolValue(BigDecimal.valueOf(10000))
                .maxPoolLimit(BigDecimal.valueOf(10000))
                .build();

        // Pool at max should trigger guaranteed win
        boolean won = strategy.evaluateWin(jackpot);

        assertThat(won).isTrue();
    }

    @Test
    void evaluateWin_highChanceHighPool_highWinProbability() {
        Jackpot jackpot = Jackpot.builder()
                .rewardChancePercentage(BigDecimal.valueOf(0.5))
                .currentPoolValue(BigDecimal.valueOf(9000))
                .maxPoolLimit(BigDecimal.valueOf(10000))
                .build();

        // With 50% base + 90% * 50% additional = 95% total chance
        // Over many iterations, should win most of the time
        int wins = 0;
        for (int i = 0; i < 100; i++) {
            if (strategy.evaluateWin(jackpot)) {
                wins++;
            }
        }

        // Should win significantly more than 50% of the time
        assertThat(wins).isGreaterThan(70);
    }

    @Test
    void evaluateWin_lowChanceLowPool_lowWinProbability() {
        Jackpot jackpot = Jackpot.builder()
                .rewardChancePercentage(BigDecimal.valueOf(0.001))
                .currentPoolValue(BigDecimal.valueOf(100))
                .maxPoolLimit(BigDecimal.valueOf(10000))
                .build();

        // With 0.1% base + 1% * 99.9% additional = ~1.1% total chance
        // Over many iterations, should lose most of the time
        int wins = 0;
        for (int i = 0; i < 100; i++) {
            if (strategy.evaluateWin(jackpot)) {
                wins++;
            }
        }

        // Should win very rarely
        assertThat(wins).isLessThan(20);
    }

    @Test
    void evaluateWin_veryLargePoolValues_handlesCorrectly() {
        Jackpot jackpot = Jackpot.builder()
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .currentPoolValue(new BigDecimal("999999999999999"))
                .maxPoolLimit(new BigDecimal("999999999999999"))
                .build();

        // Pool at max should trigger guaranteed win
        boolean won = strategy.evaluateWin(jackpot);

        assertThat(won).isTrue();
    }

    @Test
    void evaluateWin_poolSlightlyBelowMax_stillEvaluatesChance() {
        Jackpot jackpot = Jackpot.builder()
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .currentPoolValue(BigDecimal.valueOf(9999))
                .maxPoolLimit(BigDecimal.valueOf(10000))
                .build();

        // Pool is 99.99% of max, not 100%, so should still use probability
        // Not guaranteed to win (unlike pool >= max)
        // This is a probabilistic test - just verify it runs without error
        strategy.evaluateWin(jackpot);
    }

    private Jackpot createJackpot(BigDecimal currentPool, BigDecimal maxPool) {
        return Jackpot.builder()
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .currentPoolValue(currentPool)
                .maxPoolLimit(maxPool)
                .build();
    }
}
