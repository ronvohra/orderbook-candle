package io.gsr.orderbookcandle.domain;

import java.util.List;

public record SnapshotResponse(String channel, String type, List<SnapshotData> data) {
  public record SnapshotData(String symbol, List<Order> bids, List<Order> asks, long checksum) {}
}
