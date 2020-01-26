package com.lagom.TCSShoppingCart.shoppingCart.impl;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.lightbend.lagom.javadsl.persistence.ReadSide;
import com.lightbend.lagom.javadsl.persistence.jpa.JpaSession;

@Singleton
public class ShoppingCartRepository {

	private final JpaSession jpaSession;

	@Inject
	public ShoppingCartRepository(ReadSide readSide, JpaSession jpaSession) {
		this.jpaSession = jpaSession;
		readSide.register(ShoppingCartProcessor.class);
	}

	CompletionStage<ShoppingCart> findById(String cartId) {
		return jpaSession.withTransaction(em -> em.find(ShoppingCart.class, cartId));
	}
}
