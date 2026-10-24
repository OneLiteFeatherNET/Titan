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
package net.onelitefeather.titan.feature.throwing;

import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.client.play.ClientInputPacket;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.settings.SettingsRegistry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Every way a carry ends gives the thrower's hotbar slot back. One path per test: a failure names
 * the path that broke.
 */
@ExtendWith(MicrotusExtension.class)
class ThrowModuleHandRestoreTest {

    @DisplayName("A throw gives the thrower's hand back before the target flies")
    @Test
    void throwRestoresTheHand(Env env) {
        Instance instance = env.createFlatInstance();
        Carrying carrying = Carrying.start(env, instance);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = carrying.startWith(env, titan);
            try {
                carrying.clock().advance(ThrowModule.PICKUP_CLICK_GRACE);
                ThrowFixtures.beginCharge(env, carrying.thrower());
                ThrowFixtures.release(env, carrying.thrower(), ThrowFixtures.FULL_CHARGE);

                carrying.assertHandRestored("a throw");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Sneaking off the thrower's head gives the hand back without an impulse")
    @Test
    void sneakRestoresTheHand(Env env) {
        Instance instance = env.createFlatInstance();
        Carrying carrying = Carrying.start(env, instance);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = carrying.startWith(env, titan);
            try {
                env.process().eventHandler().call(new PlayerPacketEvent(carrying.target(), new ClientInputPacket(false, false, false, false, false, true, false)));

                carrying.assertHandRestored("sneaking off");
                Assertions.assertEquals(0.0, carrying.target().getVelocity().length(), 1e-6, "sneaking off must not throw");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The automatic drop after fifteen seconds gives the hand back")
    @Test
    void autoDropRestoresTheHand(Env env) {
        Instance instance = env.createFlatInstance();
        Carrying carrying = Carrying.start(env, instance);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = carrying.startWith(env, titan);
            try {
                carrying.clock().advance(Duration.ofSeconds(15));
                env.tick();

                carrying.assertHandRestored("the automatic drop");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Switching the opt-out on gives the hand back at once")
    @Test
    void optOutRestoresTheHand(Env env) {
        Instance instance = env.createFlatInstance();
        Carrying carrying = Carrying.start(env, instance);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = carrying.startWith(env, titan);
            try {
                carrying.settings().set(carrying.target().getUuid(), ThrowSettings.TARGET, false);

                carrying.assertHandRestored("the target switching the opt-out on");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Becoming immune during the carry gives the hand back within a second")
    @Test
    void immunityRestoresTheHand(Env env) {
        Instance instance = env.createFlatInstance();
        Carrying carrying = Carrying.start(env, instance);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = carrying.startWith(env, titan);
            try {
                carrying.permissions().makeImmune(carrying.target().getUuid());
                for (int tick = 0; tick < ThrowModule.IMMUNITY_CHECK_TICKS; tick++) {
                    env.tick();
                }

                carrying.assertHandRestored("the target becoming immune");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The target leaving the game gives the thrower's hand back")
    @Test
    void disconnectRestoresTheHand(Env env) {
        Instance instance = env.createFlatInstance();
        Carrying carrying = Carrying.start(env, instance);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = carrying.startWith(env, titan);
            try {
                env.process().eventHandler().call(new PlayerDisconnectEvent(carrying.target()));

                carrying.assertHandRestored("the target disconnecting");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The thrower changing instance gives the hand back before the change")
    @Test
    void instanceChangeRestoresTheHand(Env env) {
        Instance instance = env.createFlatInstance();
        Instance other = env.createFlatInstance();
        Carrying carrying = Carrying.start(env, instance);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = carrying.startWith(env, titan);
            try {
                carrying.thrower().setInstance(other, ThrowFixtures.SPOT).join();

                carrying.assertHandRestored("the thrower changing instance");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("The thrower dying gives the hand back before the respawn")
    @Test
    void deathRestoresTheHand(Env env) {
        Instance instance = env.createFlatInstance();
        Carrying carrying = Carrying.start(env, instance);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = carrying.startWith(env, titan);
            try {
                env.process().eventHandler().call(new PlayerDeathEvent(carrying.thrower(), Component.empty(), Component.empty()));

                carrying.assertHandRestored("the thrower dying");
            } finally {
                module.stop();
            }
        }
    }

    @DisplayName("Stopping the column gives every thrower's hand back")
    @Test
    void stopRestoresTheHand(Env env) {
        Instance instance = env.createFlatInstance();
        Carrying carrying = Carrying.start(env, instance);
        try (TestTitanNode titan = TestTitanNode.attach(env)) {
            ThrowModule module = carrying.startWith(env, titan);

            module.stop();

            carrying.assertHandRestored("the column stopping");
        }
    }

    /** A thrower who has picked up a target: the shared start of every path above. */
    record Carrying(Player thrower, Player target, ItemStack handBefore, AdjustableClock clock,
                    FakePlayerSettings settings, FakePermissionService permissions) {

        static Carrying start(Env env, Instance instance) {
            Player thrower = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
            Player target = ThrowFixtures.connect(env, instance, ThrowFixtures.SPOT);
            ItemStack before = ThrowFixtures.heldItem(thrower);
            Carrying carrying = new Carrying(thrower, target, before, ThrowFixtures.clock(), new FakePlayerSettings(), new FakePermissionService().grant(ThrowModule.PERMISSION));
            return carrying;
        }

        /**
         * Starts a column and picks the target up with it, so the path under test begins mid-carry.
         */
        ThrowModule startWith(Env env, TestTitanNode titan) {
            ThrowModule module = new ThrowModule(titan.node(), env.process().scheduler(), this.clock, Telemetry.noop(), this.permissions, this.settings, new SettingsRegistry());
            module.start();
            ThrowFixtures.rightClick(env, this.thrower, this.target);
            Assertions.assertTrue(CarrierItem.isCarrier(ThrowFixtures.heldItem(this.thrower)), "the pickup must put the charge item in the hand before the path is run");
            return module;
        }

        void assertHandRestored(String path) {
            Assertions.assertFalse(CarrierItem.isCarrier(ThrowFixtures.heldItem(this.thrower)), "the charge item must be gone after " + path);
            Assertions.assertEquals(this.handBefore, ThrowFixtures.heldItem(this.thrower), "the hand must hold exactly its content from before the carry after " + path);
            Assertions.assertFalse(this.target.getVehicle() == this.thrower, "the target must no longer ride the thrower after " + path);
        }
    }
}
