/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.feature.portal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.module.FeatureNode;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.testfixtures.EventListenerCounter;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MicrotusExtension.class)
class PortalModuleLeakTest {

    private final AdjustableClock clock = new AdjustableClock(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);
    private final RecordingDeliver deliver = new RecordingDeliver();

    private PortalModule module(Env env, TestTitanNode titan) {
        Portal portal = new Portal("survival", new Box(new Vec(0, 64, 0), new Vec(1, 65, 1)), "Survival", null);
        return new PortalModule(titan.node(), () -> List.of(portal), this.deliver, new FakePermissionService(), this.clock, env.createFlatInstance(), env.process().scheduler(), Runnable::run, unused -> new LabelReading.Local(0), new PortalSettings(1));
    }

    @DisplayName("After stop the module's node is gone and a move into a portal delivers nothing")
    @Test
    void stopRemovesTheNodeAndTheListeners(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            PortalModule module = module(env, titan);
            module.start();
            Instance instance = env.createFlatInstance();
            Player player = env.createPlayer(instance);
            player.teleport(new Pos(10.5, 64, 0.5)).join();
            Assertions.assertEquals(1, titan.node().findChildren("titan/portal").size(), "started module must own exactly one 'titan/portal' node");

            module.stop();
            env.process().eventHandler().call(new PlayerMoveEvent(player, new Pos(0.5, 64, 0.5), true));

            Assertions.assertTrue(titan.node().findChildren("titan/portal").isEmpty(), "stop must detach the node");
            Assertions.assertTrue(this.deliver.deliveries().isEmpty(), "a stopped module must not react to moves");
        }
    }

    @DisplayName("Moving many times never adds a listener to the module's node")
    @Test
    void movingDoesNotAddListeners(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            PortalModule module = module(env, titan);
            module.start();
            Player player = env.createPlayer(env.createFlatInstance());
            List<EventNode<Event>> children = titan.node().findChildren("titan/portal");
            int before = EventListenerCounter.countListeners(children.get(0));

            for (int i = 0; i < 50; i++) {
                env.process().eventHandler().call(new PlayerMoveEvent(player, new Pos(10.5 + i, 64, 0.5), true));
            }

            Assertions.assertEquals(before, EventListenerCounter.countListeners(children.get(0)), "handling moves must not register listeners");
            module.stop();
        }
    }

    @DisplayName("The portal node sits at priority 900 and coexists with a node at another priority")
    @Test
    void priorityIs900AndDoesNotCollide(Env env) {
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            FeatureNode other = FeatureNode.attach(titan.node(), "other", 200);
            PortalModule module = module(env, titan);

            module.start();

            Assertions.assertEquals(900, titan.node().findChildren("titan/portal").get(0).getPriority(), "priority must match the documented 900");
            module.stop();
            other.close();
        }
    }
}
