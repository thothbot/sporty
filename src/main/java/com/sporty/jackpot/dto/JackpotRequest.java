package com.sporty.jackpot.dto;

import com.sporty.jackpot.entity.ContributionType;
import com.sporty.jackpot.entity.RewardType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JackpotRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Initial pool value is required")
    @PositiveOrZero(message = "Initial pool value must be zero or positive")
    private BigDecimal initialPoolValue;

    @NotNull(message = "Contribution type is required")
    private ContributionType contributionType;

    @NotNull(message = "Contribution percentage is required")
    @DecimalMin(value = "0.0001", message = "Contribution percentage must be at least 0.0001 (0.01%)")
    @DecimalMax(value = "1.0000", message = "Contribution percentage must be at most 1.0 (100%)")
    private BigDecimal contributionPercentage;

    @NotNull(message = "Reward type is required")
    private RewardType rewardType;

    @NotNull(message = "Reward chance percentage is required")
    @DecimalMin(value = "0.0000", inclusive = true, message = "Reward chance percentage must be at least 0 (0%)")
    @DecimalMax(value = "1.0000", message = "Reward chance percentage must be at most 1.0 (100%)")
    private BigDecimal rewardChancePercentage;

    @Positive(message = "Max pool limit must be positive if specified")
    private BigDecimal maxPoolLimit;
}
