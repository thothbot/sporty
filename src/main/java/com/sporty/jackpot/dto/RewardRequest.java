package com.sporty.jackpot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardRequest {
    @NotNull(message = "Bet ID is required")
    private UUID betId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Jackpot ID is required")
    private UUID jackpotId;
}
