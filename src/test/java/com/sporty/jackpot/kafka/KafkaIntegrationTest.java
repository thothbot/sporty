package com.sporty.jackpot.kafka;

import com.sporty.jackpot.dto.BetMessage;
import com.sporty.jackpot.entity.ContributionType;
import com.sporty.jackpot.entity.Jackpot;
import com.sporty.jackpot.entity.RewardType;
import com.sporty.jackpot.repository.JackpotContributionRepository;
import com.sporty.jackpot.repository.JackpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"jackpot-bets"}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "jackpot.kafka.topic.bets=jackpot-bets"
})
class KafkaIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaBetProducer producer;

    @Autowired
    private JackpotRepository jackpotRepository;

    @Autowired
    private JackpotContributionRepository contributionRepository;

    private Jackpot testJackpot;

    @BeforeEach
    void setUp() {
        testJackpot = jackpotRepository.save(Jackpot.builder()
                .name("Integration Test Jackpot")
                .contributionType(ContributionType.FIXED)
                .contributionPercentage(BigDecimal.valueOf(0.05))
                .rewardType(RewardType.FIXED)
                .rewardChancePercentage(BigDecimal.valueOf(0.01))
                .initialPoolValue(BigDecimal.valueOf(1000))
                .currentPoolValue(BigDecimal.valueOf(1000))
                .maxPoolLimit(BigDecimal.valueOf(100000))
                .build());
    }

    @Test
    void publishAndConsumeBet_createsContribution() {
        UUID betId = UUID.randomUUID();
        BetMessage bet = BetMessage.builder()
                .betId(betId)
                .userId(UUID.randomUUID())
                .jackpotId(testJackpot.getId())
                .betAmount(BigDecimal.valueOf(100))
                .build();

        producer.publishBetSync(bet);

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var contributions = contributionRepository.findByBetId(betId);
                    assertThat(contributions).isNotEmpty();
                    assertThat(contributions.get(0).getContributionAmount())
                            .isEqualByComparingTo(BigDecimal.valueOf(5));
                });
    }

    @Test
    void publishBet_updatesJackpotPool() {
        UUID betId = UUID.randomUUID();
        BetMessage bet = BetMessage.builder()
                .betId(betId)
                .userId(UUID.randomUUID())
                .jackpotId(testJackpot.getId())
                .betAmount(BigDecimal.valueOf(100))
                .build();

        producer.publishBetSync(bet);

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var updatedJackpot = jackpotRepository.findById(testJackpot.getId()).orElseThrow();
                    assertThat(updatedJackpot.getCurrentPoolValue())
                            .isEqualByComparingTo(BigDecimal.valueOf(1005));
                });
    }
}
