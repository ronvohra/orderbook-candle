package io.gsr.orderbookcandle.domain;

import static io.gsr.orderbookcandle.domain.Side.ASK;
import static io.gsr.orderbookcandle.domain.Side.BID;
import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparing;

import java.util.*;

public class Orderbook {
  private final Map<Side, PriorityQueue<Order>> books;

  public Orderbook() {
    books = new EnumMap<>(Side.class);
    books.put(BID, new PriorityQueue<>(reverseOrder(comparing(Order::price))));
    books.put(ASK, new PriorityQueue<>(comparing(Order::price)));
  }

  public void insertOrder(Order order, Side side) {
    if (Objects.requireNonNull(side) == BID) {
      books.get(BID).add(order);
    } else if (side == ASK) {
      books.get(ASK).add(order);
    }
  }

  public Optional<Order> getTopOfBookOrder(Side side) {
    return switch (side) {
      case BID -> Optional.ofNullable(books.get(BID).peek());
      case ASK -> Optional.ofNullable(books.get(ASK).peek());
    };
  }

  public PriorityQueue<Order> getBookBySide(Side side) {
    return books.get(side);
  }
}
