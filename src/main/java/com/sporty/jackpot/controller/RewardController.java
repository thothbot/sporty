package com.sporty.jackpot.controller;

import com.sporty.jackpot.dto.RewardRequest;
import com.sporty.jackpot.dto.RewardResponse;
import com.sporty.jackpot.entity.JackpotReward;
import com.sporty.jackpot.service.JackpotRewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Rewards", description = "Jackpot reward evaluation")
public class RewardController {

    private final JackpotRewardService rewardService;

    @Operation(summary = "Evaluate reward", description = "Evaluates if a bet wins a jackpot reward based on configured strategy")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reward evaluation completed"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Bet or Jackpot not found")
    })
    @PostMapping("/evaluate")
    public ResponseEntity<RewardResponse> evaluateReward(@Valid @RequestBody RewardRequest request) {
        log.info("Evaluating reward for bet {} on jackpot {}", request.getBetId(), request.getJackpotId());

        Optional<JackpotReward> reward = rewardService.evaluateReward(
                request.getBetId(),
                request.getUserId(),
                request.getJackpotId()
        );

        RewardResponse response;
        if (reward.isPresent()) {
            response = RewardResponse.builder()
                    .betId(request.getBetId())
                    .userId(request.getUserId())
                    .jackpotId(request.getJackpotId())
                    .won(true)
                    .rewardAmount(reward.get().getRewardAmount())
                    .message("Congratulations! You won the jackpot!")
                    .build();
        } else {
            response = RewardResponse.builder()
                    .betId(request.getBetId())
                    .userId(request.getUserId())
                    .jackpotId(request.getJackpotId())
                    .won(false)
                    .rewardAmount(BigDecimal.ZERO)
                    .message("Better luck next time!")
                    .build();
        }

        return ResponseEntity.ok(response);
    }
}
