package com.sporty.jackpot.kafka;

import com.sporty.jackpot.dto.BetMessage;
import com.sporty.jackpot.exception.KafkaPublishException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaBetProducer {

    private final KafkaTemplate<String, BetMessage> kafkaTemplate;

    @Value("${jackpot.kafka.topic.bets:jackpot-bets}")
    private String topic;

    public CompletableFuture<SendResult<String, BetMessage>> publishBet(BetMessage bet) {
        String key = bet.getJackpotId().toString();

        return kafkaTemplate.send(topic, key, bet)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish bet {}: {}", bet.getBetId(), ex.getMessage());
                    } else {
                        log.debug("Published bet {} to partition {}",
                                bet.getBetId(), result.getRecordMetadata().partition());
                    }
                });
    }

    public void publishBetSync(BetMessage bet) {
        try {
            publishBet(bet).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new KafkaPublishException("Failed to publish bet: " + bet.getBetId(), e);
        }
    }

    public List<CompletableFuture<SendResult<String, BetMessage>>> publishBets(List<BetMessage> bets) {
        return bets.stream()
                .map(this::publishBet)
                .toList();
    }
}
