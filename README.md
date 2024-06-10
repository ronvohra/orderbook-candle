# 1m Orderbook Candle

## Overview

This is a project that generates a one-minute 'candle' for an orderbook, where the tick-level orderbook itself is 
generated via the Kraken API. We connect to the API v2 (L2 => Book API) via a websocket client and aggregate the 
orderbook every minute. 

We do so by creating the orderbook for a symbol with the first snapshot message received upon subscription, and we 
append to it with every subsequent update message. As soon as a minute is completed, we generate a candle for that 
minute (with high, low, open, and close mid-prices, along with the symbol and tick count for that minute) and log it
to stdout.

## Usage and implementation details 

The project is created with Java 17 using Spring Boot, and demonstrates the use of Kafka to publish messages (candle)
and consume them (logging to stdout). The project also works for multiple symbols at once - these can be supplied via
the `application.properties` file in `src/main/resources`. It is also possible to change the window to generate the 
candle (default is 1 min) and the market depth of the orderbook feed subscription (default is 10) from `Constants.java`.

There are a few unit tests which can be run with `mvn clean test`.

We're using Docker Compose to supply a local instance of Kafka - it plays nicely with this version of Spring Boot and
therefore it can just be run directly through your IDE (I use IntelliJ). Otherwise:

```bash
mvn clean install  # or use the bundled fat JAR in the repo
docker compose up  # then open a new terminal window for the next command
java -jar java -jar orderbook-candle-0.0.1-SNAPSHOT.jar
```

## Potential improvements

- Prevent Kafka from starting in unit tests
- There are no integration tests, nor do I have 100% coverage. I've tried to unit test the core logic, but one can never have too many tests!
- The code has been written keeping the Kraken WS API in mind - there are no generalised interfaces here yet. My reasoning for this was that there's no point in generalising without context.

### A note on backpressure

- While the feed has been tested to run for quite a few minutes, I wasn't able to simulate a long running task and didn't spend too long on playing with high market depth. As a result, I don't know how much backpressure it's capable of handling.
- As a corollary to the above, there are no mechanisms in place to handle backpressure - no increased buffer for instance.
- I looked at reactive sockets as a potential means of handling high throughput, but since I have zero experience I thought this might not be the best time to try them.
- Because I didn't do the above, I didn't see a lot of value in ensuring there are thread or concurrency safe data structures.
