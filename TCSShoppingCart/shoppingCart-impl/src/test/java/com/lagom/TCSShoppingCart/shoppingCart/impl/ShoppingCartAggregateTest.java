package com.lagom.TCSShoppingCart.shoppingCart.impl;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import akka.cluster.sharding.typed.javadsl.EntityContext;
import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartCommand.Hello;
import com.lagom.TCSShoppingCart.shoppingCart.impl.ShoppingCartCommand.UseGreetingMessage;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.UUID;

public class ShoppingCartAggregateTest {
  private static final String inmemConfig =
      "akka.persistence.journal.plugin = \"akka.persistence.journal.inmem\" \n";

  private static final String snapshotConfig =
      "akka.persistence.snapshot-store.plugin = \"akka.persistence.snapshot-store.local\" \n"
      + "akka.persistence.snapshot-store.local.dir = \"target/snapshot-"
      + UUID.randomUUID().toString()
      + "\" \n";

  private static final String config = inmemConfig + snapshotConfig;

  @ClassRule
  public static final TestKitJunitResource testKit = new TestKitJunitResource(config);

  @Test
  public void testHello() {

      String id = "Alice";
      ActorRef<ShoppingCartCommand> ref =
        testKit.spawn(
          ShoppingCartAggregate.create(
            // Unit testing the Aggregate requires an EntityContext but starting
            // a complete Akka Cluster or sharding the actors is not requried.
            // The actorRef to the shard can be null as it won't be used.
            new EntityContext(ShoppingCartAggregate.ENTITY_TYPE_KEY, id,  null)
          )
        );

      TestProbe<ShoppingCartCommand.Greeting> probe =
        testKit.createTestProbe(ShoppingCartCommand.Greeting.class);
      ref.tell(new Hello(id,probe.getRef()));
      probe.expectMessage(new ShoppingCartCommand.Greeting("Hello, Alice!"));
  }

  @Test
  public void testUpdateGreeting() {
//      String id = "Alice";
//      ActorRef<ShoppingCartCommand> ref =
//        testKit.spawn(
//          ShoppingCartAggregate.create(
//            // Unit testing the Aggregate requires an EntityContext but starting
//            // a complete Akka Cluster or sharding the actors is not requried.
//            // The actorRef to the shard can be null as it won't be used.
//           new EntityContext(ShoppingCartAggregate.ENTITY_TYPE_KEY, id,  null)
//          )
//        );
//
//      TestProbe<ShoppingCartCommand.Confirmation> probe1 =
//        testKit.createTestProbe(ShoppingCartCommand.Confirmation.class);
//      ref.tell(new UseGreetingMessage("Hi", probe1.getRef()));
//      probe1.expectMessage(new ShoppingCartCommand.Accepted());
//
//      TestProbe<ShoppingCartCommand.Greeting> probe2 =
//        testKit.createTestProbe(ShoppingCartCommand.Greeting.class);
//      ref.tell(new Hello(id,probe2.getRef()));
//      probe2.expectMessage(new ShoppingCartCommand.Greeting("Hi, Alice!"));
    }
}
