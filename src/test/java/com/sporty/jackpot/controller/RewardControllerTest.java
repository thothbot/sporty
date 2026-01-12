package com.sporty.jackpot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sporty.jackpot.dto.RewardRequest;
import com.sporty.jackpot.entity.JackpotReward;
import com.sporty.jackpot.exception.ContributionNotFoundException;
import com.sporty.jackpot.exception.JackpotNotFoundException;
import com.sporty.jackpot.service.JackpotRewardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RewardController.class)
class RewardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JackpotRewardService rewardService;

    @Test
    void evaluateReward_betWins_returnsWinResponse() throws Exception {
        UUID betId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID jackpotId = UUID.randomUUID();

        RewardRequest request = RewardRequest.builder()
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .build();

        JackpotReward reward = JackpotReward.builder()
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .rewardAmount(BigDecimal.valueOf(5000))
                .build();

        when(rewardService.evaluateReward(betId, userId, jackpotId)).thenReturn(Optional.of(reward));

        mockMvc.perform(post("/api/v1/rewards/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.won").value(true))
                .andExpect(jsonPath("$.rewardAmount").value(5000))
                .andExpect(jsonPath("$.message").value("Congratulations! You won the jackpot!"));
    }

    @Test
    void evaluateReward_betLoses_returnsLoseResponse() throws Exception {
        UUID betId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID jackpotId = UUID.randomUUID();

        RewardRequest request = RewardRequest.builder()
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .build();

        when(rewardService.evaluateReward(betId, userId, jackpotId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/rewards/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.won").value(false))
                .andExpect(jsonPath("$.rewardAmount").value(0))
                .andExpect(jsonPath("$.message").value("Better luck next time!"));
    }

    @Test
    void evaluateReward_jackpotNotFound_returns404() throws Exception {
        UUID betId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID jackpotId = UUID.randomUUID();

        RewardRequest request = RewardRequest.builder()
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .build();

        when(rewardService.evaluateReward(any(), any(), any()))
                .thenThrow(new JackpotNotFoundException(jackpotId));

        mockMvc.perform(post("/api/v1/rewards/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void evaluateReward_contributionNotFound_returns404() throws Exception {
        UUID betId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID jackpotId = UUID.randomUUID();

        RewardRequest request = RewardRequest.builder()
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .build();

        when(rewardService.evaluateReward(any(), any(), any()))
                .thenThrow(new ContributionNotFoundException(betId));

        mockMvc.perform(post("/api/v1/rewards/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void evaluateReward_missingBetId_returns400() throws Exception {
        RewardRequest request = RewardRequest.builder()
                .userId(UUID.randomUUID())
                .jackpotId(UUID.randomUUID())
                .build();

        mockMvc.perform(post("/api/v1/rewards/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========== Edge Case Tests ==========

    @Test
    void evaluateReward_missingUserId_returns400() throws Exception {
        RewardRequest request = RewardRequest.builder()
                .betId(UUID.randomUUID())
                .jackpotId(UUID.randomUUID())
                .build();

        mockMvc.perform(post("/api/v1/rewards/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.userId").exists());
    }

    @Test
    void evaluateReward_missingJackpotId_returns400() throws Exception {
        RewardRequest request = RewardRequest.builder()
                .betId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .build();

        mockMvc.perform(post("/api/v1/rewards/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.jackpotId").exists());
    }

    @Test
    void evaluateReward_allFieldsNull_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/rewards/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evaluateReward_invalidUuidFormat_returns400() throws Exception {
        String invalidJson = """
                {
                    "betId": "not-a-uuid",
                    "userId": "also-invalid",
                    "jackpotId": "still-invalid"
                }
                """;

        mockMvc.perform(post("/api/v1/rewards/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evaluateReward_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/rewards/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evaluateReward_zeroRewardAmount_returnsWinWithZero() throws Exception {
        UUID betId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID jackpotId = UUID.randomUUID();

        RewardRequest request = RewardRequest.builder()
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .build();

        JackpotReward reward = JackpotReward.builder()
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .rewardAmount(BigDecimal.ZERO)
                .build();

        when(rewardService.evaluateReward(betId, userId, jackpotId)).thenReturn(Optional.of(reward));

        mockMvc.perform(post("/api/v1/rewards/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.won").value(true))
                .andExpect(jsonPath("$.rewardAmount").value(0));
    }

    @Test
    void evaluateReward_veryLargeRewardAmount_returnsCorrectAmount() throws Exception {
        UUID betId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID jackpotId = UUID.randomUUID();

        RewardRequest request = RewardRequest.builder()
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .build();

        BigDecimal largeAmount = new BigDecimal("999999999999999.9999");
        JackpotReward reward = JackpotReward.builder()
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .rewardAmount(largeAmount)
                .build();

        when(rewardService.evaluateReward(betId, userId, jackpotId)).thenReturn(Optional.of(reward));

        mockMvc.perform(post("/api/v1/rewards/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.won").value(true))
                .andExpect(jsonPath("$.rewardAmount").exists());
    }

    @Test
    void evaluateReward_idempotentCall_returnsSameResult() throws Exception {
        UUID betId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID jackpotId = UUID.randomUUID();

        RewardRequest request = RewardRequest.builder()
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .build();

        JackpotReward reward = JackpotReward.builder()
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .rewardAmount(BigDecimal.valueOf(5000))
                .build();

        when(rewardService.evaluateReward(betId, userId, jackpotId)).thenReturn(Optional.of(reward));

        // First call
        mockMvc.perform(post("/api/v1/rewards/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.won").value(true))
                .andExpect(jsonPath("$.rewardAmount").value(5000));

        // Second call (should return same result - idempotent)
        mockMvc.perform(post("/api/v1/rewards/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.won").value(true))
                .andExpect(jsonPath("$.rewardAmount").value(5000));
    }

    @Test
    void evaluateReward_responseContainsAllFields() throws Exception {
        UUID betId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID jackpotId = UUID.randomUUID();

        RewardRequest request = RewardRequest.builder()
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .build();

        JackpotReward reward = JackpotReward.builder()
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .rewardAmount(BigDecimal.valueOf(5000))
                .build();

        when(rewardService.evaluateReward(betId, userId, jackpotId)).thenReturn(Optional.of(reward));

        mockMvc.perform(post("/api/v1/rewards/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.betId").value(betId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.jackpotId").value(jackpotId.toString()))
                .andExpect(jsonPath("$.won").value(true))
                .andExpect(jsonPath("$.rewardAmount").value(5000))
                .andExpect(jsonPath("$.message").exists());
    }
}
