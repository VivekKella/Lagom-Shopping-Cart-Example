package com.lagom.TCSShoppingCart.shoppingCart.impl;

import java.time.Instant;
import java.util.Optional;

import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartCommand.Summary;
import com.lightbend.lagom.serialization.CompressedJsonable;

import lombok.Value;

/**
 * The state for the {@link ShoppingCartState} aggregate.
 */
@SuppressWarnings("serial")
@Value
@JsonDeserialize
public final class ShoppingCartState implements CompressedJsonable {

	public static final ShoppingCartState EMPTY = new ShoppingCartState(HashTreePMap.empty(), null);

	public final PMap<String, Integer> items;
	public final Optional<Instant> checkoutDate;

	@JsonCreator
	ShoppingCartState(PMap<String, Integer> items, Instant checkoutDate) {
		this.items = Preconditions.checkNotNull(items, "items");
		this.checkoutDate = Optional.ofNullable(checkoutDate);
	}

	ShoppingCartState removeItem(String itemId) {
		PMap<String, Integer> newItems = items.minus(itemId);
		return new ShoppingCartState(newItems, null);
	}

	ShoppingCartState updateItem(String itemId, int quantity) {
		PMap<String, Integer> newItems = items.plus(itemId, quantity);
		return new ShoppingCartState(newItems, null);
	}

	boolean isEmpty() {
		return items.isEmpty();
	}

	boolean hasItem(String itemId) {
		return items.containsKey(itemId);
	}

	ShoppingCartState checkout(Instant when) {
		return new ShoppingCartState(items, when);
	}

	boolean isOpen() {
		return !this.isCheckedOut();
	}

	boolean isCheckedOut() {
		return this.checkoutDate.isPresent();
	}

	public static Summary toSummary(ShoppingCartState shoppingCart) {
		return new Summary(shoppingCart.items, shoppingCart.isCheckedOut(), shoppingCart.checkoutDate);
	}

//	public ShoppingCartState withMessage(String message) {
//		return new ShoppingCartState(message, LocalDateTime.now().toString());
//	}
}
