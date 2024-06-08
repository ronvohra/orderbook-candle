package io.gsr.orderbookcandle.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gsr.orderbookcandle.domain.SnapshotResponse;
import io.gsr.orderbookcandle.domain.SubscriptionRequest;
import io.gsr.orderbookcandle.domain.SubscriptionResponse;
import io.gsr.orderbookcandle.domain.UpdateResponse;
import io.gsr.orderbookcandle.orderbook.KrakenOrderbookManager;
import java.time.Instant;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class KrakenWebsocketHandler extends TextWebSocketHandler {

  private final ObjectMapper mapper = new ObjectMapper();

  private final Logger logger = LogManager.getLogger(KrakenWebsocketHandler.class);
  @Autowired private KrakenOrderbookManager manager;

  @Autowired private KafkaTemplate kafkaTemplate;

  @Value("${kraken.symbols}")
  private List<String> symbols;

  @Value("${spring.kafka.topic.name}")
  private String topic;

  private long startTimeMillis;

  public KrakenWebsocketHandler(KrakenOrderbookManager manager) {
    this.manager = manager;
  }

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message)
      throws JsonProcessingException {

    var jsonNode = mapper.readTree(message.getPayload());
    var type = jsonNode.findValuesAsText("type").stream().findFirst().orElse("unknownType");
    var channel =
        jsonNode.findValuesAsText("channel").stream().findFirst().orElse("unknownChannel");
    var method = jsonNode.findValuesAsText("method").stream().findFirst().orElse("unknownMethod");
    if (method.equals("subscribe")) {
      var subscriptionMessage = mapper.readValue(message.getPayload(), SubscriptionResponse.class);
      assert subscriptionMessage.success();
      startTimeMillis = Instant.parse(subscriptionMessage.timeOut()).toEpochMilli();
      return;
    }

    if (!channel.equals("book")) return;

    switch (type) {
      case "snapshot" -> handleOrderbookSnapshotEvent(message);
      case "update" -> handleOrderbookUpdateEvent(message);
      default -> logger.info("type={} is not an update or snapshot event", type);
    }
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    logger.info("Connected to Kraken websocket server");
    String requestPayload =
        mapper.writeValueAsString(
            new SubscriptionRequest("subscribe", new SubscriptionRequest.Params("book", symbols)));

    logger.info("Sending {}", requestPayload);

    session.sendMessage(new TextMessage(requestPayload));
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    logger.info("Connection Closed; reason: {}", status.getReason());
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable ex) {
    logger.error("Transport error", ex);
  }

  long getStartTimeMillis() {
    return startTimeMillis;
  }

  void setStartTimeMillis(long timeMillis) {
    startTimeMillis = timeMillis;
  }

  private void handleOrderbookSnapshotEvent(TextMessage message) throws JsonProcessingException {
    assert startTimeMillis > 0L;
    var snapshotMessage = mapper.readValue(message.getPayload(), SnapshotResponse.class);
    try {
      logger.info("Received snapshot event: {}", snapshotMessage);
      manager.createOrderbooksFromSnapshot(snapshotMessage, startTimeMillis);
    } catch (Exception e) {
      logger.error("An error occurred while handling snapshot event: {}", e.getMessage());
    }
  }

  private void handleOrderbookUpdateEvent(TextMessage message) throws JsonProcessingException {
    assert startTimeMillis > 0L;
    var updateMessage = mapper.readValue(message.getPayload(), UpdateResponse.class);
    try {
      var symbolCandleMap = manager.appendToOrderbooksFromUpdate(updateMessage);
      if (!symbolCandleMap.isEmpty()) {
        startTimeMillis =
            Instant.parse(
                    updateMessage.data().stream()
                        .findFirst()
                        .orElseThrow(IllegalStateException::new)
                        .timestamp())
                .toEpochMilli();
        symbolCandleMap.forEach((symbol, candle) -> kafkaTemplate.send(topic, symbol, candle));
      }
    } catch (Exception e) {
      logger.error("An error occurred while handling update event: {}", e.getMessage());
    }
  }
}
