package com.lagom.TCSShoppingCart.shoppingCart.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.lagom.TCSShoppingCart.shoppingCart.api.Quantity;
import com.lagom.TCSShoppingCart.shoppingCart.api.ShoppingCartItem;
import com.lagom.TCSShoppingCart.shoppingCart.api.ShoppingCartReportView;
import com.lagom.TCSShoppingCart.shoppingCart.api.ShoppingCartService;
import com.lagom.TCSShoppingCart.shoppingCart.api.ShoppingCartView;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.transport.BadRequest;
import com.lightbend.lagom.javadsl.api.transport.NotFound;
import com.lightbend.lagom.javadsl.broker.TopicProducer;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;

import akka.Done;
import akka.NotUsed;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.japi.Pair;

/**
 * Implementation of the ShoppingCartService.
 */
public class ShoppingCartServiceImpl implements ShoppingCartService {

	private final PersistentEntityRegistry persistentEntityRegistry;

	private final Duration askTimeout = Duration.ofSeconds(5);
	private ClusterSharding clusterSharding;

	private ShoppingCartRepository repository;

	@Inject
	public ShoppingCartServiceImpl(PersistentEntityRegistry persistentEntityRegistry, ClusterSharding clusterSharding,
			ShoppingCartRepository repository) {
		this.clusterSharding = clusterSharding;
		// The persistent entity registry is only required to build an event stream for
		// the TopicProducer
		this.persistentEntityRegistry = persistentEntityRegistry;

		// register the Aggregate as a sharded entity
		this.clusterSharding.init(Entity.of(ShoppingCartAggregate.ENTITY_TYPE_KEY, ShoppingCartAggregate::create));

		this.repository = repository;
	}

	@Override
	public ServiceCall<NotUsed, ShoppingCartView> get(String id) {
		return request -> entityRef(id).ask(ShoppingCartCommand.Get::new, askTimeout)
				.thenApply(summary -> asShoppingCartView(id, summary));
	}

	@Override
	public ServiceCall<NotUsed, ShoppingCartReportView> getReport(String id) {
		return request -> repository.findById(id).thenApply(report -> {
			if (report != null)
				return new ShoppingCartReportView(id, report.getCreationDate(), report.getCheckoutDate());
			else
				throw new NotFound("Couldn't find a shopping cart report for '" + id + "'");
		});
	}

	@Override
	public ServiceCall<ShoppingCartItem, Done> addItem(String cartId) {
		return item -> entityRef(cartId).<ShoppingCartCommand.Confirmation>ask(
				replyTo -> new ShoppingCartCommand.AddItem(item.itemId, item.quantity, replyTo), askTimeout)
				.thenApply(this::handleConfirmation).thenApply(accepted -> Done.getInstance());
	}

	@Override
	public ServiceCall<NotUsed, ShoppingCartView> removeItem(String cartId, String itemId) {
		return request -> entityRef(cartId).<ShoppingCartCommand.Confirmation>ask(
				replyTo -> new ShoppingCartCommand.RemoveItem(itemId, replyTo), askTimeout)
				.thenApply(this::handleConfirmation)
				.thenApply(accepted -> asShoppingCartView(cartId, accepted.summary));
	}

	@Override
	public ServiceCall<Quantity, ShoppingCartView> adjustItemQuantity(String cartId, String itemId) {
		return quantity -> entityRef(cartId).<ShoppingCartCommand.Confirmation>ask(
				replyTo -> new ShoppingCartCommand.AdjustItemQuantity(itemId, quantity.quantity, replyTo), askTimeout)
				.thenApply(this::handleConfirmation)
				.thenApply(accepted -> asShoppingCartView(cartId, accepted.summary));
	}

	@Override
	public ServiceCall<NotUsed, Done> checkout(String cartId) {
		return request -> entityRef(cartId).ask(ShoppingCartCommand.Checkout::new, askTimeout)
				.thenApply(this::handleConfirmation).thenApply(accepted -> Done.getInstance());
	}

	@Override
	public Topic<ShoppingCartView> shoppingCartTopic() {
		// We want to publish all the shards of the shopping cart events
		return TopicProducer.taggedStreamWithOffset(ShoppingCartEvent.TAG.allTags(), (tag, offset) ->
		// Load the event stream for the passed in shard tag
		persistentEntityRegistry.eventStream(tag, offset)
				// We only want to publish checkout events
				.filter(pair -> pair.first() instanceof ShoppingCartEvent.CheckedOut)
				// Now we want to convert from the persisted event to the published event.
				// To do this, we need to load the current shopping cart state.
				.mapAsync(4, eventAndOffset -> {
					ShoppingCartEvent.CheckedOut checkedOut = (ShoppingCartEvent.CheckedOut) eventAndOffset.first();
					return entityRef(checkedOut.shoppingCartId).ask(ShoppingCartCommand.Get::new, askTimeout)
							.thenApply(summary -> Pair.create(asShoppingCartView(checkedOut.shoppingCartId, summary),
									eventAndOffset.second()));
				}));
	}

	@Override
	public Topic<com.lagom.TCSShoppingCart.shoppingCart.api.ShoppingCartEvent> helloEvents() {
		// We want to publish all the shards of the hello event
		return TopicProducer.taggedStreamWithOffset(ShoppingCartEvent.TAG.allTags(), (tag, offset) ->

		// Load the event stream for the passed in shard tag
		persistentEntityRegistry.eventStream(tag, offset).map(eventAndOffset -> {

			// Now we want to convert from the persisted event to the published event.
			// Although these two events are currently identical, in future they may
			// change and need to evolve separately, by separating them now we save
			// a lot of potential trouble in future.
			com.lagom.TCSShoppingCart.shoppingCart.api.ShoppingCartEvent eventToPublish;

			if (eventAndOffset.first() instanceof ShoppingCartEvent.GreetingMessageChanged) {
				ShoppingCartEvent.GreetingMessageChanged messageChanged = (ShoppingCartEvent.GreetingMessageChanged) eventAndOffset
						.first();
				eventToPublish = new com.lagom.TCSShoppingCart.shoppingCart.api.ShoppingCartEvent.GreetingMessageChanged(
						messageChanged.getName(), messageChanged.getMessage());
			} else {
				throw new IllegalArgumentException("Unknown event: " + eventAndOffset.first());
			}

			// We return a pair of the translated event, and its offset, so that
			// Lagom can track which offsets have been published.
			return Pair.create(eventToPublish, eventAndOffset.second());
		}));
	}

	private EntityRef<ShoppingCartCommand> entityRef(String id) {
		return clusterSharding.entityRefFor(ShoppingCartAggregate.ENTITY_TYPE_KEY, id);
	}

	private ShoppingCartCommand.Accepted handleConfirmation(ShoppingCartCommand.Confirmation confirmation) {
		if (confirmation instanceof ShoppingCartCommand.Accepted) {
			ShoppingCartCommand.Accepted accepted = (ShoppingCartCommand.Accepted) confirmation;
			return accepted;
		}

		ShoppingCartCommand.Rejected rejected = (ShoppingCartCommand.Rejected) confirmation;
		throw new BadRequest(rejected.reason);
	}

	private ShoppingCartView asShoppingCartView(String id, ShoppingCartCommand.Summary summary) {
		List<ShoppingCartItem> items = new ArrayList<>();
		for (Map.Entry<String, Integer> item : summary.items.entrySet()) {
			items.add(new ShoppingCartItem(item.getKey(), item.getValue()));
		}
		return new ShoppingCartView(id, items, summary.checkoutDate);
	}

}
