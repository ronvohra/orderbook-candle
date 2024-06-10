package io.gsr.orderbookcandle;

import io.gsr.orderbookcandle.domain.Order;
import io.gsr.orderbookcandle.domain.SnapshotResponse;
import io.gsr.orderbookcandle.domain.UpdateResponse;
import java.time.Instant;
import java.util.List;

public class TestHelpers {
  public static final String SYMBOL = "FOO/BAR";

  public static SnapshotResponse createSnapshotResponse() {
    var bids = List.of(new Order(99, 1), new Order(98, 1));
    var asks = List.of(new Order(101, 1), new Order(102, 1));

    var snapshotData = new SnapshotResponse.SnapshotData(SYMBOL, bids, asks, 12345L);
    return new SnapshotResponse("book", "snapshot", List.of(snapshotData));
  }

  public static UpdateResponse createUpdateResponse() {
    var now = Instant.now();
    var bids = List.of(new Order(97, 1), new Order(95, 1));
    var asks = List.of(new Order(100, 1), new Order(102, 1));

    var updateData = new UpdateResponse.UpdateData(SYMBOL, bids, asks, 12345L, now.toString());
    return new UpdateResponse("book", "update", List.of(updateData));
  }
}
