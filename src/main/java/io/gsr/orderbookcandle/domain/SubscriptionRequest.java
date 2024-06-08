package io.gsr.orderbookcandle.domain;

import static io.gsr.orderbookcandle.config.Constants.FEED_DEFAULT_DEPTH;

import java.util.List;
import java.util.Set;

public record SubscriptionRequest(String method, Params params) {
  private static final Set<Integer> ALLOWED_DEPTH_VALUES = Set.of(10, 25, 100, 500, 1000);

  public record Params(String channel, List<String> symbol, int depth) {
    public Params {
      if (!ALLOWED_DEPTH_VALUES.contains(depth)) {
        throw new IllegalArgumentException("Invalid depth: " + depth);
      }
    }

    public Params(String channel, List<String> symbol) {
      this(channel, symbol, FEED_DEFAULT_DEPTH);
    }
  }
}
