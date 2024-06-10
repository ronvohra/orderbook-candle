package io.github.ronvohra.orderbookcandle.handler;

import static io.github.ronvohra.orderbookcandle.TestHelpers.createSnapshotResponse;
import static io.github.ronvohra.orderbookcandle.TestHelpers.createUpdateResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ronvohra.orderbookcandle.domain.Candle;
import io.github.ronvohra.orderbookcandle.orderbook.KrakenOrderbookManager;
import java.time.Instant;
import java.util.HashMap;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@SpringBootTest()
@TestPropertySource(locations = "classpath:test.properties")
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
class KrakenWebsocketHandlerTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final WebSocketSession session = mock(WebSocketSession.class);
  private final KrakenOrderbookManager manager = spy(new KrakenOrderbookManager());
  private final KrakenWebsocketHandler handler = new KrakenWebsocketHandler(manager);

  @Test
  public void subscribeMessageSetsStartTime() throws JsonProcessingException {
    var timeNow = Instant.now();

    var jsonMessage = new JSONObject();
    jsonMessage.put("method", "subscribe");
    jsonMessage.put("success", true);
    jsonMessage.put("time_out", timeNow.toString());
    var message = new TextMessage(jsonMessage.toString());

    handler.handleTextMessage(session, message);

    assertEquals(timeNow.toEpochMilli(), handler.getStartTimeMillis());
  }

  @Test
  public void nothingToDoWhenMessageIsNotBookChannel() throws JsonProcessingException {
    var jsonMessage = new JSONObject();
    jsonMessage.put("channel", "not-book");
    var message = new TextMessage(jsonMessage.toString());

    handler.handleTextMessage(session, message);

    verifyNoInteractions(manager);
    assertEquals(0L, handler.getStartTimeMillis());
  }

  @Test
  public void snapshotMessageCreatesOrderbooks() throws JsonProcessingException {
    var messageObj = createSnapshotResponse();
    var messageStr = mapper.writeValueAsString(messageObj);
    var message = new TextMessage(messageStr);
    var startTimeMillis = 42L;
    handler.setStartTimeMillis(startTimeMillis);
    handler.handleTextMessage(session, message);
    doNothing().when(manager).createOrderbooksFromSnapshot(messageObj, startTimeMillis);
    verify(manager).createOrderbooksFromSnapshot(messageObj, startTimeMillis);
  }

  @Test
  public void updateMessageAppendsToOrderbooksIfLessThanCandleWindowElapsed()
      throws JsonProcessingException {
    var now = Instant.now();
    var justBeforeNow = Instant.ofEpochSecond(now.getEpochSecond() - 10L);
    var messageObj = createUpdateResponse();
    var messageStr = mapper.writeValueAsString(messageObj);
    var message = new TextMessage(messageStr);
    handler.setStartTimeMillis(justBeforeNow.toEpochMilli());
    handler.handleTextMessage(session, message);
    doReturn(new HashMap<String, Candle>()).when(manager).appendToOrderbooksFromUpdate(messageObj);
    verify(manager).appendToOrderbooksFromUpdate(messageObj);
  }
}
