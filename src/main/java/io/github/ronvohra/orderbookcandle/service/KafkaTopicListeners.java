package io.github.ronvohra.orderbookcandle.service;

import io.github.ronvohra.orderbookcandle.domain.Candle;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaTopicListeners {

  private final Logger logger = LogManager.getLogger(KafkaTopicListeners.class);

  @KafkaListener(
      topics = "${spring.kafka.topic.name}",
      groupId = "${spring.kafka.consumer.group-id}")
  public void consume(Candle candle) {

    logger.info(
        "Candle at timestamp {}: {}", Instant.ofEpochMilli(candle.timeEpochMillis()), candle);
  }
}
