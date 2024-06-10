package io.github.ronvohra.orderbookcandle.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SubscriptionResponse(
    String method,
    Result result,
    boolean success,
    @JsonProperty("time_in") String timeIn,
    @JsonProperty("time_out") String timeOut) {
  record Result(String channel, int depth, boolean snapshot, String symbol) {}
}
