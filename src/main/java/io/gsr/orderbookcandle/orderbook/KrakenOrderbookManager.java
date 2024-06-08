package io.gsr.orderbookcandle.orderbook;

import static io.gsr.orderbookcandle.config.Constants.ONE_MIN_MILLIS;
import static io.gsr.orderbookcandle.domain.Side.ASK;
import static io.gsr.orderbookcandle.domain.Side.BID;
import static java.util.stream.Collectors.toMap;

import io.gsr.orderbookcandle.domain.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KrakenOrderbookManager {
  @Value("${kraken.symbols}")
  List<String> symbols;

  private Map<String, Orderbook> orderbooks;
  private Map<String, MidPrices> symbolMidPriceMap;
  private Map<String, Double> recentSymbolMidPriceMap;
  private Map<String, Integer> symbolTickCountMap;
  private long startTimeMillis;
  private long currentTimeMillis;

  public KrakenOrderbookManager() {
    resetState();
  }

  public void createOrderbooksFromSnapshot(SnapshotResponse snapshot, long timeMillis) {
    snapshot
        .data()
        .forEach(
            data -> {
              var symbol = data.symbol();
              orderbooks.putIfAbsent(symbol, new Orderbook());
              data.bids().forEach(bid -> orderbooks.get(symbol).insertOrder(bid, BID));
              data.asks().forEach(ask -> orderbooks.get(symbol).insertOrder(ask, ASK));

              var highestBidPrice =
                  orderbooks
                      .get(symbol)
                      .getTopOfBookOrder(BID)
                      .orElseThrow(IllegalStateException::new)
                      .price();
              var lowestAskPrice =
                  orderbooks
                      .get(symbol)
                      .getTopOfBookOrder(ASK)
                      .orElseThrow(IllegalStateException::new)
                      .price();
              var openMidPrice = (highestBidPrice + lowestAskPrice) / 2;
              symbolMidPriceMap.putIfAbsent(symbol, new MidPrices());
              symbolMidPriceMap.get(symbol).setOpenMidPrice(openMidPrice);
              symbolMidPriceMap.get(symbol).setHighMidPrice(openMidPrice);
              symbolMidPriceMap.get(symbol).setLowMidPrice(openMidPrice);
              startTimeMillis = timeMillis;
              symbolTickCountMap.putIfAbsent(symbol, 1);
            });
  }

  public Map<String, Candle> appendToOrderbooksFromUpdate(UpdateResponse update) {
    var symbolCandleMap = new HashMap<String, Candle>();
    update
        .data()
        .forEach(
            data -> {
              var symbol = data.symbol();
              orderbooks.putIfAbsent(symbol, new Orderbook());
              symbolMidPriceMap.putIfAbsent(symbol, new MidPrices());
              currentTimeMillis = Instant.parse(data.timestamp()).toEpochMilli();
              data.bids().forEach(bid -> orderbooks.get(symbol).insertOrder(bid, BID));
              data.asks().forEach(ask -> orderbooks.get(symbol).insertOrder(ask, ASK));

              var highestBidPrice = orderbooks.get(symbol).getTopOfBookOrder(BID).map(Order::price);
              var lowestAskPrice = orderbooks.get(symbol).getTopOfBookOrder(ASK).map(Order::price);

              if (highestBidPrice.isPresent() && lowestAskPrice.isPresent()) {
                var currMidPrice = (highestBidPrice.get() + lowestAskPrice.get()) / 2;
                if (symbolMidPriceMap.get(symbol).getOpenMidPrice() == 0) {
                  symbolMidPriceMap.get(symbol).setOpenMidPrice(currMidPrice);
                }
                if (symbolMidPriceMap.get(symbol).getLowMidPrice() == 0) {
                  symbolMidPriceMap.get(symbol).setLowMidPrice(currMidPrice);
                }
                if (symbolMidPriceMap.get(symbol).getHighMidPrice() == 0) {
                  symbolMidPriceMap.get(symbol).setHighMidPrice(currMidPrice);
                }
                recentSymbolMidPriceMap.put(symbol, currMidPrice);
                var currHighMidPrice = symbolMidPriceMap.get(symbol).getHighMidPrice();
                var currLowMidPrice = symbolMidPriceMap.get(symbol).getLowMidPrice();
                symbolMidPriceMap
                    .get(symbol)
                    .setHighMidPrice(Math.max(currHighMidPrice, currMidPrice));
                symbolMidPriceMap
                    .get(symbol)
                    .setLowMidPrice(Math.min(currLowMidPrice, currMidPrice));
              }
              symbolTickCountMap.merge(symbol, 1, Integer::sum);
            });
    if (currentTimeMillis - startTimeMillis >= ONE_MIN_MILLIS) {
      symbols.forEach(
          symbol ->
              symbolMidPriceMap.get(symbol).setCloseMidPrice(recentSymbolMidPriceMap.get(symbol)));
      symbolCandleMap = createCandles();
      resetState();
      startTimeMillis = currentTimeMillis;
    }
    return symbolCandleMap;
  }

  public Map<String, Orderbook> getOrderbooks() {
    return orderbooks;
  }

  public Map<String, MidPrices> getMidPrices() {
    return symbolMidPriceMap;
  }

  public Map<String, Integer> getTickCount() {
    return symbolTickCountMap;
  }

  private HashMap<String, Candle> createCandles() {
    var rawMap =
        symbols.stream()
            .map(
                symbol ->
                    new Candle(
                        symbol,
                        startTimeMillis,
                        symbolMidPriceMap.get(symbol).getOpenMidPrice(),
                        symbolMidPriceMap.get(symbol).getCloseMidPrice(),
                        symbolMidPriceMap.get(symbol).getLowMidPrice(),
                        symbolMidPriceMap.get(symbol).getHighMidPrice(),
                        symbolTickCountMap.get(symbol)))
            .collect(toMap(Candle::symbol, c -> c));
    return new HashMap<>(rawMap);
  }

  private void resetState() {
    orderbooks = new HashMap<>();
    symbolMidPriceMap = new HashMap<>();
    recentSymbolMidPriceMap = new HashMap<>();
    symbolTickCountMap = new HashMap<>();
  }
}
