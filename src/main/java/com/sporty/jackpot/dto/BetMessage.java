package com.sporty.jackpot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BetMessage {
    private UUID betId;
    private UUID userId;
    private UUID jackpotId;
    private BigDecimal betAmount;
}
