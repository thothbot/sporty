package com.sporty.jackpot.config;

import com.sporty.jackpot.dto.BetMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {

    @Value("${jackpot.kafka.consumer.concurrency:10}")
    private int concurrency;

    @Value("${jackpot.kafka.consumer.use-virtual-threads:true}")
    private boolean useVirtualThreads;

    @Value("${jackpot.kafka.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${jackpot.kafka.retry.initial-interval-ms:1000}")
    private long initialIntervalMs;

    @Value("${jackpot.kafka.retry.multiplier:2.0}")
    private double multiplier;

    @Value("${jackpot.kafka.retry.max-interval-ms:10000}")
    private long maxIntervalMs;

    @Bean
    public ConsumerFactory<String, BetMessage> consumerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(null));
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, BetMessage> kafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> {
                    log.error("Sending message to DLQ. Topic: {}, Partition: {}, Offset: {}, Error: {}",
                            record.topic(), record.partition(), record.offset(), ex.getMessage());
                    return new TopicPartition(
                            record.topic() + "-dlq",
                            record.partition()
                    );
                }
        );

        ExponentialBackOff backOff = new ExponentialBackOff(initialIntervalMs, multiplier);
        backOff.setMaxInterval(maxIntervalMs);
        backOff.setMaxElapsedTime(maxIntervalMs * maxRetryAttempts);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class
        );

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("Retry attempt {} for record in topic {} partition {} offset {}. Error: {}",
                        deliveryAttempt,
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        ex.getMessage())
        );

        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BetMessage> kafkaListenerContainerFactory(
            ConsumerFactory<String, BetMessage> consumerFactory,
            CommonErrorHandler kafkaErrorHandler) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, BetMessage>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(concurrency);
        factory.setBatchListener(true);
        factory.setCommonErrorHandler(kafkaErrorHandler);

        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        if (useVirtualThreads) {
            SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("kafka-");
            executor.setVirtualThreads(true);
            factory.getContainerProperties().setListenerTaskExecutor(executor);
        }

        return factory;
    }
}
