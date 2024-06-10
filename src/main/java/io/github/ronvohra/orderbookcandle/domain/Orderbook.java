package io.github.ronvohra.orderbookcandle.domain;

import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparing;

import java.util.*;

public class Orderbook {
  private final Map<Side, PriorityQueue<Order>> books;

  public Orderbook() {
    books = new EnumMap<>(Side.class);
    books.put(Side.BID, new PriorityQueue<>(reverseOrder(comparing(Order::price))));
    books.put(Side.ASK, new PriorityQueue<>(comparing(Order::price)));
  }

  public void insertOrder(Order order, Side side) {
    if (Objects.requireNonNull(side) == Side.BID) {
      books.get(Side.BID).add(order);
    } else if (side == Side.ASK) {
      books.get(Side.ASK).add(order);
    }
  }

  public Optional<Order> getTopOfBookOrder(Side side) {
    return switch (side) {
      case BID -> Optional.ofNullable(books.get(Side.BID).peek());
      case ASK -> Optional.ofNullable(books.get(Side.ASK).peek());
    };
  }

  public PriorityQueue<Order> getBookBySide(Side side) {
    return books.get(side);
  }
}
