package com.sporty.jackpot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sporty.jackpot.dto.BetMessage;
import com.sporty.jackpot.dto.BetRequest;
import com.sporty.jackpot.entity.Bet;
import com.sporty.jackpot.entity.BetStatus;
import com.sporty.jackpot.exception.BetNotFoundException;
import com.sporty.jackpot.kafka.KafkaBetProducer;
import com.sporty.jackpot.service.BetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BetController.class)
class BetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KafkaBetProducer kafkaBetProducer;

    @MockBean
    private BetService betService;

    @Test
    void publishBet_validRequest_returns201() throws Exception {
        BetRequest request = BetRequest.builder()
                .userId(UUID.randomUUID())
                .jackpotId(UUID.randomUUID())
                .betAmount(BigDecimal.valueOf(100))
                .build();

        Bet bet = Bet.builder()
                .id(UUID.randomUUID())
                .userId(request.getUserId())
                .jackpotId(request.getJackpotId())
                .betAmount(request.getBetAmount())
                .status(BetStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        BetMessage message = BetMessage.builder()
                .betId(bet.getId())
                .userId(bet.getUserId())
                .jackpotId(bet.getJackpotId())
                .betAmount(bet.getBetAmount())
                .build();

        when(betService.createBet(any(UUID.class), any(BetRequest.class))).thenReturn(bet);
        when(betService.toMessage(bet)).thenReturn(message);
        doNothing().when(kafkaBetProducer).publishBetSync(any());
        doNothing().when(betService).updateStatus(any(UUID.class), any(BetStatus.class));

        mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.betId").exists())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(betService).createBet(any(UUID.class), any(BetRequest.class));
        verify(kafkaBetProducer).publishBetSync(any());
        verify(betService).updateStatus(any(UUID.class), eq(BetStatus.PUBLISHED));
    }

    @Test
    void publishBet_missingUserId_returns400() throws Exception {
        BetRequest request = BetRequest.builder()
                .jackpotId(UUID.randomUUID())
                .betAmount(BigDecimal.valueOf(100))
                .build();

        mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.userId").exists());
    }

    @Test
    void publishBet_missingJackpotId_returns400() throws Exception {
        BetRequest request = BetRequest.builder()
                .userId(UUID.randomUUID())
                .betAmount(BigDecimal.valueOf(100))
                .build();

        mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.jackpotId").exists());
    }

    @Test
    void publishBet_negativeBetAmount_returns400() throws Exception {
        BetRequest request = BetRequest.builder()
                .userId(UUID.randomUUID())
                .jackpotId(UUID.randomUUID())
                .betAmount(BigDecimal.valueOf(-100))
                .build();

        mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.betAmount").exists());
    }

    @Test
    void publishBet_zeroBetAmount_returns400() throws Exception {
        BetRequest request = BetRequest.builder()
                .userId(UUID.randomUUID())
                .jackpotId(UUID.randomUUID())
                .betAmount(BigDecimal.ZERO)
                .build();

        mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.betAmount").exists());
    }

    @Test
    void getBet_existingBet_returnsBetStatus() throws Exception {
        UUID betId = UUID.randomUUID();
        Bet bet = Bet.builder()
                .id(betId)
                .userId(UUID.randomUUID())
                .jackpotId(UUID.randomUUID())
                .betAmount(BigDecimal.valueOf(100))
                .status(BetStatus.PROCESSED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(betService.getBet(betId)).thenReturn(bet);

        mockMvc.perform(get("/api/v1/bets/{id}", betId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.betId").value(betId.toString()))
                .andExpect(jsonPath("$.status").value("PROCESSED"));
    }

    @Test
    void getBet_nonExistingBet_returns404() throws Exception {
        UUID betId = UUID.randomUUID();

        when(betService.getBet(betId)).thenThrow(new BetNotFoundException(betId));

        mockMvc.perform(get("/api/v1/bets/{id}", betId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // ========== Edge Case Tests ==========

    @Test
    void publishBet_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publishBet_emptyRequestBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publishBet_invalidUuidFormat_returns400() throws Exception {
        String invalidJson = "{\"userId\": \"not-a-uuid\", \"jackpotId\": \"also-not-uuid\", \"betAmount\": 100}";

        mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publishBet_veryLargeBetAmount_returns201() throws Exception {
        BetRequest request = BetRequest.builder()
                .userId(UUID.randomUUID())
                .jackpotId(UUID.randomUUID())
                .betAmount(new BigDecimal("99999999999999.9999"))
                .build();

        Bet bet = Bet.builder()
                .id(UUID.randomUUID())
                .userId(request.getUserId())
                .jackpotId(request.getJackpotId())
                .betAmount(request.getBetAmount())
                .status(BetStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        BetMessage message = BetMessage.builder()
                .betId(bet.getId())
                .userId(bet.getUserId())
                .jackpotId(bet.getJackpotId())
                .betAmount(bet.getBetAmount())
                .build();

        when(betService.createBet(any(UUID.class), any(BetRequest.class))).thenReturn(bet);
        when(betService.toMessage(bet)).thenReturn(message);
        doNothing().when(kafkaBetProducer).publishBetSync(any());
        doNothing().when(betService).updateStatus(any(UUID.class), any(BetStatus.class));

        mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void publishBet_verySmallBetAmount_returns201() throws Exception {
        BetRequest request = BetRequest.builder()
                .userId(UUID.randomUUID())
                .jackpotId(UUID.randomUUID())
                .betAmount(new BigDecimal("0.0001"))
                .build();

        Bet bet = Bet.builder()
                .id(UUID.randomUUID())
                .userId(request.getUserId())
                .jackpotId(request.getJackpotId())
                .betAmount(request.getBetAmount())
                .status(BetStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        BetMessage message = BetMessage.builder()
                .betId(bet.getId())
                .userId(bet.getUserId())
                .jackpotId(bet.getJackpotId())
                .betAmount(bet.getBetAmount())
                .build();

        when(betService.createBet(any(UUID.class), any(BetRequest.class))).thenReturn(bet);
        when(betService.toMessage(bet)).thenReturn(message);
        doNothing().when(kafkaBetProducer).publishBetSync(any());
        doNothing().when(betService).updateStatus(any(UUID.class), any(BetStatus.class));

        mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void publishBet_nullBetAmount_returns400() throws Exception {
        String jsonWithNullAmount = String.format(
                "{\"userId\": \"%s\", \"jackpotId\": \"%s\", \"betAmount\": null}",
                UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithNullAmount))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publishBet_kafkaPublishFailure_returns500() throws Exception {
        BetRequest request = BetRequest.builder()
                .userId(UUID.randomUUID())
                .jackpotId(UUID.randomUUID())
                .betAmount(BigDecimal.valueOf(100))
                .build();

        Bet bet = Bet.builder()
                .id(UUID.randomUUID())
                .userId(request.getUserId())
                .jackpotId(request.getJackpotId())
                .betAmount(request.getBetAmount())
                .status(BetStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        BetMessage message = BetMessage.builder()
                .betId(bet.getId())
                .userId(bet.getUserId())
                .jackpotId(bet.getJackpotId())
                .betAmount(bet.getBetAmount())
                .build();

        when(betService.createBet(any(UUID.class), any(BetRequest.class))).thenReturn(bet);
        when(betService.toMessage(bet)).thenReturn(message);
        doThrow(new RuntimeException("Kafka connection failed"))
                .when(kafkaBetProducer).publishBetSync(any());

        mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void publishBet_databaseSaveFailure_returns500() throws Exception {
        BetRequest request = BetRequest.builder()
                .userId(UUID.randomUUID())
                .jackpotId(UUID.randomUUID())
                .betAmount(BigDecimal.valueOf(100))
                .build();

        when(betService.createBet(any(UUID.class), any(BetRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        verify(kafkaBetProducer, never()).publishBetSync(any());
    }

    @Test
    void getBet_invalidUuidFormat_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/bets/{id}", "not-a-valid-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publishBet_missingContentType_returns415() throws Exception {
        BetRequest request = BetRequest.builder()
                .userId(UUID.randomUUID())
                .jackpotId(UUID.randomUUID())
                .betAmount(BigDecimal.valueOf(100))
                .build();

        mockMvc.perform(post("/api/v1/bets")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void publishBet_allFieldsMissing_returns400WithMultipleErrors() throws Exception {
        mockMvc.perform(post("/api/v1/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }
}
