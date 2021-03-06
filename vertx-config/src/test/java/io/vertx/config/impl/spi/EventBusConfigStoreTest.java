/*
 * Copyright (c) 2014 Red Hat, Inc. and others
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */

package io.vertx.config.impl.spi;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class EventBusConfigStoreTest extends ConfigStoreTestBase {

  @Before
  public void init() {
    factory = new EventBusConfigStoreFactory();
  }

  @Test
  public void testWithSend(TestContext tc) {
    Async async = tc.async();
    store = factory.create(vertx, new JsonObject()
      .put("address", "config")
    );

    getJsonConfiguration(vertx, store, ar -> {
      assertThat(ar.result().isEmpty()).isTrue();

      vertx.eventBus().send("config", new JsonObject(HttpConfigStoreTest.JSON));

      AtomicReference<JsonObject> received = new AtomicReference<>();
      while (received.get() == null) {
        getJsonConfiguration(vertx, store, ar2 -> {
          if (!ar2.result().isEmpty()) {
            received.set(ar2.result());
          }
        });
      }
      await().untilAtomic(received, is(notNullValue()));

      ConfigChecker.check(received.get());
      async.complete();
    });
  }

  @Test
  public void testWithSendWithBuffer(TestContext tc) {
    Async async = tc.async();
    store = factory.create(vertx, new JsonObject()
      .put("address", "config")
    );

    getJsonConfiguration(vertx, store, ar -> {
      assertThat(ar.result().isEmpty()).isTrue();

      vertx.eventBus().send("config", Buffer.buffer(HttpConfigStoreTest.JSON));

      AtomicReference<JsonObject> received = new AtomicReference<>();
      while (received.get() == null) {
        getJsonConfiguration(vertx, store, ar2 -> {
          if (!ar2.result().isEmpty()) {
            received.set(ar2.result());
          }
        });
      }
      await().untilAtomic(received, is(notNullValue()));

      ConfigChecker.check(received.get());
      async.complete();
    });
  }

  private Handler<AsyncResult<JsonObject>> handler;

  @Test
  public void testWithPublish(TestContext tc) {
    Async async = tc.async();
    store = factory.create(vertx, new JsonObject()
      .put("address", "config")
    );

    getJsonConfiguration(vertx, store, ar -> {
      if (ar.failed()) {
        ar.cause().printStackTrace();
      }
      assertThat(ar.result().isEmpty()).isTrue();

      vertx.eventBus().publish("config", new JsonObject(HttpConfigStoreTest.JSON));

      handler = (ar2) -> {
        if (ar2.result().isEmpty()) {
          // Retry as the publication may not have been dispatched yet.
          getJsonConfiguration(vertx, store, handler);
          return;
        }
        ConfigChecker.check(ar2.result());
        async.complete();
      };

      getJsonConfiguration(vertx, store, handler);
    });
  }

}
