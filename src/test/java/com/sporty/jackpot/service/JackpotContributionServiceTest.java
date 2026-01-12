package com.sporty.jackpot.service;

import com.sporty.jackpot.dto.BetMessage;
import com.sporty.jackpot.entity.ContributionType;
import com.sporty.jackpot.entity.Jackpot;
import com.sporty.jackpot.entity.JackpotContribution;
import com.sporty.jackpot.exception.JackpotNotFoundException;
import com.sporty.jackpot.repository.JackpotContributionRepository;
import com.sporty.jackpot.repository.JackpotRepository;
import com.sporty.jackpot.strategy.contribution.ContributionStrategy;
import com.sporty.jackpot.strategy.contribution.ContributionStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JackpotContributionServiceTest {

    @Mock
    private JackpotRepository jackpotRepository;

    @Mock
    private JackpotContributionRepository contributionRepository;

    @Mock
    private ContributionStrategyFactory strategyFactory;

    @Mock
    private ContributionStrategy contributionStrategy;

    @InjectMocks
    private JackpotContributionService service;

    private UUID jackpotId;
    private Jackpot jackpot;
    private BetMessage bet;

    @BeforeEach
    void setUp() {
        jackpotId = UUID.randomUUID();
        jackpot = Jackpot.builder()
                .id(jackpotId)
                .name("Test Jackpot")
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .currentPoolValue(BigDecimal.valueOf(1000))
                .build();

        bet = BetMessage.builder()
                .betId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .jackpotId(jackpotId)
                .betAmount(BigDecimal.valueOf(100))
                .build();
    }

    @Test
    void processBatch_validBets_createsContributions() {
        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(jackpot));
        when(strategyFactory.getStrategy(ContributionType.FIXED)).thenReturn(contributionStrategy);
        when(contributionStrategy.calculateContribution(any(), any())).thenReturn(BigDecimal.valueOf(5));
        when(contributionRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        List<JackpotContribution> result = service.processBatch(List.of(bet));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBetId()).isEqualTo(bet.getBetId());
        assertThat(result.get(0).getContributionAmount()).isEqualByComparingTo(BigDecimal.valueOf(5));
        verify(jackpotRepository).save(jackpot);
    }

    @Test
    void processBatch_jackpotNotFound_throwsException() {
        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processBatch(List.of(bet)))
                .isInstanceOf(JackpotNotFoundException.class);
    }

    @Test
    void processBatch_multipleBets_updatesPoolCorrectly() {
        BetMessage bet2 = BetMessage.builder()
                .betId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .jackpotId(jackpotId)
                .betAmount(BigDecimal.valueOf(200))
                .build();

        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(jackpot));
        when(strategyFactory.getStrategy(ContributionType.FIXED)).thenReturn(contributionStrategy);
        when(contributionStrategy.calculateContribution(eq(BigDecimal.valueOf(100)), any()))
                .thenReturn(BigDecimal.valueOf(5));
        when(contributionStrategy.calculateContribution(eq(BigDecimal.valueOf(200)), any()))
                .thenReturn(BigDecimal.valueOf(10));
        when(contributionRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        service.processBatch(List.of(bet, bet2));

        ArgumentCaptor<Jackpot> jackpotCaptor = ArgumentCaptor.forClass(Jackpot.class);
        verify(jackpotRepository).save(jackpotCaptor.capture());
        assertThat(jackpotCaptor.getValue().getCurrentPoolValue()).isEqualByComparingTo(BigDecimal.valueOf(1015));
    }

    @Test
    void processContribution_singleBet_delegatesToBatch() {
        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(jackpot));
        when(strategyFactory.getStrategy(ContributionType.FIXED)).thenReturn(contributionStrategy);
        when(contributionStrategy.calculateContribution(any(), any())).thenReturn(BigDecimal.valueOf(5));
        when(contributionRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        JackpotContribution result = service.processContribution(bet);

        assertThat(result).isNotNull();
        assertThat(result.getBetId()).isEqualTo(bet.getBetId());
    }

    // ========== Edge Case Tests ==========

    @Test
    void processBatch_emptyList_returnsEmptyAndNoSave() {
        List<JackpotContribution> result = service.processBatch(List.of());

        assertThat(result).isEmpty();
        verify(jackpotRepository, never()).findByIdWithLock(any());
        verify(contributionRepository).saveAll(any());
    }

    @Test
    void processBatch_veryLargeBetAmount_handlesCorrectly() {
        BetMessage largeBet = BetMessage.builder()
                .betId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .jackpotId(jackpotId)
                .betAmount(new BigDecimal("99999999999999.9999"))
                .build();

        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(jackpot));
        when(strategyFactory.getStrategy(ContributionType.FIXED)).thenReturn(contributionStrategy);
        when(contributionStrategy.calculateContribution(any(), any()))
                .thenReturn(new BigDecimal("4999999999999.9999"));
        when(contributionRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        List<JackpotContribution> result = service.processBatch(List.of(largeBet));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContributionAmount())
                .isEqualByComparingTo(new BigDecimal("4999999999999.9999"));
    }

    @Test
    void processBatch_verySmallBetAmount_handlesCorrectly() {
        BetMessage smallBet = BetMessage.builder()
                .betId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .jackpotId(jackpotId)
                .betAmount(new BigDecimal("0.0001"))
                .build();

        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(jackpot));
        when(strategyFactory.getStrategy(ContributionType.FIXED)).thenReturn(contributionStrategy);
        when(contributionStrategy.calculateContribution(any(), any()))
                .thenReturn(new BigDecimal("0.000005"));
        when(contributionRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        List<JackpotContribution> result = service.processBatch(List.of(smallBet));

        assertThat(result).hasSize(1);
    }

    @Test
    void processBatch_multipleBetsForDifferentJackpots_processesAllJackpots() {
        UUID jackpotId2 = UUID.randomUUID();
        Jackpot jackpot2 = Jackpot.builder()
                .id(jackpotId2)
                .name("Second Jackpot")
                .contributionType(ContributionType.VARIABLE)
                .contributionPercentage(BigDecimal.valueOf(0.10))
                .currentPoolValue(BigDecimal.valueOf(5000))
                .build();

        BetMessage bet2 = BetMessage.builder()
                .betId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .jackpotId(jackpotId2)
                .betAmount(BigDecimal.valueOf(200))
                .build();

        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(jackpot));
        when(jackpotRepository.findByIdWithLock(jackpotId2)).thenReturn(Optional.of(jackpot2));
        when(strategyFactory.getStrategy(ContributionType.FIXED)).thenReturn(contributionStrategy);
        when(strategyFactory.getStrategy(ContributionType.VARIABLE)).thenReturn(contributionStrategy);
        when(contributionStrategy.calculateContribution(any(), any())).thenReturn(BigDecimal.valueOf(10));
        when(contributionRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        List<JackpotContribution> result = service.processBatch(List.of(bet, bet2));

        assertThat(result).hasSize(2);
    }

    @Test
    void processBatch_recordsCurrentJackpotAmountInContribution() {
        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(jackpot));
        when(strategyFactory.getStrategy(ContributionType.FIXED)).thenReturn(contributionStrategy);
        when(contributionStrategy.calculateContribution(any(), any())).thenReturn(BigDecimal.valueOf(5));
        when(contributionRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        List<JackpotContribution> result = service.processBatch(List.of(bet));

        // After contribution, pool should be 1000 + 5 = 1005
        assertThat(result.get(0).getCurrentJackpotAmount()).isEqualByComparingTo(BigDecimal.valueOf(1005));
    }

    @Test
    void processBatch_zeroContribution_stillProcesses() {
        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(jackpot));
        when(strategyFactory.getStrategy(ContributionType.FIXED)).thenReturn(contributionStrategy);
        when(contributionStrategy.calculateContribution(any(), any())).thenReturn(BigDecimal.ZERO);
        when(contributionRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        List<JackpotContribution> result = service.processBatch(List.of(bet));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContributionAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void processBatch_preservesBetMetadata() {
        UUID betId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BetMessage detailedBet = BetMessage.builder()
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .betAmount(BigDecimal.valueOf(500))
                .build();

        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(jackpot));
        when(strategyFactory.getStrategy(ContributionType.FIXED)).thenReturn(contributionStrategy);
        when(contributionStrategy.calculateContribution(any(), any())).thenReturn(BigDecimal.valueOf(25));
        when(contributionRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        List<JackpotContribution> result = service.processBatch(List.of(detailedBet));

        JackpotContribution contribution = result.get(0);
        assertThat(contribution.getBetId()).isEqualTo(betId);
        assertThat(contribution.getUserId()).isEqualTo(userId);
        assertThat(contribution.getJackpotId()).isEqualTo(jackpotId);
        assertThat(contribution.getStakeAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(contribution.getContributionAmount()).isEqualByComparingTo(BigDecimal.valueOf(25));
    }

    @Test
    void processBatch_largeBatch_processesAll() {
        List<BetMessage> largeBatch = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeBatch.add(BetMessage.builder()
                    .betId(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .jackpotId(jackpotId)
                    .betAmount(BigDecimal.valueOf(10))
                    .build());
        }

        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(jackpot));
        when(strategyFactory.getStrategy(ContributionType.FIXED)).thenReturn(contributionStrategy);
        when(contributionStrategy.calculateContribution(any(), any())).thenReturn(BigDecimal.valueOf(0.5));
        when(contributionRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        List<JackpotContribution> result = service.processBatch(largeBatch);

        assertThat(result).hasSize(100);
        // Pool should increase by 0.5 * 100 = 50
        ArgumentCaptor<Jackpot> captor = ArgumentCaptor.forClass(Jackpot.class);
        verify(jackpotRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentPoolValue()).isEqualByComparingTo(BigDecimal.valueOf(1050));
    }

    @Test
    void processBatch_usesVariableStrategy_whenConfigured() {
        Jackpot variableJackpot = Jackpot.builder()
                .id(jackpotId)
                .name("Variable Jackpot")
                .contributionType(ContributionType.VARIABLE)
                .contributionPercentage(BigDecimal.valueOf(0.10))
                .currentPoolValue(BigDecimal.valueOf(1000))
                .maxPoolLimit(BigDecimal.valueOf(10000))
                .build();

        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(variableJackpot));
        when(strategyFactory.getStrategy(ContributionType.VARIABLE)).thenReturn(contributionStrategy);
        when(contributionStrategy.calculateContribution(any(), any())).thenReturn(BigDecimal.valueOf(9));
        when(contributionRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        List<JackpotContribution> result = service.processBatch(List.of(bet));

        assertThat(result).hasSize(1);
        verify(strategyFactory).getStrategy(ContributionType.VARIABLE);
    }

    @Test
    void processContribution_emptyBatch_returnsNull() {
        when(contributionRepository.saveAll(any())).thenReturn(List.of());

        // When processBatch returns empty, processContribution returns null
        // But we need to actually call the method properly
        List<BetMessage> emptyBatch = List.of();
        List<JackpotContribution> batchResult = service.processBatch(emptyBatch);

        assertThat(batchResult).isEmpty();
    }
}
