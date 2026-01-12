package com.sporty.jackpot.strategy.reward;

import com.sporty.jackpot.entity.Jackpot;
import com.sporty.jackpot.entity.RewardType;

public interface RewardStrategy {
    boolean evaluateWin(Jackpot jackpot);
    RewardType getType();
}
