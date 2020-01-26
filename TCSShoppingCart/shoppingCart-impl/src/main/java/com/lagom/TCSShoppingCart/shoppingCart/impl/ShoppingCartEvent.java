package com.lagom.TCSShoppingCart.shoppingCart.impl;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.javadsl.persistence.AggregateEventShards;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTagger;
import com.lightbend.lagom.serialization.Jsonable;

import lombok.Value;

/**
 * This interface defines all the events that the ShoppingCart aggregate
 * supports.
 * <p>
 * By convention, the events should be inner classes of the interface, which
 * makes it simple to get a complete picture of what events an entity has.
 */
public interface ShoppingCartEvent extends Jsonable, AggregateEvent<ShoppingCartEvent> {

	/**
	 * Tags are used for getting and publishing streams of events. Each event will
	 * have this tag, and in this case, we are partitioning the tags into 4 shards,
	 * which means we can have 4 concurrent processors/publishers of events.
	 */
	AggregateEventShards<ShoppingCartEvent> TAG = AggregateEventTag.sharded(ShoppingCartEvent.class, 4);

	/**
	 * An event that represents a change in greeting message.
	 */
	@SuppressWarnings("serial")
	@Value
	@JsonDeserialize
	public final class GreetingMessageChanged implements ShoppingCartEvent, Jsonable {

		public final String name;
		public final String message;

		@JsonCreator
		public GreetingMessageChanged(String name, String message) {
			this.name = Preconditions.checkNotNull(name, "name");
			this.message = Preconditions.checkNotNull(message, "message");
		}

		public String getName() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getMessage() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	@SuppressWarnings("serial")
	@Value
	@JsonDeserialize
	public final class ItemAdded implements ShoppingCartEvent, Jsonable {
		public final String shoppingCartId;
		public final String itemId;
		public final int quantity;
		public final Instant eventTime;

		@JsonCreator
		ItemAdded(String shoppingCartId, String itemId, int quantity, Instant eventTime) {
			this.shoppingCartId = Preconditions.checkNotNull(shoppingCartId, "shoppingCartId");
			this.itemId = Preconditions.checkNotNull(itemId, "itemId");
			this.quantity = quantity;
			this.eventTime = eventTime;
		}
	}

	@SuppressWarnings("serial")
	@Value
	@JsonDeserialize
	public final class ItemRemoved implements ShoppingCartEvent, Jsonable {
		public final String shoppingCartId;
		public final String itemId;
		public final Instant eventTime;

		@JsonCreator
		ItemRemoved(String shoppingCartId, String itemId, Instant eventTime) {
			this.shoppingCartId = Preconditions.checkNotNull(shoppingCartId, "shoppingCartId");
			this.itemId = Preconditions.checkNotNull(itemId, "itemId");
			this.eventTime = eventTime;
		}
	}

	@SuppressWarnings("serial")
	@Value
	@JsonDeserialize
	public final class ItemQuantityAdjusted implements ShoppingCartEvent, Jsonable {
		public final String shoppingCartId;
		public final String itemId;
		public final int quantity;
		public final Instant eventTime;

		@JsonCreator
		ItemQuantityAdjusted(String shoppingCartId, String itemId, int quantity, Instant eventTime) {
			this.shoppingCartId = Preconditions.checkNotNull(shoppingCartId, "shoppingCartId");
			this.itemId = Preconditions.checkNotNull(itemId, "itemId");
			this.quantity = quantity;
			this.eventTime = eventTime;
		}
	}

	@SuppressWarnings("serial")
	@Value
	@JsonDeserialize
	public final class CheckedOut implements ShoppingCartEvent, Jsonable {

		public final String shoppingCartId;
		public final Instant eventTime;

		@JsonCreator
		CheckedOut(String shoppingCartId, Instant eventTime) {
			this.shoppingCartId = Preconditions.checkNotNull(shoppingCartId, "shoppingCartId");
			this.eventTime = eventTime;
		}
	}

	@Override
	default AggregateEventTagger<ShoppingCartEvent> aggregateTag() {
		return TAG;
	}

}
