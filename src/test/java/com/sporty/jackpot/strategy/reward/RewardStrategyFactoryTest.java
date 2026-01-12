package com.sporty.jackpot.strategy.reward;

import com.sporty.jackpot.entity.RewardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RewardStrategyFactoryTest {

    private RewardStrategyFactory factory;

    @BeforeEach
    void setUp() {
        factory = new RewardStrategyFactory(List.of(
                new FixedRewardStrategy(),
                new VariableRewardStrategy()
        ));
    }

    @Test
    void getStrategy_fixedType_returnsFixedStrategy() {
        RewardStrategy strategy = factory.getStrategy(RewardType.FIXED);

        assertThat(strategy).isInstanceOf(FixedRewardStrategy.class);
    }

    @Test
    void getStrategy_variableType_returnsVariableStrategy() {
        RewardStrategy strategy = factory.getStrategy(RewardType.VARIABLE);

        assertThat(strategy).isInstanceOf(VariableRewardStrategy.class);
    }

    @Test
    void getStrategy_emptyFactory_throwsException() {
        RewardStrategyFactory emptyFactory = new RewardStrategyFactory(List.of());

        assertThatThrownBy(() -> emptyFactory.getStrategy(RewardType.FIXED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No reward strategy found");
    }
}
