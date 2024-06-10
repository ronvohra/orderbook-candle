package io.github.ronvohra.orderbookcandle.domain;

public record Candle(
    String symbol,
    long timeEpochMillis,
    double openMidPrice,
    double closeMidPrice,
    double lowestMidPrice,
    double highestMidPrice,
    int tickCount) {}
