package com.lagom.TCSShoppingCart.stream.impl;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lagom.TCSShoppingCart.shoppingCart.api.ShoppingCartService;
import com.lagom.TCSShoppingCart.stream.api.StreamService;

import javax.inject.Inject;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Implementation of the HelloString.
 */
public class StreamServiceImpl implements StreamService {

  private final ShoppingCartService shoppingCartService;
  private final StreamRepository repository;

  @Inject
  public StreamServiceImpl(ShoppingCartService shoppingCartService, StreamRepository repository) {
    this.shoppingCartService = shoppingCartService;
    this.repository = repository;
  }

@Override
public ServiceCall<Source<String, NotUsed>, Source<String, NotUsed>> directStream() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public ServiceCall<Source<String, NotUsed>, Source<String, NotUsed>> autonomousStream() {
	// TODO Auto-generated method stub
	return null;
}

  
}
