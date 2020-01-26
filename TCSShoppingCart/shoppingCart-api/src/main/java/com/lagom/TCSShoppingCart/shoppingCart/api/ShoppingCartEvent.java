package com.lagom.TCSShoppingCart.shoppingCart.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Preconditions;

import lombok.Value;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = ShoppingCartEvent.GreetingMessageChanged.class, name = "greeting-message-changed")
})
public interface ShoppingCartEvent {

  String getName();

  @Value
  final class GreetingMessageChanged implements ShoppingCartEvent {
    public final String name;
    public final String message;

    @JsonCreator
    public GreetingMessageChanged(String name, String message) {
        this.name = Preconditions.checkNotNull(name, "name");
        this.message = Preconditions.checkNotNull(message, "message");
    }

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

		public String getMessage() {
		// TODO Auto-generated method stub
		return null;
	}
  }
}
