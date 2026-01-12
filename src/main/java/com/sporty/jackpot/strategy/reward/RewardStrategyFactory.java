package com.sporty.jackpot.strategy.reward;

import com.sporty.jackpot.entity.RewardType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RewardStrategyFactory {

    private final Map<RewardType, RewardStrategy> strategies;

    public RewardStrategyFactory(List<RewardStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(RewardStrategy::getType, Function.identity()));
    }

    public RewardStrategy getStrategy(RewardType type) {
        RewardStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No reward strategy found for type: " + type);
        }
        return strategy;
    }
}
