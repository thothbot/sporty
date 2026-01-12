package com.sporty.jackpot.dto;

import com.sporty.jackpot.entity.ContributionType;
import com.sporty.jackpot.entity.RewardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JackpotDto {
    private UUID id;
    private String name;
    private BigDecimal initialPoolValue;
    private BigDecimal currentPoolValue;
    private ContributionType contributionType;
    private BigDecimal contributionPercentage;
    private RewardType rewardType;
    private BigDecimal rewardChancePercentage;
    private BigDecimal maxPoolLimit;
    private Instant createdAt;
    private Instant updatedAt;
}
