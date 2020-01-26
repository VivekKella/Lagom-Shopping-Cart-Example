package com.lagom.TCSShoppingCart.shoppingCart.impl;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.lightbend.lagom.serialization.CompressedJsonable;
import com.lightbend.lagom.serialization.Jsonable;

import akka.actor.typed.ActorRef;
import lombok.Value;

/**
 * This interface defines all the commands that the ShoppingCart aggregate
 * supports.
 * <p>
 * By convention, the commands and replies should be inner classes of the
 * interface, which makes it simple to get a complete picture of what commands
 * an aggregate supports.
 */
public interface ShoppingCartCommand extends Jsonable {

	/**
	 * A command to switch the greeting message.
	 * <p>
	 * It has a reply type of {@link Confirmation}, which is sent back to the caller
	 * when all the events emitted by this command are successfully persisted.
	 */
	@SuppressWarnings("serial")
	@Value
	@JsonDeserialize
	final class UseGreetingMessage implements ShoppingCartCommand, CompressedJsonable {
		public final String message;
		public final ActorRef<Confirmation> replyTo;

		@JsonCreator
		UseGreetingMessage(String message, ActorRef<Confirmation> replyTo) {
			this.message = Preconditions.checkNotNull(message, "message");
			this.replyTo = replyTo;
		}
	}

	/**
	 * A command to say hello to someone using the current greeting message.
	 * <p>
	 * The reply type is {@link Greeting} and will contain the message to say to
	 * that person.
	 */
	@SuppressWarnings("serial")
	@Value
	@JsonDeserialize
	final class Hello implements ShoppingCartCommand {
		public final String name;
		public final ActorRef<Greeting> replyTo;

		@JsonCreator
		Hello(String name, ActorRef<Greeting> replyTo) {
			this.name = Preconditions.checkNotNull(name, "name");
			this.replyTo = replyTo;
		}
	}

	@SuppressWarnings("serial")
	@Value
	@JsonDeserialize
	final class AddItem implements ShoppingCartCommand, Jsonable {
		public final String itemId;
		public final int quantity;
		public final ActorRef<Confirmation> replyTo;

		@JsonCreator
		AddItem(String itemId, int quantity, ActorRef<Confirmation> replyTo) {
			this.itemId = Preconditions.checkNotNull(itemId, "itemId");
			this.quantity = quantity;
			this.replyTo = replyTo;
		}
	}

	@SuppressWarnings("serial")
	@Value
	@JsonDeserialize
	final class RemoveItem implements ShoppingCartCommand, Jsonable {
		public final String itemId;
		public final ActorRef<Confirmation> replyTo;

		@JsonCreator
		RemoveItem(String itemId, ActorRef<Confirmation> replyTo) {
			this.itemId = Preconditions.checkNotNull(itemId, "itemId");
			this.replyTo = replyTo;
		}
	}

	@SuppressWarnings("serial")
	@Value
	@JsonDeserialize
	final class AdjustItemQuantity implements ShoppingCartCommand, Jsonable {
		public final String itemId;
		public final int quantity;
		public final ActorRef<Confirmation> replyTo;

		@JsonCreator
		AdjustItemQuantity(String itemId, int quantity, ActorRef<Confirmation> replyTo) {
			this.itemId = Preconditions.checkNotNull(itemId, "itemId");
			this.quantity = quantity;
			this.replyTo = replyTo;
		}
	}

	@Value
	@JsonDeserialize
	@SuppressWarnings("serial")
	final class Get implements ShoppingCartCommand, Jsonable {
		public final ActorRef<Summary> replyTo;

		@JsonCreator
		Get(ActorRef<Summary> replyTo) {
			this.replyTo = replyTo;
		}
	}

	@Value
	@JsonDeserialize
	@SuppressWarnings("serial")
	final class Checkout implements ShoppingCartCommand, Jsonable {
		public final ActorRef<Confirmation> replyTo;

		@JsonCreator
		Checkout(ActorRef<Confirmation> replyTo) {
			this.replyTo = replyTo;
		}
	}

	// The commands above will use different reply types (see below all the reply
	// types).

	/**
	 * Super interface for Accepted/Rejected replies used by UseGreetingMessage
	 */

	interface Reply {
	}

	interface Confirmation extends Reply {
	}

	@Value
	@JsonDeserialize
	public final class Summary implements Reply {

		public final Map<String, Integer> items;
		public final boolean checkedOut;
		public final Optional<Instant> checkoutDate;

		@JsonCreator
		Summary(Map<String, Integer> items, boolean checkedOut, Optional<Instant> checkoutDate) {
			this.items = items;
			this.checkedOut = checkedOut;
			this.checkoutDate = checkoutDate;
		}
	}

	@Value
	@JsonDeserialize
	public final class Accepted implements Confirmation {
		public final Summary summary;

		@JsonCreator
		Accepted(Summary summary) {
			this.summary = summary;
		}
	}

	@Value
	@JsonDeserialize
	public final class Rejected implements Confirmation {
		public final String reason;

		@JsonCreator
		Rejected(String reason) {
			this.reason = reason;
		}
	}

	/**
	 * Reply type for a Hello command.
	 */
	@Value
	@JsonDeserialize
	final class Greeting {
		public final String message;

		public Greeting(String message) {
			this.message = message;
		}
	}

}
