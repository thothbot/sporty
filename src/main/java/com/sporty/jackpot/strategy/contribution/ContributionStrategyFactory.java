package com.sporty.jackpot.strategy.contribution;

import com.sporty.jackpot.entity.ContributionType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ContributionStrategyFactory {

    private final Map<ContributionType, ContributionStrategy> strategies;

    public ContributionStrategyFactory(List<ContributionStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(ContributionStrategy::getType, Function.identity()));
    }

    public ContributionStrategy getStrategy(ContributionType type) {
        return switch (strategies.get(type)) {
            case ContributionStrategy s -> s;
            case null -> throw new IllegalArgumentException(
                    "No contribution strategy found for type: " + type);
        };
    }
}
