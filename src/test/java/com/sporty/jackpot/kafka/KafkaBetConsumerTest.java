package com.sporty.jackpot.kafka;

import com.sporty.jackpot.dto.BetMessage;
import com.sporty.jackpot.entity.Bet;
import com.sporty.jackpot.entity.BetStatus;
import com.sporty.jackpot.entity.JackpotContribution;
import com.sporty.jackpot.exception.JackpotNotFoundException;
import com.sporty.jackpot.repository.BetRepository;
import com.sporty.jackpot.service.JackpotContributionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaBetConsumerTest {

    @Mock
    private JackpotContributionService contributionService;

    @Mock
    private BetRepository betRepository;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private KafkaBetConsumer consumer;

    private BetMessage bet;
    private Bet betEntity;
    private UUID betId;
    private UUID userId;
    private UUID jackpotId;

    @BeforeEach
    void setUp() {
        betId = UUID.randomUUID();
        userId = UUID.randomUUID();
        jackpotId = UUID.randomUUID();

        bet = BetMessage.builder()
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .betAmount(BigDecimal.valueOf(100))
                .build();

        betEntity = Bet.builder()
                .id(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .betAmount(BigDecimal.valueOf(100))
                .status(BetStatus.PUBLISHED)
                .build();
    }

    @Test
    void consumeBets_validBatch_processesAndAcknowledges() {
        when(contributionService.processBatch(any())).thenReturn(List.of());
        when(betRepository.findById(betId)).thenReturn(Optional.of(betEntity));
        when(betRepository.save(any())).thenReturn(betEntity);

        consumer.consumeBets(List.of(bet), acknowledgment);

        verify(contributionService).processBatch(List.of(bet));
        verify(acknowledgment).acknowledge();
        verify(betRepository).findById(betId);
    }

    @Test
    void consumeBets_emptyBatch_stillAcknowledges() {
        when(contributionService.processBatch(any())).thenReturn(List.of());

        consumer.consumeBets(List.of(), acknowledgment);

        verify(contributionService).processBatch(List.of());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeBets_updatesBetStatusToProcessed() {
        when(contributionService.processBatch(any())).thenReturn(List.of());
        when(betRepository.findById(betId)).thenReturn(Optional.of(betEntity));
        when(betRepository.save(any())).thenReturn(betEntity);

        consumer.consumeBets(List.of(bet), acknowledgment);

        ArgumentCaptor<Bet> captor = ArgumentCaptor.forClass(Bet.class);
        verify(betRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(BetStatus.PROCESSED);
    }

    @Test
    void consumeBets_processingFailure_updatesBetStatusToFailed() {
        when(contributionService.processBatch(any()))
                .thenThrow(new JackpotNotFoundException(jackpotId));
        when(betRepository.findById(betId)).thenReturn(Optional.of(betEntity));
        when(betRepository.save(any())).thenReturn(betEntity);

        assertThatThrownBy(() -> consumer.consumeBets(List.of(bet), acknowledgment))
                .isInstanceOf(JackpotNotFoundException.class);

        ArgumentCaptor<Bet> captor = ArgumentCaptor.forClass(Bet.class);
        verify(betRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(BetStatus.FAILED);
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void consumeBets_betNotFoundInRepository_continuesProcessing() {
        when(contributionService.processBatch(any())).thenReturn(List.of());
        when(betRepository.findById(betId)).thenReturn(Optional.empty());

        consumer.consumeBets(List.of(bet), acknowledgment);

        verify(contributionService).processBatch(List.of(bet));
        verify(acknowledgment).acknowledge();
        verify(betRepository, never()).save(any());
    }

    @Test
    void consumeBets_multipleBets_processesAll() {
        BetMessage bet2 = BetMessage.builder()
                .betId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .jackpotId(jackpotId)
                .betAmount(BigDecimal.valueOf(200))
                .build();

        Bet betEntity2 = Bet.builder()
                .id(bet2.getBetId())
                .status(BetStatus.PUBLISHED)
                .build();

        when(contributionService.processBatch(any())).thenReturn(List.of());
        when(betRepository.findById(betId)).thenReturn(Optional.of(betEntity));
        when(betRepository.findById(bet2.getBetId())).thenReturn(Optional.of(betEntity2));
        when(betRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        consumer.consumeBets(List.of(bet, bet2), acknowledgment);

        verify(betRepository, times(2)).save(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeBets_largeBatch_processesAll() {
        List<BetMessage> largeBatch = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            UUID id = UUID.randomUUID();
            largeBatch.add(BetMessage.builder()
                    .betId(id)
                    .userId(UUID.randomUUID())
                    .jackpotId(jackpotId)
                    .betAmount(BigDecimal.valueOf(10))
                    .build());
        }

        when(contributionService.processBatch(any())).thenReturn(List.of());
        when(betRepository.findById(any())).thenReturn(Optional.empty());

        consumer.consumeBets(largeBatch, acknowledgment);

        verify(contributionService).processBatch(largeBatch);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeBets_processingThrowsRuntimeException_marksBetsFailed() {
        when(contributionService.processBatch(any()))
                .thenThrow(new RuntimeException("Database connection failed"));
        when(betRepository.findById(betId)).thenReturn(Optional.of(betEntity));
        when(betRepository.save(any())).thenReturn(betEntity);

        assertThatThrownBy(() -> consumer.consumeBets(List.of(bet), acknowledgment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection failed");

        verify(acknowledgment, never()).acknowledge();
        ArgumentCaptor<Bet> captor = ArgumentCaptor.forClass(Bet.class);
        verify(betRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(BetStatus.FAILED);
    }

    @Test
    void consumeBets_onSuccess_preservesBetMetadata() {
        when(contributionService.processBatch(any())).thenReturn(List.of());
        when(betRepository.findById(betId)).thenReturn(Optional.of(betEntity));
        when(betRepository.save(any())).thenReturn(betEntity);

        consumer.consumeBets(List.of(bet), acknowledgment);

        ArgumentCaptor<Bet> captor = ArgumentCaptor.forClass(Bet.class);
        verify(betRepository).save(captor.capture());
        Bet saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(betId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getJackpotId()).isEqualTo(jackpotId);
    }

    @Test
    void consumeBets_withContributions_processesSuccessfully() {
        JackpotContribution contribution = JackpotContribution.builder()
                .id(UUID.randomUUID())
                .betId(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .stakeAmount(BigDecimal.valueOf(100))
                .contributionAmount(BigDecimal.valueOf(5))
                .build();

        when(contributionService.processBatch(any())).thenReturn(List.of(contribution));
        when(betRepository.findById(betId)).thenReturn(Optional.of(betEntity));
        when(betRepository.save(any())).thenReturn(betEntity);

        consumer.consumeBets(List.of(bet), acknowledgment);

        verify(acknowledgment).acknowledge();
    }
}
