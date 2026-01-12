package com.sporty.jackpot.kafka;

import com.sporty.jackpot.dto.BetMessage;
import com.sporty.jackpot.entity.BetStatus;
import com.sporty.jackpot.repository.BetRepository;
import com.sporty.jackpot.service.JackpotContributionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaBetConsumer {

    private final JackpotContributionService contributionService;
    private final BetRepository betRepository;

    @KafkaListener(
            topics = "${jackpot.kafka.topic.bets:jackpot-bets}",
            groupId = "${spring.kafka.consumer.group-id:jackpot-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBets(List<BetMessage> bets, Acknowledgment ack) {
        log.info("Received batch of {} bets", bets.size());

        try {
            contributionService.processBatch(bets);

            bets.forEach(bet -> betRepository.findById(bet.getBetId())
                    .ifPresent(b -> {
                        b.setStatus(BetStatus.PROCESSED);
                        betRepository.save(b);
                    }));

            ack.acknowledge();
            log.debug("Batch processed and acknowledged");
        } catch (Exception e) {
            log.error("Failed to process batch: {}", e.getMessage(), e);

            bets.forEach(bet -> betRepository.findById(bet.getBetId())
                    .ifPresent(b -> {
                        b.setStatus(BetStatus.FAILED);
                        betRepository.save(b);
                    }));

            throw e;
        }
    }
}
