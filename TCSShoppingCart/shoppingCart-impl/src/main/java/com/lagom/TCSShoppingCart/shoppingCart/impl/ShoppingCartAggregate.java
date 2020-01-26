package com.lagom.TCSShoppingCart.shoppingCart.impl;

import java.time.Instant;
import java.util.Set;

import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartCommand.Accepted;
import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartCommand.AddItem;
import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartCommand.AdjustItemQuantity;
import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartCommand.Checkout;
import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartCommand.Get;
import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartCommand.Hello;
import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartCommand.Rejected;
import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartCommand.RemoveItem;
import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartCommand.UseGreetingMessage;
import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartEvent.CheckedOut;
import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartEvent.GreetingMessageChanged;
import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartEvent.ItemAdded;
import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartEvent.ItemQuantityAdjusted;
import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartEvent.ItemRemoved;
import com.lightbend.lagom.javadsl.persistence.AkkaTaggerAdapter;

import akka.cluster.sharding.typed.javadsl.EntityContext;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.javadsl.CommandHandlerWithReply;
import akka.persistence.typed.javadsl.CommandHandlerWithReplyBuilder;
import akka.persistence.typed.javadsl.EventHandler;
import akka.persistence.typed.javadsl.EventSourcedBehaviorWithEnforcedReplies;
import akka.persistence.typed.javadsl.ReplyEffect;
import akka.persistence.typed.javadsl.RetentionCriteria;

/**
 * This is an event sourced aggregate. It has a state,
 * {@link ShoppingCartState}, which stores what the greeting should be (eg,
 * "Hello").
 * <p>
 * Event sourced aggregate are interacted with by sending them commands. This
 * aggregate supports two commands, a {@link UseGreetingMessage} command, which
 * is used to change the greeting, and a {@link Hello} command, which is a read
 * only command which returns a greeting to the name specified by the command.
 * <p>
 * Commands may emit events, and it's the events that get persisted. Each event
 * will have an event handler registered for it, and an event handler simply
 * applies an event to the current state. This will be done when the event is
 * first created, and it will also be done when the entity is loaded from the
 * database - each event will be replayed to recreate the state of the
 * aggregate.
 * <p>
 * This aggregate defines one event, the {@link GreetingMessageChanged} event,
 * which is emitted when a {@link UseGreetingMessage} command is received.
 */
public class ShoppingCartAggregate
		extends EventSourcedBehaviorWithEnforcedReplies<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState> {

	public static EntityTypeKey<ShoppingCartCommand> ENTITY_TYPE_KEY = EntityTypeKey.create(ShoppingCartCommand.class,
			"ShoppingCartAggregate");

	final private EntityContext<ShoppingCartCommand> entityContext;

	final private String shoppingCartId;

	ShoppingCartAggregate(EntityContext<ShoppingCartCommand> entityContext) {
		super(PersistenceId.of(entityContext.getEntityTypeKey().name(), entityContext.getEntityId()));
		this.entityContext = entityContext;
		this.shoppingCartId = entityContext.getEntityId();
	}

	public static ShoppingCartAggregate create(EntityContext<ShoppingCartCommand> entityContext) {
		return new ShoppingCartAggregate(entityContext);
	}

	@Override
	public ShoppingCartState emptyState() {
		return ShoppingCartState.EMPTY;
	}

	@Override
	public RetentionCriteria retentionCriteria() {
		return RetentionCriteria.snapshotEvery(100, 2);
	}

	@Override
	public CommandHandlerWithReply<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState> commandHandler() {

		CommandHandlerWithReplyBuilder<ShoppingCartCommand, ShoppingCartEvent, ShoppingCartState> builder = newCommandHandlerWithReplyBuilder();

		builder.forState(ShoppingCartState::isOpen).onCommand(AddItem.class, this::onAddItem)
				.onCommand(RemoveItem.class, this::onRemoveItem)
				.onCommand(AdjustItemQuantity.class, this::onAdjustItemQuantity)
				.onCommand(Checkout.class, this::onCheckout);

		builder.forState(ShoppingCartState::isCheckedOut)
				.onCommand(AddItem.class,
						cmd -> Effect().reply(cmd.replyTo, new Rejected("Cannot add an item to a checked-out cart")))
				.onCommand(RemoveItem.class,
						cmd -> Effect().reply(cmd.replyTo, new Rejected("Cannot remove an item to a checked-out cart")))
				.onCommand(AdjustItemQuantity.class,
						cmd -> Effect().reply(cmd.replyTo,
								new Rejected("Cannot adjust item quantity in a checked-out cart")))
				.onCommand(Checkout.class,
						cmd -> Effect().reply(cmd.replyTo, new Rejected("Cannot checkout a checked-out cart")));

		builder.forAnyState().onCommand(Get.class, this::onGet);

		return builder.build();

	}

	@Override
	public EventHandler<ShoppingCartState, ShoppingCartEvent> eventHandler() {
		return newEventHandlerBuilder().forAnyState()
				.onEvent(ItemAdded.class, (shoppingCart, evt) -> shoppingCart.updateItem(evt.itemId, evt.quantity))
				.onEvent(ItemRemoved.class, (shoppingCart, evt) -> shoppingCart.removeItem(evt.itemId))
				.onEvent(ItemQuantityAdjusted.class,
						(shoppingCart, evt) -> shoppingCart.updateItem(evt.itemId, evt.quantity))
				.onEvent(CheckedOut.class, (shoppingCart, evt) -> shoppingCart.checkout(evt.eventTime)).build();
	}

	@Override
	public Set<String> tagsFor(ShoppingCartEvent shoppingCartEvent) {
		return AkkaTaggerAdapter.fromLagom(entityContext, ShoppingCartEvent.TAG).apply(shoppingCartEvent);
	}

	private ReplyEffect<ShoppingCartEvent, ShoppingCartState> onAddItem(ShoppingCartState shoppingCart, AddItem cmd) {
		if (shoppingCart.hasItem(cmd.itemId)) {
			return Effect().reply(cmd.replyTo, new Rejected("Item was already added to this shopping cart"));
		} else if (cmd.quantity <= 0) {
			return Effect().reply(cmd.replyTo, new Rejected("Quantity must be greater than zero"));
		} else {
			return Effect()
					.persist(new ShoppingCartEvent.ItemAdded(shoppingCartId, cmd.itemId, cmd.quantity, Instant.now()))
					.thenReply(cmd.replyTo, s -> new Accepted(ShoppingCartState.toSummary(s)));
		}
	}

	private ReplyEffect<ShoppingCartEvent, ShoppingCartState> onRemoveItem(ShoppingCartState shoppingCart,
			RemoveItem cmd) {
		if (shoppingCart.hasItem(cmd.itemId)) {
			return Effect().persist(new ShoppingCartEvent.ItemRemoved(shoppingCartId, cmd.itemId, Instant.now()))
					.thenReply(cmd.replyTo,
							updatedShoppingCart -> new Accepted(ShoppingCartState.toSummary(updatedShoppingCart)));
		} else {
			// Remove is idempotent, so we can just return the summary here
			return Effect().reply(cmd.replyTo, new Accepted(ShoppingCartState.toSummary(shoppingCart)));
		}
	}

	private ReplyEffect<ShoppingCartEvent, ShoppingCartState> onAdjustItemQuantity(ShoppingCartState shoppingCart,
			AdjustItemQuantity cmd) {
		if (cmd.quantity <= 0) {
			return Effect().reply(cmd.replyTo, new Rejected("Quantity must be greater than zero"));
		} else if (shoppingCart.hasItem(cmd.itemId)) {
			return Effect().persist(
					new ShoppingCartEvent.ItemQuantityAdjusted(shoppingCartId, cmd.itemId, cmd.quantity, Instant.now()))
					.thenReply(cmd.replyTo, s -> new Accepted(ShoppingCartState.toSummary(s)));
		} else {
			return Effect().reply(cmd.replyTo, new Rejected("Item not found in shopping cart"));
		}
	}

	private ReplyEffect<ShoppingCartEvent, ShoppingCartState> onGet(ShoppingCartState shoppingCart, Get cmd) {
		return Effect().reply(cmd.replyTo, ShoppingCartState.toSummary(shoppingCart));
	}

	private ReplyEffect<ShoppingCartEvent, ShoppingCartState> onCheckout(ShoppingCartState shoppingCart, Checkout cmd) {
		if (shoppingCart.isEmpty()) {
			return Effect().reply(cmd.replyTo, new Rejected("Cannot checkout empty shopping cart"));
		} else {
			return Effect().persist(new CheckedOut(shoppingCartId, Instant.now())).thenReply(cmd.replyTo,
					s -> new Accepted(ShoppingCartState.toSummary(s)));
		}
	}

}
