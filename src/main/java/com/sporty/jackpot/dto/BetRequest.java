package com.sporty.jackpot.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class BetRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Jackpot ID is required")
    private UUID jackpotId;

    @NotNull(message = "Bet amount is required")
    @Positive(message = "Bet amount must be positive")
    private BigDecimal betAmount;
}
