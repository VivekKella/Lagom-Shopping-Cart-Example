package com.lagom.TCSShoppingCart.stream.impl;

import akka.Done;
import akka.stream.javadsl.Flow;
import com.lagom.TCSShoppingCart.shoppingCart.api.ShoppingCartEvent;
import com.lagom.TCSShoppingCart.shoppingCart.api.ShoppingCartService;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;

/**
 * This subscribes to the ShoppingCartService event stream.
 */
public class StreamSubscriber {

  @Inject
  public StreamSubscriber(ShoppingCartService shoppingCartService, StreamRepository repository) {
    // Create a subscriber
    shoppingCartService.helloEvents().subscribe()
      // And subscribe to it with at least once processing semantics.
      .atLeastOnce(
        // Create a flow that emits a Done for each message it processes
        Flow.<ShoppingCartEvent>create().mapAsync(1, event -> {

          if (event instanceof ShoppingCartEvent.GreetingMessageChanged) {
            ShoppingCartEvent.GreetingMessageChanged messageChanged = (ShoppingCartEvent.GreetingMessageChanged) event;
            // Update the message
            return repository.updateMessage(messageChanged.getName(), messageChanged.getMessage());

          } else {
            // Ignore all other events
            return CompletableFuture.completedFuture(Done.getInstance());
          }
        })
      );

  }
}
