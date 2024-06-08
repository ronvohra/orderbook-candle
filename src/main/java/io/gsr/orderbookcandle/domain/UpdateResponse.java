package io.gsr.orderbookcandle.domain;

import java.util.List;

public record UpdateResponse(String channel, String type, List<UpdateData> data) {
  public record UpdateData(
      String symbol, List<Order> bids, List<Order> asks, long checksum, String timestamp) {}
}
