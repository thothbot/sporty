package com.sporty.jackpot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sporty.jackpot.dto.JackpotDto;
import com.sporty.jackpot.dto.JackpotRequest;
import com.sporty.jackpot.entity.ContributionType;
import com.sporty.jackpot.entity.RewardType;
import com.sporty.jackpot.exception.JackpotNotFoundException;
import com.sporty.jackpot.service.JackpotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JackpotController.class)
class JackpotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JackpotService jackpotService;

    @Test
    void createJackpot_validRequest_returns201() throws Exception {
        JackpotRequest request = JackpotRequest.builder()
                .name("Test Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .maxPoolLimit(BigDecimal.valueOf(100000))
                .build();

        UUID jackpotId = UUID.randomUUID();
        JackpotDto response = JackpotDto.builder()
                .id(jackpotId)
                .name("Test Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .currentPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .maxPoolLimit(BigDecimal.valueOf(100000))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(jackpotService.createJackpot(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(jackpotId.toString()))
                .andExpect(jsonPath("$.name").value("Test Jackpot"))
                .andExpect(jsonPath("$.initialPoolValue").value(10000))
                .andExpect(jsonPath("$.currentPoolValue").value(10000));
    }

    @Test
    void createJackpot_missingName_returns400() throws Exception {
        JackpotRequest request = JackpotRequest.builder()
                .initialPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .build();

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void createJackpot_negativeContributionPercentage_returns400() throws Exception {
        JackpotRequest request = JackpotRequest.builder()
                .name("Test Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(-0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .build();

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.contributionPercentage").exists());
    }

    @Test
    void getJackpot_existingId_returnsJackpot() throws Exception {
        UUID jackpotId = UUID.randomUUID();
        JackpotDto jackpot = JackpotDto.builder()
                .id(jackpotId)
                .name("Test Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .currentPoolValue(BigDecimal.valueOf(15000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(jackpotService.getJackpot(jackpotId)).thenReturn(jackpot);

        mockMvc.perform(get("/api/v1/jackpots/{id}", jackpotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jackpotId.toString()))
                .andExpect(jsonPath("$.name").value("Test Jackpot"))
                .andExpect(jsonPath("$.currentPoolValue").value(15000));
    }

    @Test
    void getJackpot_nonExistingId_returns404() throws Exception {
        UUID jackpotId = UUID.randomUUID();

        when(jackpotService.getJackpot(jackpotId))
                .thenThrow(new JackpotNotFoundException(jackpotId));

        mockMvc.perform(get("/api/v1/jackpots/{id}", jackpotId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getAllJackpots_returnsListOfJackpots() throws Exception {
        JackpotDto jackpot1 = JackpotDto.builder()
                .id(UUID.randomUUID())
                .name("Jackpot 1")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .currentPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        JackpotDto jackpot2 = JackpotDto.builder()
                .id(UUID.randomUUID())
                .name("Jackpot 2")
                .initialPoolValue(BigDecimal.valueOf(50000))
                .currentPoolValue(BigDecimal.valueOf(75000))
                .contributionType(ContributionType.VARIABLE)
                .contributionPercentage(BigDecimal.valueOf(0.10))
                .rewardType(RewardType.VARIABLE)
                .rewardChancePercentage(BigDecimal.valueOf(0.05))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(jackpotService.getAllJackpots()).thenReturn(List.of(jackpot1, jackpot2));

        mockMvc.perform(get("/api/v1/jackpots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Jackpot 1"))
                .andExpect(jsonPath("$[1].name").value("Jackpot 2"));
    }

    // ========== Edge Case Tests ==========

    @Test
    void createJackpot_emptyName_returns400() throws Exception {
        JackpotRequest request = JackpotRequest.builder()
                .name("")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .build();

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void createJackpot_whitespaceOnlyName_returns400() throws Exception {
        JackpotRequest request = JackpotRequest.builder()
                .name("   ")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .build();

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void createJackpot_veryLongName_returns201() throws Exception {
        String longName = "A".repeat(255);
        JackpotRequest request = JackpotRequest.builder()
                .name(longName)
                .initialPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .build();

        UUID jackpotId = UUID.randomUUID();
        JackpotDto response = JackpotDto.builder()
                .id(jackpotId)
                .name(longName)
                .initialPoolValue(BigDecimal.valueOf(10000))
                .currentPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(jackpotService.createJackpot(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createJackpot_zeroContributionPercentage_returns400() throws Exception {
        JackpotRequest request = JackpotRequest.builder()
                .name("Test Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.ZERO)
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .build();

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.contributionPercentage").exists());
    }

    @Test
    void createJackpot_negativeInitialPoolValue_returns400() throws Exception {
        JackpotRequest request = JackpotRequest.builder()
                .name("Test Jackpot")
                .initialPoolValue(BigDecimal.valueOf(-1000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .build();

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.initialPoolValue").exists());
    }

    @Test
    void createJackpot_zeroInitialPoolValue_returns201() throws Exception {
        JackpotRequest request = JackpotRequest.builder()
                .name("Test Jackpot")
                .initialPoolValue(BigDecimal.ZERO)
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .build();

        UUID jackpotId = UUID.randomUUID();
        JackpotDto response = JackpotDto.builder()
                .id(jackpotId)
                .name("Test Jackpot")
                .initialPoolValue(BigDecimal.ZERO)
                .currentPoolValue(BigDecimal.ZERO)
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(jackpotService.createJackpot(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createJackpot_negativeRewardChance_returns400() throws Exception {
        JackpotRequest request = JackpotRequest.builder()
                .name("Test Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(-0.01))
                .build();

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rewardChancePercentage").exists());
    }

    @Test
    void createJackpot_zeroRewardChance_returns201() throws Exception {
        JackpotRequest request = JackpotRequest.builder()
                .name("Test Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.ZERO)
                .build();

        UUID jackpotId = UUID.randomUUID();
        JackpotDto response = JackpotDto.builder()
                .id(jackpotId)
                .name("Test Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .currentPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(jackpotService.createJackpot(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createJackpot_negativeMaxPoolLimit_returns400() throws Exception {
        JackpotRequest request = JackpotRequest.builder()
                .name("Test Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .maxPoolLimit(BigDecimal.valueOf(-50000))
                .build();

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.maxPoolLimit").exists());
    }

    @Test
    void createJackpot_zeroMaxPoolLimit_returns400() throws Exception {
        JackpotRequest request = JackpotRequest.builder()
                .name("Test Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .maxPoolLimit(BigDecimal.ZERO)
                .build();

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.maxPoolLimit").exists());
    }

    @Test
    void createJackpot_nullMaxPoolLimit_returns201() throws Exception {
        JackpotRequest request = JackpotRequest.builder()
                .name("Test Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .maxPoolLimit(null)
                .build();

        UUID jackpotId = UUID.randomUUID();
        JackpotDto response = JackpotDto.builder()
                .id(jackpotId)
                .name("Test Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .currentPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(jackpotService.createJackpot(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createJackpot_invalidContributionType_returns400() throws Exception {
        String jsonWithInvalidEnum = """
                {
                    "name": "Test Jackpot",
                    "initialPoolValue": 10000,
                    "contributionType": "INVALID_TYPE",
                    "contributionPercentage": 0.05,
                    "rewardType": "FIXED",
                    "rewardChancePercentage": 0.01
                }
                """;

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithInvalidEnum))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createJackpot_invalidRewardType_returns400() throws Exception {
        String jsonWithInvalidEnum = """
                {
                    "name": "Test Jackpot",
                    "initialPoolValue": 10000,
                    "contributionType": "FIXED",
                    "contributionPercentage": 0.05,
                    "rewardType": "INVALID_REWARD",
                    "rewardChancePercentage": 0.01
                }
                """;

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithInvalidEnum))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createJackpot_missingContributionType_returns400() throws Exception {
        String jsonMissingEnum = """
                {
                    "name": "Test Jackpot",
                    "initialPoolValue": 10000,
                    "contributionPercentage": 0.05,
                    "rewardType": "FIXED",
                    "rewardChancePercentage": 0.01
                }
                """;

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMissingEnum))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.contributionType").exists());
    }

    @Test
    void createJackpot_missingRewardType_returns400() throws Exception {
        String jsonMissingEnum = """
                {
                    "name": "Test Jackpot",
                    "initialPoolValue": 10000,
                    "contributionType": "FIXED",
                    "contributionPercentage": 0.05,
                    "rewardChancePercentage": 0.01
                }
                """;

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMissingEnum))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rewardType").exists());
    }

    @Test
    void createJackpot_veryLargePoolValues_returns201() throws Exception {
        JackpotRequest request = JackpotRequest.builder()
                .name("High Roller Jackpot")
                .initialPoolValue(new BigDecimal("999999999999999.9999"))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .maxPoolLimit(new BigDecimal("9999999999999999.9999"))
                .build();

        UUID jackpotId = UUID.randomUUID();
        JackpotDto response = JackpotDto.builder()
                .id(jackpotId)
                .name("High Roller Jackpot")
                .initialPoolValue(request.getInitialPoolValue())
                .currentPoolValue(request.getInitialPoolValue())
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .maxPoolLimit(request.getMaxPoolLimit())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(jackpotService.createJackpot(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createJackpot_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createJackpot_emptyRequestBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getJackpot_invalidUuidFormat_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/jackpots/{id}", "not-a-valid-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllJackpots_emptyList_returns200WithEmptyArray() throws Exception {
        when(jackpotService.getAllJackpots()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/jackpots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void createJackpot_withVariableStrategies_returns201() throws Exception {
        JackpotRequest request = JackpotRequest.builder()
                .name("Variable Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.VARIABLE)
                .contributionPercentage(BigDecimal.valueOf(0.10))
                .rewardType(RewardType.VARIABLE)
                .rewardChancePercentage(BigDecimal.valueOf(0.05))
                .maxPoolLimit(BigDecimal.valueOf(100000))
                .build();

        UUID jackpotId = UUID.randomUUID();
        JackpotDto response = JackpotDto.builder()
                .id(jackpotId)
                .name("Variable Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .currentPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.VARIABLE)
                .contributionPercentage(BigDecimal.valueOf(0.10))
                .rewardType(RewardType.VARIABLE)
                .rewardChancePercentage(BigDecimal.valueOf(0.05))
                .maxPoolLimit(BigDecimal.valueOf(100000))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(jackpotService.createJackpot(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/jackpots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contributionType").value("VARIABLE"))
                .andExpect(jsonPath("$.rewardType").value("VARIABLE"));
    }
}
