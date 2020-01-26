package com.lagom.TCSShoppingCart.shoppingCart.api;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Value;
@Value
@JsonDeserialize
public final class ShoppingCartReportView {

	/**
	 * The ID of the shopping cart.
	 */
	public final String id;

	/**
	 * The shopping cart creation date
	 */
	public final Instant creationDate;

	public final Instant checkoutDate;

	@JsonCreator
	public ShoppingCartReportView(String id, Instant creationDate, Instant checkoutDate) {
		this.id = id;
		this.creationDate = creationDate;
		this.checkoutDate = checkoutDate;
	}
}
