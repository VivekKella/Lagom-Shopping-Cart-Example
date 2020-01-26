package com.lagom.TCSShoppingCart.shoppingCart.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Value;

@Value
@JsonDeserialize
public final class Quantity {

	public final int quantity;

	@JsonCreator
	public Quantity(int quantity) {
		this.quantity = quantity;
	}
}
