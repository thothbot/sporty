package com.sporty.jackpot.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${jackpot.kafka.topic.bets:jackpot-bets}")
    private String topicName;

    @Value("${jackpot.kafka.topic.bets-dlq:jackpot-bets-dlq}")
    private String dlqTopicName;

    @Value("${jackpot.kafka.topic.partitions:10}")
    private int partitions;

    @Bean
    public NewTopic jackpotBetsTopic() {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic jackpotBetsDlqTopic() {
        return TopicBuilder.name(dlqTopicName)
                .partitions(partitions)
                .replicas(1)
                .build();
    }
}
