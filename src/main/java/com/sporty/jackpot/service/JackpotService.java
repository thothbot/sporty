package com.sporty.jackpot.service;

import com.sporty.jackpot.dto.JackpotDto;
import com.sporty.jackpot.dto.JackpotRequest;
import com.sporty.jackpot.entity.Jackpot;
import com.sporty.jackpot.exception.JackpotNotFoundException;
import com.sporty.jackpot.mapper.JackpotMapper;
import com.sporty.jackpot.repository.JackpotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JackpotService {

    private final JackpotRepository jackpotRepository;
    private final JackpotMapper jackpotMapper;

    @Transactional
    public JackpotDto createJackpot(JackpotRequest request) {
        Jackpot jackpot = Jackpot.builder()
                .name(request.getName())
                .initialPoolValue(request.getInitialPoolValue())
                .currentPoolValue(request.getInitialPoolValue())
                .contributionType(request.getContributionType())
                .contributionPercentage(request.getContributionPercentage())
                .rewardType(request.getRewardType())
                .rewardChancePercentage(request.getRewardChancePercentage())
                .maxPoolLimit(request.getMaxPoolLimit())
                .build();

        Jackpot saved = jackpotRepository.save(jackpot);
        log.info("Created jackpot {} with name '{}'", saved.getId(), saved.getName());

        return jackpotMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public JackpotDto getJackpot(UUID id) {
        Jackpot jackpot = jackpotRepository.findById(id)
                .orElseThrow(() -> new JackpotNotFoundException(id));

        return jackpotMapper.toDto(jackpot);
    }

    @Transactional(readOnly = true)
    public List<JackpotDto> getAllJackpots() {
        return jackpotRepository.findAll().stream()
                .map(jackpotMapper::toDto)
                .toList();
    }
}
