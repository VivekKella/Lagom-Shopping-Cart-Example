package com.lagom.TCSShoppingCart.shoppingCart.impl;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.pcollections.PSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartEvent.CheckedOut;
import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartEvent.ItemAdded;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.ReadSideProcessor;
import com.lightbend.lagom.javadsl.persistence.jpa.JpaReadSide;

public class ShoppingCartProcessor extends ReadSideProcessor<ShoppingCartEvent> {

	private final JpaReadSide jpaReadSide;
	final private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Inject
	public ShoppingCartProcessor(JpaReadSide jpaReadSide) {
		this.jpaReadSide = jpaReadSide;
	}

	@Override
	public ReadSideHandler<ShoppingCartEvent> buildHandler() {
		return jpaReadSide.<ShoppingCartEvent>builder("shopping-cart-report").setGlobalPrepare(this::createSchema)
				.setEventHandler(ItemAdded.class, this::createReport)
				.setEventHandler(CheckedOut.class, this::addCheckoutTime).build();
	}

	@Override
	public PSequence<AggregateEventTag<ShoppingCartEvent>> aggregateTags() {
		return ShoppingCartEvent.TAG.allTags();
	}

	private void createSchema(@SuppressWarnings("unused") EntityManager ignored) {
		Persistence.generateSchema("default", ImmutableMap.of("hibernate.hbm2ddl.auto", "update"));
	}

	private void createReport(EntityManager entityManager, ItemAdded evt) {

		logger.debug("Received ItemUpdate event: " + evt);
		if (findReport(entityManager, evt.shoppingCartId) == null) {
			logger.debug("Creating report for CartID: " + evt.shoppingCartId);
			ShoppingCart report = new ShoppingCart();
			report.setId(evt.shoppingCartId);
			report.setCreationDate(evt.eventTime);
			entityManager.persist(report);
		}
	}

	private void addCheckoutTime(EntityManager entityManager, CheckedOut evt) {
		ShoppingCart report = findReport(entityManager, evt.shoppingCartId);

		logger.debug("Received CheckedOut event: " + evt);
		if (report != null) {
			logger.debug("Adding checkout time (" + evt.eventTime + ") for CartID: " + evt.shoppingCartId);
			report.setCheckoutDate(evt.eventTime);
			entityManager.persist(report);
		} else {
			throw new RuntimeException("Didn't find cart for checkout. CartID: " + evt.shoppingCartId);
		}
	}

	private ShoppingCart findReport(EntityManager entityManager, String cartId) {
		return entityManager.find(ShoppingCart.class, cartId);
	}
}
