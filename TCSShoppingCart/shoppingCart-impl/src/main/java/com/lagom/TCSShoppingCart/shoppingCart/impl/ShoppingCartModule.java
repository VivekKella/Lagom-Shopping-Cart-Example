package com.lagom.TCSShoppingCart.shoppingCart.impl;

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import com.lagom.TCSShoppingCart.shoppingCart.api.ShoppingCartService;

/**
 * The module that binds the ShoppingCartService so that it can be served.
 */
public class ShoppingCartModule extends AbstractModule implements ServiceGuiceSupport {
  @Override
  protected void configure() {
    bindService(ShoppingCartService.class, ShoppingCartServiceImpl.class);
  }
}
