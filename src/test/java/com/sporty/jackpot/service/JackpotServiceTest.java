package com.sporty.jackpot.service;

import com.sporty.jackpot.dto.JackpotDto;
import com.sporty.jackpot.dto.JackpotRequest;
import com.sporty.jackpot.entity.ContributionType;
import com.sporty.jackpot.entity.Jackpot;
import com.sporty.jackpot.entity.RewardType;
import com.sporty.jackpot.exception.JackpotNotFoundException;
import com.sporty.jackpot.mapper.JackpotMapper;
import com.sporty.jackpot.repository.JackpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JackpotServiceTest {

    @Mock
    private JackpotRepository jackpotRepository;

    @Mock
    private JackpotMapper jackpotMapper;

    @InjectMocks
    private JackpotService jackpotService;

    private JackpotRequest request;
    private Jackpot jackpot;
    private JackpotDto jackpotDto;
    private UUID jackpotId;

    @BeforeEach
    void setUp() {
        jackpotId = UUID.randomUUID();

        request = JackpotRequest.builder()
                .name("Test Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .maxPoolLimit(BigDecimal.valueOf(100000))
                .build();

        jackpot = Jackpot.builder()
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

        jackpotDto = JackpotDto.builder()
                .id(jackpotId)
                .name("Test Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .currentPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .maxPoolLimit(BigDecimal.valueOf(100000))
                .createdAt(jackpot.getCreatedAt())
                .updatedAt(jackpot.getUpdatedAt())
                .build();
    }

    @Test
    void createJackpot_validRequest_createsAndReturnsJackpot() {
        when(jackpotRepository.save(any(Jackpot.class))).thenReturn(jackpot);
        when(jackpotMapper.toDto(jackpot)).thenReturn(jackpotDto);

        JackpotDto result = jackpotService.createJackpot(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(jackpotId);
        assertThat(result.getName()).isEqualTo("Test Jackpot");
        assertThat(result.getCurrentPoolValue()).isEqualByComparingTo(BigDecimal.valueOf(10000));

        verify(jackpotRepository).save(any(Jackpot.class));
    }

    @Test
    void createJackpot_setsCurrentPoolToInitialValue() {
        when(jackpotRepository.save(any(Jackpot.class))).thenAnswer(invocation -> {
            Jackpot saved = invocation.getArgument(0);
            assertThat(saved.getCurrentPoolValue()).isEqualByComparingTo(saved.getInitialPoolValue());
            saved.setId(jackpotId);
            return saved;
        });
        when(jackpotMapper.toDto(any(Jackpot.class))).thenReturn(jackpotDto);

        jackpotService.createJackpot(request);

        verify(jackpotRepository).save(any(Jackpot.class));
    }

    @Test
    void getJackpot_existingId_returnsJackpot() {
        when(jackpotRepository.findById(jackpotId)).thenReturn(Optional.of(jackpot));
        when(jackpotMapper.toDto(jackpot)).thenReturn(jackpotDto);

        JackpotDto result = jackpotService.getJackpot(jackpotId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(jackpotId);
        assertThat(result.getName()).isEqualTo("Test Jackpot");
    }

    @Test
    void getJackpot_nonExistingId_throwsException() {
        when(jackpotRepository.findById(jackpotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jackpotService.getJackpot(jackpotId))
                .isInstanceOf(JackpotNotFoundException.class);
    }

    @Test
    void getAllJackpots_returnsAllJackpots() {
        Jackpot jackpot2 = Jackpot.builder()
                .id(UUID.randomUUID())
                .name("Another Jackpot")
                .build();

        JackpotDto jackpotDto2 = JackpotDto.builder()
                .id(jackpot2.getId())
                .name("Another Jackpot")
                .build();

        when(jackpotRepository.findAll()).thenReturn(List.of(jackpot, jackpot2));
        when(jackpotMapper.toDto(jackpot)).thenReturn(jackpotDto);
        when(jackpotMapper.toDto(jackpot2)).thenReturn(jackpotDto2);

        List<JackpotDto> result = jackpotService.getAllJackpots();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(JackpotDto::getName)
                .containsExactly("Test Jackpot", "Another Jackpot");
    }

    @Test
    void getAllJackpots_emptyDatabase_returnsEmptyList() {
        when(jackpotRepository.findAll()).thenReturn(List.of());

        List<JackpotDto> result = jackpotService.getAllJackpots();

        assertThat(result).isEmpty();
    }

    // ========== Edge Case Tests ==========

    @Test
    void createJackpot_withNullMaxPoolLimit_createsJackpot() {
        JackpotRequest requestWithoutLimit = JackpotRequest.builder()
                .name("No Limit Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.VARIABLE)
                .contributionPercentage(BigDecimal.valueOf(0.10))
                .rewardType(RewardType.VARIABLE)
                .rewardChancePercentage(BigDecimal.valueOf(0.05))
                .maxPoolLimit(null)
                .build();

        Jackpot savedJackpot = Jackpot.builder()
                .id(jackpotId)
                .name("No Limit Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .currentPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.VARIABLE)
                .contributionPercentage(BigDecimal.valueOf(0.10))
                .rewardType(RewardType.VARIABLE)
                .rewardChancePercentage(BigDecimal.valueOf(0.05))
                .maxPoolLimit(null)
                .build();

        JackpotDto resultDto = JackpotDto.builder()
                .id(jackpotId)
                .name("No Limit Jackpot")
                .maxPoolLimit(null)
                .build();

        when(jackpotRepository.save(any(Jackpot.class))).thenReturn(savedJackpot);
        when(jackpotMapper.toDto(savedJackpot)).thenReturn(resultDto);

        JackpotDto result = jackpotService.createJackpot(requestWithoutLimit);

        assertThat(result).isNotNull();
        assertThat(result.getMaxPoolLimit()).isNull();
    }

    @Test
    void createJackpot_withZeroInitialPool_createsJackpot() {
        JackpotRequest zeroPoolRequest = JackpotRequest.builder()
                .name("Zero Start Jackpot")
                .initialPoolValue(BigDecimal.ZERO)
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .build();

        when(jackpotRepository.save(any(Jackpot.class))).thenAnswer(invocation -> {
            Jackpot saved = invocation.getArgument(0);
            assertThat(saved.getInitialPoolValue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getCurrentPoolValue()).isEqualByComparingTo(BigDecimal.ZERO);
            saved.setId(jackpotId);
            return saved;
        });
        when(jackpotMapper.toDto(any(Jackpot.class))).thenReturn(jackpotDto);

        jackpotService.createJackpot(zeroPoolRequest);

        verify(jackpotRepository).save(any(Jackpot.class));
    }

    @Test
    void createJackpot_withVariableStrategies_setsCorrectTypes() {
        JackpotRequest variableRequest = JackpotRequest.builder()
                .name("Variable Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.VARIABLE)
                .contributionPercentage(BigDecimal.valueOf(0.10))
                .rewardType(RewardType.VARIABLE)
                .rewardChancePercentage(BigDecimal.valueOf(0.05))
                .maxPoolLimit(BigDecimal.valueOf(100000))
                .build();

        ArgumentCaptor<Jackpot> captor = ArgumentCaptor.forClass(Jackpot.class);
        when(jackpotRepository.save(captor.capture())).thenReturn(jackpot);
        when(jackpotMapper.toDto(any())).thenReturn(jackpotDto);

        jackpotService.createJackpot(variableRequest);

        Jackpot captured = captor.getValue();
        assertThat(captured.getContributionType()).isEqualTo(ContributionType.VARIABLE);
        assertThat(captured.getRewardType()).isEqualTo(RewardType.VARIABLE);
    }

    @Test
    void createJackpot_withVeryLargePoolValues_handlesCorrectly() {
        JackpotRequest largeRequest = JackpotRequest.builder()
                .name("High Roller")
                .initialPoolValue(new BigDecimal("999999999999999.9999"))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.01))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.001))
                .maxPoolLimit(new BigDecimal("9999999999999999.9999"))
                .build();

        ArgumentCaptor<Jackpot> captor = ArgumentCaptor.forClass(Jackpot.class);
        when(jackpotRepository.save(captor.capture())).thenReturn(jackpot);
        when(jackpotMapper.toDto(any())).thenReturn(jackpotDto);

        jackpotService.createJackpot(largeRequest);

        Jackpot captured = captor.getValue();
        assertThat(captured.getInitialPoolValue()).isEqualByComparingTo(new BigDecimal("999999999999999.9999"));
        assertThat(captured.getMaxPoolLimit()).isEqualByComparingTo(new BigDecimal("9999999999999999.9999"));
    }

    @Test
    void createJackpot_preservesAllRequestFields() {
        ArgumentCaptor<Jackpot> captor = ArgumentCaptor.forClass(Jackpot.class);
        when(jackpotRepository.save(captor.capture())).thenReturn(jackpot);
        when(jackpotMapper.toDto(any())).thenReturn(jackpotDto);

        jackpotService.createJackpot(request);

        Jackpot captured = captor.getValue();
        assertThat(captured.getName()).isEqualTo(request.getName());
        assertThat(captured.getInitialPoolValue()).isEqualByComparingTo(request.getInitialPoolValue());
        assertThat(captured.getContributionType()).isEqualTo(request.getContributionType());
        assertThat(captured.getContributionPercentage()).isEqualByComparingTo(request.getContributionPercentage());
        assertThat(captured.getRewardType()).isEqualTo(request.getRewardType());
        assertThat(captured.getRewardChancePercentage()).isEqualByComparingTo(request.getRewardChancePercentage());
        assertThat(captured.getMaxPoolLimit()).isEqualByComparingTo(request.getMaxPoolLimit());
    }

    @Test
    void getJackpot_exceptionMessageContainsJackpotId() {
        UUID missingId = UUID.randomUUID();
        when(jackpotRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jackpotService.getJackpot(missingId))
                .isInstanceOf(JackpotNotFoundException.class)
                .hasMessageContaining(missingId.toString());
    }

    @Test
    void getAllJackpots_withManyJackpots_returnsAllInOrder() {
        List<Jackpot> manyJackpots = List.of(
                Jackpot.builder().id(UUID.randomUUID()).name("Jackpot 1").build(),
                Jackpot.builder().id(UUID.randomUUID()).name("Jackpot 2").build(),
                Jackpot.builder().id(UUID.randomUUID()).name("Jackpot 3").build(),
                Jackpot.builder().id(UUID.randomUUID()).name("Jackpot 4").build(),
                Jackpot.builder().id(UUID.randomUUID()).name("Jackpot 5").build()
        );

        when(jackpotRepository.findAll()).thenReturn(manyJackpots);
        for (int i = 0; i < manyJackpots.size(); i++) {
            Jackpot j = manyJackpots.get(i);
            JackpotDto dto = JackpotDto.builder().id(j.getId()).name(j.getName()).build();
            when(jackpotMapper.toDto(j)).thenReturn(dto);
        }

        List<JackpotDto> result = jackpotService.getAllJackpots();

        assertThat(result).hasSize(5);
        assertThat(result).extracting(JackpotDto::getName)
                .containsExactly("Jackpot 1", "Jackpot 2", "Jackpot 3", "Jackpot 4", "Jackpot 5");
    }

    @Test
    void createJackpot_withZeroRewardChance_createsJackpot() {
        JackpotRequest zeroChanceRequest = JackpotRequest.builder()
                .name("No Win Jackpot")
                .initialPoolValue(BigDecimal.valueOf(10000))
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.ZERO)
                .build();

        ArgumentCaptor<Jackpot> captor = ArgumentCaptor.forClass(Jackpot.class);
        when(jackpotRepository.save(captor.capture())).thenReturn(jackpot);
        when(jackpotMapper.toDto(any())).thenReturn(jackpotDto);

        jackpotService.createJackpot(zeroChanceRequest);

        Jackpot captured = captor.getValue();
        assertThat(captured.getRewardChancePercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
