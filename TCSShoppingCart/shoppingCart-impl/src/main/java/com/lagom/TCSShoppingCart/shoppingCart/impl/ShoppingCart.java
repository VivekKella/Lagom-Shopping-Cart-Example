package com.lagom.TCSShoppingCart.shoppingCart.impl;

import java.time.Instant;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.sun.istack.NotNull;

@Entity
public class ShoppingCart {

	@Id
	private String id;

	/**
	 * The shopping cart creation date
	 */
	@NotNull
	private Instant creationDate;

	private Instant checkoutDate;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@NotNull
	public Instant getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(@NotNull Instant creationDate) {
		this.creationDate = creationDate;
	}

	public Instant getCheckoutDate() {
		return checkoutDate;
	}

	public void setCheckoutDate(Instant checkoutDate) {
		this.checkoutDate = checkoutDate;
	}

}
