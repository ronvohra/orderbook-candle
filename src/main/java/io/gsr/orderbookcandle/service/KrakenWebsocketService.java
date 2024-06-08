package io.gsr.orderbookcandle.service;

import io.gsr.orderbookcandle.handler.KrakenWebsocketHandler;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Service
public class KrakenWebsocketService {
  @Value("${kraken.websocket.uri}")
  private String krakenWebsocketUri;

  @Autowired private KrakenWebsocketHandler handler;

  @EventListener(ApplicationReadyEvent.class)
  public void openOrderbookSocket() {
    var connectionManager =
        new WebSocketConnectionManager(
            new StandardWebSocketClient(), handler, URI.create(krakenWebsocketUri));

    connectionManager.start();
  }
}
