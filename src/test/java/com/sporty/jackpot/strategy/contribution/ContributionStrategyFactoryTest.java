package com.sporty.jackpot.strategy.contribution;

import com.sporty.jackpot.entity.ContributionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContributionStrategyFactoryTest {

    private ContributionStrategyFactory factory;

    @BeforeEach
    void setUp() {
        factory = new ContributionStrategyFactory(List.of(
                new FixedContributionStrategy(),
                new VariableContributionStrategy()
        ));
    }

    @Test
    void getStrategy_fixedType_returnsFixedStrategy() {
        ContributionStrategy strategy = factory.getStrategy(ContributionType.FIXED);

        assertThat(strategy).isInstanceOf(FixedContributionStrategy.class);
    }

    @Test
    void getStrategy_variableType_returnsVariableStrategy() {
        ContributionStrategy strategy = factory.getStrategy(ContributionType.VARIABLE);

        assertThat(strategy).isInstanceOf(VariableContributionStrategy.class);
    }

    @Test
    void getStrategy_emptyFactory_throwsException() {
        ContributionStrategyFactory emptyFactory = new ContributionStrategyFactory(List.of());

        assertThatThrownBy(() -> emptyFactory.getStrategy(ContributionType.FIXED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No contribution strategy found");
    }
}
