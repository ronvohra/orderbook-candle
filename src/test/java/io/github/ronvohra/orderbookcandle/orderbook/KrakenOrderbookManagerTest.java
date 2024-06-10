package io.github.ronvohra.orderbookcandle.orderbook;

import static io.github.ronvohra.orderbookcandle.TestHelpers.*;
import static io.github.ronvohra.orderbookcandle.domain.Side.ASK;
import static io.github.ronvohra.orderbookcandle.domain.Side.BID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest()
@TestPropertySource(locations = "classpath:test.properties")
public class KrakenOrderbookManagerTest {
  private final KrakenOrderbookManager manager = new KrakenOrderbookManager();

  @Test
  public void createOrderbooksAndMidPricesOnSnapshot() {
    var snapshot = createSnapshotResponse();

    assertTrue(manager.getOrderbooks().isEmpty());
    assertTrue(manager.getTickCount().isEmpty());

    manager.createOrderbooksFromSnapshot(snapshot, 42L);

    assertEquals(2, manager.getOrderbooks().get(SYMBOL).getBookBySide(BID).size());
    assertEquals(2, manager.getOrderbooks().get(SYMBOL).getBookBySide(ASK).size());

    assertEquals(99, manager.getOrderbooks().get(SYMBOL).getTopOfBookOrder(BID).get().price());
    assertEquals(101, manager.getOrderbooks().get(SYMBOL).getTopOfBookOrder(ASK).get().price());

    assertEquals(100, manager.getMidPrices().get(SYMBOL).getOpenMidPrice());
    assertEquals(100, manager.getMidPrices().get(SYMBOL).getHighMidPrice());
    assertEquals(100, manager.getMidPrices().get(SYMBOL).getLowMidPrice());
    assertEquals(0, manager.getMidPrices().get(SYMBOL).getCloseMidPrice());

    assertEquals(1, manager.getTickCount().get(SYMBOL));
  }

  @Test
  public void appendToOrderbooksAndMidPricesOnSnapshot() {
    var now = Instant.now();
    var justBeforeNow = Instant.ofEpochSecond(now.getEpochSecond() - 5L);

    var snapshot = createSnapshotResponse();
    manager.createOrderbooksFromSnapshot(snapshot, justBeforeNow.toEpochMilli());

    var update = createUpdateResponse();
    manager.appendToOrderbooksFromUpdate(update);

    assertEquals(4, manager.getOrderbooks().get(SYMBOL).getBookBySide(BID).size());
    assertEquals(4, manager.getOrderbooks().get(SYMBOL).getBookBySide(ASK).size());

    assertEquals(99, manager.getOrderbooks().get(SYMBOL).getTopOfBookOrder(BID).get().price());
    assertEquals(100, manager.getOrderbooks().get(SYMBOL).getTopOfBookOrder(ASK).get().price());

    assertEquals(100, manager.getMidPrices().get(SYMBOL).getOpenMidPrice());
    assertEquals(100, manager.getMidPrices().get(SYMBOL).getHighMidPrice());
    assertEquals(99.5, manager.getMidPrices().get(SYMBOL).getLowMidPrice());
    assertEquals(0, manager.getMidPrices().get(SYMBOL).getCloseMidPrice());

    assertEquals(2, manager.getTickCount().get(SYMBOL));
  }

  @Test
  public void appendAndResetPricingAfterCandleThresholdReached() {
    var now = Instant.now();
    var sometimeBeforeNow = Instant.ofEpochSecond(now.getEpochSecond() - 65L);

    var snapshot = createSnapshotResponse();
    manager.createOrderbooksFromSnapshot(snapshot, sometimeBeforeNow.toEpochMilli());
    var update = createUpdateResponse();
    manager.symbols = List.of(SYMBOL);
    var candles = manager.appendToOrderbooksFromUpdate(update);

    assertEquals(100, candles.get(SYMBOL).openMidPrice());
    assertEquals(100, candles.get(SYMBOL).highestMidPrice());
    assertEquals(99.5, candles.get(SYMBOL).lowestMidPrice());
    assertEquals(99.5, candles.get(SYMBOL).closeMidPrice());

    assertTrue(manager.getOrderbooks().isEmpty());
    assertTrue(manager.getTickCount().isEmpty());
  }
}
