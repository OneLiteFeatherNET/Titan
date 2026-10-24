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

import net.onelitefeather.titan.core.testfixtures.AdjustableClock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.item.PlayerBeginItemUseEvent;
import net.minestom.server.event.item.PlayerCancelItemUseEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemAnimation;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.titan.core.permission.PermissionService;
import net.onelitefeather.titan.core.settings.PlayerSettings;
import net.onelitefeather.titan.core.settings.SettingsRegistry;
import net.onelitefeather.titan.core.telemetry.Telemetry;
import net.onelitefeather.titan.core.testfixtures.TestTitanNode;

/**
 * Builds the throw column the way the tests need it, and drives the client actions it reacts to.
 */
final class ThrowFixtures {

    static final Instant NOW = Instant.parse("2026-10-24T12:00:00Z");
    static final Pos SPOT = new Pos(0, 40, 0);
    /** Ticks a fully held charge reports to the cancel event: 1.5 seconds. */
    static final long FULL_CHARGE = 30;

    private ThrowFixtures() {
    }

    static ThrowModule module(Env env, TestTitanNode titan, PermissionService permissions, PlayerSettings settings, Clock clock, Telemetry telemetry) {
        return new ThrowModule(titan.node(), env.process().scheduler(), clock, telemetry, permissions, settings, new SettingsRegistry());
    }

    static AdjustableClock clock() {
        return new AdjustableClock(NOW, ZoneOffset.UTC);
    }

    /** A player on a real test connection, standing at {@code spot}. */
    static Player connect(Env env, Instance instance, Pos spot) {
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        player.teleport(spot);
        return player;
    }

    /** A right-click on {@code target} with the main hand, as the client sends it. */
    static void rightClick(Env env, Player thrower, Player target) {
        env.process().eventHandler().call(new PlayerEntityInteractEvent(thrower, target, PlayerHand.MAIN, target.getPosition()));
    }

    static ItemStack heldItem(Player player) {
        return player.getInventory().getItemStack(player.getHeldSlot());
    }

    /** A charge a human starts after the pickup grace: the clock moves past it first. */
    static void beginChargeAfterGrace(Env env, Player thrower, AdjustableClock clock) {
        clock.advance(ThrowModule.PICKUP_CLICK_GRACE);
        beginCharge(env, thrower);
    }

    /** The client starts drawing the charge item. */
    static void beginCharge(Env env, Player thrower) {
        env.process().eventHandler().call(new PlayerBeginItemUseEvent(thrower, PlayerHand.MAIN, heldItem(thrower), ItemAnimation.BOW, 72_000));
    }

    /** The client releases the charge item after {@code ticks} ticks. */
    static void release(Env env, Player thrower, long ticks) {
        env.process().eventHandler().call(new PlayerCancelItemUseEvent(thrower, PlayerHand.MAIN, heldItem(thrower), ticks));
    }

    static boolean carries(Player thrower, Player target) {
        return target.getVehicle() == thrower;
    }
}
