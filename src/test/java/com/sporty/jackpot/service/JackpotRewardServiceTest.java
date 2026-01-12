package com.sporty.jackpot.service;

import com.sporty.jackpot.entity.Jackpot;
import com.sporty.jackpot.entity.JackpotContribution;
import com.sporty.jackpot.entity.JackpotReward;
import com.sporty.jackpot.entity.RewardType;
import com.sporty.jackpot.exception.ContributionNotFoundException;
import com.sporty.jackpot.exception.JackpotNotFoundException;
import com.sporty.jackpot.repository.JackpotContributionRepository;
import com.sporty.jackpot.repository.JackpotRepository;
import com.sporty.jackpot.repository.JackpotRewardRepository;
import com.sporty.jackpot.strategy.reward.RewardStrategy;
import com.sporty.jackpot.strategy.reward.RewardStrategyFactory;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JackpotRewardServiceTest {

    @Mock
    private JackpotRepository jackpotRepository;

    @Mock
    private JackpotContributionRepository contributionRepository;

    @Mock
    private JackpotRewardRepository rewardRepository;

    @Mock
    private RewardStrategyFactory strategyFactory;

    @Mock
    private RewardStrategy rewardStrategy;

    @InjectMocks
    private JackpotRewardService service;

    private UUID betId;
    private UUID userId;
    private UUID jackpotId;
    private Jackpot jackpot;
    private JackpotContribution contribution;

    @BeforeEach
    void setUp() {
        betId = UUID.randomUUID();
        userId = UUID.randomUUID();
        jackpotId = UUID.randomUUID();

        jackpot = Jackpot.builder()
                .id(jackpotId)
                .name("Test Jackpot")
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.05))
                .initialPoolValue(BigDecimal.valueOf(1000))
                .currentPoolValue(BigDecimal.valueOf(5000))
                .build();

        contribution = JackpotContribution.builder()
                .id(UUID.randomUUID())
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .stakeAmount(BigDecimal.valueOf(100))
                .contributionAmount(BigDecimal.valueOf(5))
                .build();
    }

    @Test
    void evaluateReward_betWins_returnsReward() {
        when(rewardRepository.findByBetId(betId)).thenReturn(Optional.empty());
        when(contributionRepository.findByBetId(betId)).thenReturn(List.of(contribution));
        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(jackpot));
        when(strategyFactory.getStrategy(RewardType.FIXED)).thenReturn(rewardStrategy);
        when(rewardStrategy.evaluateWin(jackpot)).thenReturn(true);
        when(rewardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Optional<JackpotReward> result = service.evaluateReward(betId, userId, jackpotId);

        assertThat(result).isPresent();
        assertThat(result.get().getRewardAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(result.get().getBetId()).isEqualTo(betId);
    }

    @Test
    void evaluateReward_betWins_resetsPool() {
        when(rewardRepository.findByBetId(betId)).thenReturn(Optional.empty());
        when(contributionRepository.findByBetId(betId)).thenReturn(List.of(contribution));
        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(jackpot));
        when(strategyFactory.getStrategy(RewardType.FIXED)).thenReturn(rewardStrategy);
        when(rewardStrategy.evaluateWin(jackpot)).thenReturn(true);
        when(rewardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.evaluateReward(betId, userId, jackpotId);

        ArgumentCaptor<Jackpot> jackpotCaptor = ArgumentCaptor.forClass(Jackpot.class);
        verify(jackpotRepository).save(jackpotCaptor.capture());
        assertThat(jackpotCaptor.getValue().getCurrentPoolValue()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void evaluateReward_betLoses_returnsEmpty() {
        when(rewardRepository.findByBetId(betId)).thenReturn(Optional.empty());
        when(contributionRepository.findByBetId(betId)).thenReturn(List.of(contribution));
        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(jackpot));
        when(strategyFactory.getStrategy(RewardType.FIXED)).thenReturn(rewardStrategy);
        when(rewardStrategy.evaluateWin(jackpot)).thenReturn(false);

        Optional<JackpotReward> result = service.evaluateReward(betId, userId, jackpotId);

        assertThat(result).isEmpty();
        verify(rewardRepository, never()).save(any());
    }

    @Test
    void evaluateReward_alreadyEvaluated_returnsExisting() {
        JackpotReward existingReward = JackpotReward.builder()
                .betId(betId)
                .rewardAmount(BigDecimal.valueOf(5000))
                .build();
        when(rewardRepository.findByBetId(betId)).thenReturn(Optional.of(existingReward));

        Optional<JackpotReward> result = service.evaluateReward(betId, userId, jackpotId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(existingReward);
        verify(strategyFactory, never()).getStrategy(any());
    }

    @Test
    void evaluateReward_noContribution_throwsException() {
        when(rewardRepository.findByBetId(betId)).thenReturn(Optional.empty());
        when(contributionRepository.findByBetId(betId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.evaluateReward(betId, userId, jackpotId))
                .isInstanceOf(ContributionNotFoundException.class);
    }

    @Test
    void evaluateReward_jackpotNotFound_throwsException() {
        when(rewardRepository.findByBetId(betId)).thenReturn(Optional.empty());
        when(contributionRepository.findByBetId(betId)).thenReturn(List.of(contribution));
        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.evaluateReward(betId, userId, jackpotId))
                .isInstanceOf(JackpotNotFoundException.class);
    }

    // ========== Edge Case Tests ==========

    @Test
    void evaluateReward_zeroPoolValue_winsWithZeroReward() {
        Jackpot zeroPoolJackpot = Jackpot.builder()
                .id(jackpotId)
                .name("Zero Pool Jackpot")
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.05))
                .initialPoolValue(BigDecimal.ZERO)
                .currentPoolValue(BigDecimal.ZERO)
                .build();

        when(rewardRepository.findByBetId(betId)).thenReturn(Optional.empty());
        when(contributionRepository.findByBetId(betId)).thenReturn(List.of(contribution));
        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(zeroPoolJackpot));
        when(strategyFactory.getStrategy(RewardType.FIXED)).thenReturn(rewardStrategy);
        when(rewardStrategy.evaluateWin(zeroPoolJackpot)).thenReturn(true);
        when(rewardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Optional<JackpotReward> result = service.evaluateReward(betId, userId, jackpotId);

        assertThat(result).isPresent();
        assertThat(result.get().getRewardAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void evaluateReward_veryLargePoolValue_handlesCorrectly() {
        Jackpot largePoolJackpot = Jackpot.builder()
                .id(jackpotId)
                .name("High Roller Jackpot")
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .initialPoolValue(BigDecimal.valueOf(1000))
                .currentPoolValue(new BigDecimal("999999999999999.9999"))
                .build();

        when(rewardRepository.findByBetId(betId)).thenReturn(Optional.empty());
        when(contributionRepository.findByBetId(betId)).thenReturn(List.of(contribution));
        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(largePoolJackpot));
        when(strategyFactory.getStrategy(RewardType.FIXED)).thenReturn(rewardStrategy);
        when(rewardStrategy.evaluateWin(largePoolJackpot)).thenReturn(true);
        when(rewardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Optional<JackpotReward> result = service.evaluateReward(betId, userId, jackpotId);

        assertThat(result).isPresent();
        assertThat(result.get().getRewardAmount())
                .isEqualByComparingTo(new BigDecimal("999999999999999.9999"));
    }

    @Test
    void evaluateReward_multipleContributionsExist_stillEvaluates() {
        JackpotContribution contribution2 = JackpotContribution.builder()
                .id(UUID.randomUUID())
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .stakeAmount(BigDecimal.valueOf(200))
                .contributionAmount(BigDecimal.valueOf(10))
                .build();

        when(rewardRepository.findByBetId(betId)).thenReturn(Optional.empty());
        when(contributionRepository.findByBetId(betId)).thenReturn(List.of(contribution, contribution2));
        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(jackpot));
        when(strategyFactory.getStrategy(RewardType.FIXED)).thenReturn(rewardStrategy);
        when(rewardStrategy.evaluateWin(jackpot)).thenReturn(true);
        when(rewardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Optional<JackpotReward> result = service.evaluateReward(betId, userId, jackpotId);

        assertThat(result).isPresent();
    }

    @Test
    void evaluateReward_usesVariableStrategy_whenConfigured() {
        Jackpot variableJackpot = Jackpot.builder()
                .id(jackpotId)
                .name("Variable Jackpot")
                .rewardType(RewardType.VARIABLE)
                .rewardChancePercentage(BigDecimal.valueOf(0.05))
                .initialPoolValue(BigDecimal.valueOf(1000))
                .currentPoolValue(BigDecimal.valueOf(5000))
                .maxPoolLimit(BigDecimal.valueOf(10000))
                .build();

        when(rewardRepository.findByBetId(betId)).thenReturn(Optional.empty());
        when(contributionRepository.findByBetId(betId)).thenReturn(List.of(contribution));
        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(variableJackpot));
        when(strategyFactory.getStrategy(RewardType.VARIABLE)).thenReturn(rewardStrategy);
        when(rewardStrategy.evaluateWin(variableJackpot)).thenReturn(false);

        service.evaluateReward(betId, userId, jackpotId);

        verify(strategyFactory).getStrategy(RewardType.VARIABLE);
    }

    @Test
    void evaluateReward_preservesRewardMetadata() {
        when(rewardRepository.findByBetId(betId)).thenReturn(Optional.empty());
        when(contributionRepository.findByBetId(betId)).thenReturn(List.of(contribution));
        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(jackpot));
        when(strategyFactory.getStrategy(RewardType.FIXED)).thenReturn(rewardStrategy);
        when(rewardStrategy.evaluateWin(jackpot)).thenReturn(true);

        ArgumentCaptor<JackpotReward> captor = ArgumentCaptor.forClass(JackpotReward.class);
        when(rewardRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        service.evaluateReward(betId, userId, jackpotId);

        JackpotReward savedReward = captor.getValue();
        assertThat(savedReward.getBetId()).isEqualTo(betId);
        assertThat(savedReward.getUserId()).isEqualTo(userId);
        assertThat(savedReward.getJackpotId()).isEqualTo(jackpotId);
        assertThat(savedReward.getRewardAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

    @Test
    void evaluateReward_betLoses_doesNotModifyPool() {
        BigDecimal originalPool = jackpot.getCurrentPoolValue();

        when(rewardRepository.findByBetId(betId)).thenReturn(Optional.empty());
        when(contributionRepository.findByBetId(betId)).thenReturn(List.of(contribution));
        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(jackpot));
        when(strategyFactory.getStrategy(RewardType.FIXED)).thenReturn(rewardStrategy);
        when(rewardStrategy.evaluateWin(jackpot)).thenReturn(false);

        service.evaluateReward(betId, userId, jackpotId);

        verify(jackpotRepository, never()).save(any());
        assertThat(jackpot.getCurrentPoolValue()).isEqualByComparingTo(originalPool);
    }

    @Test
    void evaluateReward_alreadyEvaluated_doesNotReevaluate() {
        JackpotReward existingReward = JackpotReward.builder()
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .rewardAmount(BigDecimal.valueOf(5000))
                .build();

        when(rewardRepository.findByBetId(betId)).thenReturn(Optional.of(existingReward));

        Optional<JackpotReward> result = service.evaluateReward(betId, userId, jackpotId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(existingReward);
        verify(contributionRepository, never()).findByBetId(any());
        verify(jackpotRepository, never()).findByIdWithLock(any());
        verify(rewardStrategy, never()).evaluateWin(any());
    }

    @Test
    void evaluateReward_contributionNotFoundExceptionContainsBetId() {
        when(rewardRepository.findByBetId(betId)).thenReturn(Optional.empty());
        when(contributionRepository.findByBetId(betId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.evaluateReward(betId, userId, jackpotId))
                .isInstanceOf(ContributionNotFoundException.class)
                .hasMessageContaining(betId.toString());
    }

    @Test
    void evaluateReward_jackpotNotFoundExceptionContainsJackpotId() {
        when(rewardRepository.findByBetId(betId)).thenReturn(Optional.empty());
        when(contributionRepository.findByBetId(betId)).thenReturn(List.of(contribution));
        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.evaluateReward(betId, userId, jackpotId))
                .isInstanceOf(JackpotNotFoundException.class)
                .hasMessageContaining(jackpotId.toString());
    }

    @Test
    void evaluateReward_poolResetsToInitialValueOnWin() {
        Jackpot customJackpot = Jackpot.builder()
                .id(jackpotId)
                .name("Custom Initial Jackpot")
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.05))
                .initialPoolValue(BigDecimal.valueOf(2500))
                .currentPoolValue(BigDecimal.valueOf(10000))
                .build();

        when(rewardRepository.findByBetId(betId)).thenReturn(Optional.empty());
        when(contributionRepository.findByBetId(betId)).thenReturn(List.of(contribution));
        when(jackpotRepository.findByIdWithLock(jackpotId)).thenReturn(Optional.of(customJackpot));
        when(strategyFactory.getStrategy(RewardType.FIXED)).thenReturn(rewardStrategy);
        when(rewardStrategy.evaluateWin(customJackpot)).thenReturn(true);
        when(rewardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.evaluateReward(betId, userId, jackpotId);

        ArgumentCaptor<Jackpot> captor = ArgumentCaptor.forClass(Jackpot.class);
        verify(jackpotRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentPoolValue()).isEqualByComparingTo(BigDecimal.valueOf(2500));
    }
}
