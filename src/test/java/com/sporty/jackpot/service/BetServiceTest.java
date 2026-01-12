package com.sporty.jackpot.service;

import com.sporty.jackpot.dto.BetMessage;
import com.sporty.jackpot.dto.BetRequest;
import com.sporty.jackpot.entity.Bet;
import com.sporty.jackpot.entity.BetStatus;
import com.sporty.jackpot.exception.BetNotFoundException;
import com.sporty.jackpot.repository.BetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BetServiceTest {

    @Mock
    private BetRepository betRepository;

    @InjectMocks
    private BetService betService;

    private UUID betId;
    private UUID userId;
    private UUID jackpotId;
    private BetRequest request;
    private Bet bet;

    @BeforeEach
    void setUp() {
        betId = UUID.randomUUID();
        userId = UUID.randomUUID();
        jackpotId = UUID.randomUUID();

        request = BetRequest.builder()
                .userId(userId)
                .jackpotId(jackpotId)
                .betAmount(BigDecimal.valueOf(100))
                .build();

        bet = Bet.builder()
                .id(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .betAmount(BigDecimal.valueOf(100))
                .status(BetStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void createBet_validRequest_createsBetWithPendingStatus() {
        when(betRepository.save(any(Bet.class))).thenReturn(bet);

        Bet result = betService.createBet(betId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(betId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getJackpotId()).isEqualTo(jackpotId);
        assertThat(result.getBetAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));

        ArgumentCaptor<Bet> betCaptor = ArgumentCaptor.forClass(Bet.class);
        verify(betRepository).save(betCaptor.capture());

        Bet savedBet = betCaptor.getValue();
        assertThat(savedBet.getStatus()).isEqualTo(BetStatus.PENDING);
    }

    @Test
    void updateStatus_existingBet_updatesStatus() {
        when(betRepository.findById(betId)).thenReturn(Optional.of(bet));
        when(betRepository.save(any(Bet.class))).thenReturn(bet);

        betService.updateStatus(betId, BetStatus.PUBLISHED);

        verify(betRepository).save(any(Bet.class));
        assertThat(bet.getStatus()).isEqualTo(BetStatus.PUBLISHED);
    }

    @Test
    void updateStatus_nonExistingBet_throwsException() {
        when(betRepository.findById(betId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> betService.updateStatus(betId, BetStatus.PUBLISHED))
                .isInstanceOf(BetNotFoundException.class);
    }

    @Test
    void getBet_existingBet_returnsBet() {
        when(betRepository.findById(betId)).thenReturn(Optional.of(bet));

        Bet result = betService.getBet(betId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(betId);
    }

    @Test
    void getBet_nonExistingBet_throwsException() {
        when(betRepository.findById(betId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> betService.getBet(betId))
                .isInstanceOf(BetNotFoundException.class);
    }

    @Test
    void toMessage_bet_returnsBetMessage() {
        BetMessage message = betService.toMessage(bet);

        assertThat(message).isNotNull();
        assertThat(message.getBetId()).isEqualTo(betId);
        assertThat(message.getUserId()).isEqualTo(userId);
        assertThat(message.getJackpotId()).isEqualTo(jackpotId);
        assertThat(message.getBetAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    // ========== Edge Case Tests ==========

    @Test
    void createBet_veryLargeBetAmount_createsBet() {
        BetRequest largeRequest = BetRequest.builder()
                .userId(userId)
                .jackpotId(jackpotId)
                .betAmount(new BigDecimal("99999999999999.9999"))
                .build();

        Bet largeBet = Bet.builder()
                .id(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .betAmount(largeRequest.getBetAmount())
                .status(BetStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(betRepository.save(any(Bet.class))).thenReturn(largeBet);

        Bet result = betService.createBet(betId, largeRequest);

        assertThat(result.getBetAmount()).isEqualByComparingTo(new BigDecimal("99999999999999.9999"));
    }

    @Test
    void createBet_verySmallBetAmount_createsBet() {
        BetRequest smallRequest = BetRequest.builder()
                .userId(userId)
                .jackpotId(jackpotId)
                .betAmount(new BigDecimal("0.0001"))
                .build();

        Bet smallBet = Bet.builder()
                .id(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .betAmount(smallRequest.getBetAmount())
                .status(BetStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(betRepository.save(any(Bet.class))).thenReturn(smallBet);

        Bet result = betService.createBet(betId, smallRequest);

        assertThat(result.getBetAmount()).isEqualByComparingTo(new BigDecimal("0.0001"));
    }

    @Test
    void updateStatus_toProcessed_updatesCorrectly() {
        when(betRepository.findById(betId)).thenReturn(Optional.of(bet));
        when(betRepository.save(any(Bet.class))).thenReturn(bet);

        betService.updateStatus(betId, BetStatus.PROCESSED);

        assertThat(bet.getStatus()).isEqualTo(BetStatus.PROCESSED);
    }

    @Test
    void updateStatus_toFailed_updatesCorrectly() {
        when(betRepository.findById(betId)).thenReturn(Optional.of(bet));
        when(betRepository.save(any(Bet.class))).thenReturn(bet);

        betService.updateStatus(betId, BetStatus.FAILED);

        assertThat(bet.getStatus()).isEqualTo(BetStatus.FAILED);
    }

    @Test
    void updateStatus_multipleUpdates_lastStatusPersists() {
        when(betRepository.findById(betId)).thenReturn(Optional.of(bet));
        when(betRepository.save(any(Bet.class))).thenReturn(bet);

        betService.updateStatus(betId, BetStatus.PUBLISHED);
        assertThat(bet.getStatus()).isEqualTo(BetStatus.PUBLISHED);

        betService.updateStatus(betId, BetStatus.PROCESSED);
        assertThat(bet.getStatus()).isEqualTo(BetStatus.PROCESSED);
    }

    @Test
    void createBet_savesWithCorrectInitialStatus() {
        when(betRepository.save(any(Bet.class))).thenAnswer(invocation -> {
            Bet savedBet = invocation.getArgument(0);
            assertThat(savedBet.getStatus()).isEqualTo(BetStatus.PENDING);
            return savedBet;
        });

        betService.createBet(betId, request);

        verify(betRepository).save(any(Bet.class));
    }

    @Test
    void createBet_preservesAllRequestFields() {
        ArgumentCaptor<Bet> betCaptor = ArgumentCaptor.forClass(Bet.class);
        when(betRepository.save(betCaptor.capture())).thenReturn(bet);

        betService.createBet(betId, request);

        Bet capturedBet = betCaptor.getValue();
        assertThat(capturedBet.getId()).isEqualTo(betId);
        assertThat(capturedBet.getUserId()).isEqualTo(request.getUserId());
        assertThat(capturedBet.getJackpotId()).isEqualTo(request.getJackpotId());
        assertThat(capturedBet.getBetAmount()).isEqualByComparingTo(request.getBetAmount());
    }

    @Test
    void toMessage_preservesAllFields() {
        Bet fullBet = Bet.builder()
                .id(betId)
                .userId(userId)
                .jackpotId(jackpotId)
                .betAmount(new BigDecimal("12345.6789"))
                .status(BetStatus.PROCESSED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        BetMessage message = betService.toMessage(fullBet);

        assertThat(message.getBetId()).isEqualTo(betId);
        assertThat(message.getUserId()).isEqualTo(userId);
        assertThat(message.getJackpotId()).isEqualTo(jackpotId);
        assertThat(message.getBetAmount()).isEqualByComparingTo(new BigDecimal("12345.6789"));
    }

    @Test
    void getBet_exceptionMessageContainsBetId() {
        UUID missingBetId = UUID.randomUUID();
        when(betRepository.findById(missingBetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> betService.getBet(missingBetId))
                .isInstanceOf(BetNotFoundException.class)
                .hasMessageContaining(missingBetId.toString());
    }

    @Test
    void updateStatus_exceptionMessageContainsBetId() {
        UUID missingBetId = UUID.randomUUID();
        when(betRepository.findById(missingBetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> betService.updateStatus(missingBetId, BetStatus.PUBLISHED))
                .isInstanceOf(BetNotFoundException.class)
                .hasMessageContaining(missingBetId.toString());
    }
}
