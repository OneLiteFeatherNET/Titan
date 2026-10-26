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
package net.onelitefeather.titan.feature.navigator;

import java.time.Instant;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.service.ServiceState;
import net.onelitefeather.titan.core.service.ServiceState.Phase;
import net.onelitefeather.titan.core.testfixtures.AdjustableClock;
import net.onelitefeather.titan.core.testfixtures.FakeOnDemandServices;
import net.onelitefeather.titan.core.testfixtures.FakePermissionService;
import net.onelitefeather.titan.core.testfixtures.RecordingDeliver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * A click on a staging entry through the navigator, with the services served by a fake and the
 * server advanced by explicit ticks: the first click starts one service, a second click starts
 * none,
 * and the waiting player is connected by the refresh once the service is ready.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorStagingStartTest {

    private static final String STAGING_PERMISSION = "cygnus.staging.join";
    private static final String TASK = "Slender-Staging";
    private static final String SERVICE = "Slender-Staging-1";
    private static final int STAGING_SLOT = Destination.SLENDER_STAGING.slot();
    private static final int STAGING_HUB_SLOT = NavigatorLayout.STAGING_HUB_SLOT;
    private static final Pos SPAWN = new Pos(11.5, 40, 60);
    // The refresh runs every REFRESH_SECONDS, which is 20 ticks at the server rate; 25 ticks reach one.
    private static final int TICKS_PAST_A_REFRESH = 25;

    private static AbstractInventory openStagingMenu(Env env, NavigatorFixture fixture, Player player) {
        fixture.equip(player);
        fixture.useFeather(player);
        AbstractInventory navigator = player.getOpenInventory();
        click(env, navigator, player, new Click.Left(STAGING_HUB_SLOT));
        return player.getOpenInventory();
    }

    private static void click(Env env, AbstractInventory inventory, Player player, Click click) {
        env.process().eventHandler().call(new InventoryPreClickEvent(inventory, player, click));
    }

    @DisplayName("A click on a staging entry with no running service starts exactly one service")
    @Test
    void firstClickStartsExactlyOneService(Env env) {
        FakeOnDemandServices services = new FakeOnDemandServices();
        RecordingDeliver deliver = new RecordingDeliver();
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = fixture(env, deliver, services)) {
            AbstractInventory submenu = openStagingMenu(env, fixture, player);

            click(env, submenu, player, new Click.Left(STAGING_SLOT));
            NavigatorFixture.tick(env, 1);

            Assertions.assertEquals(1, services.starts(), "one click with no running service starts exactly one");
            Assertions.assertTrue(deliver.deliveries().isEmpty(), "the player waits until the service is ready");
        }
    }

    @DisplayName("A second click while the service starts starts nothing")
    @Test
    void secondClickStartsNothing(Env env) {
        FakeOnDemandServices services = new FakeOnDemandServices();
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = fixture(env, new RecordingDeliver(), services)) {
            AbstractInventory submenu = openStagingMenu(env, fixture, player);
            click(env, submenu, player, new Click.Left(STAGING_SLOT));
            NavigatorFixture.tick(env, 1);

            AbstractInventory again = openStagingMenu(env, fixture, player);
            click(env, again, player, new Click.Left(STAGING_SLOT));
            NavigatorFixture.tick(env, 1);

            Assertions.assertEquals(1, services.starts(), "a second click must not start a second service");
        }
    }

    @DisplayName("A waiting player is connected to the staging task once the service is ready and a refresh runs")
    @Test
    void waitingPlayerConnectsWhenTheServiceIsReady(Env env) {
        FakeOnDemandServices services = new FakeOnDemandServices();
        RecordingDeliver deliver = new RecordingDeliver();
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = fixture(env, deliver, services)) {
            AbstractInventory submenu = openStagingMenu(env, fixture, player);
            click(env, submenu, player, new Click.Left(STAGING_SLOT));
            NavigatorFixture.tick(env, 1);

            services.serve(TASK, new ServiceState(SERVICE, Phase.RUNNING, 0, 10));
            NavigatorFixture.tick(env, TICKS_PAST_A_REFRESH);

            Assertions.assertEquals(1, deliver.deliveries().size(), "the waiting player is sent once");
            Assertions.assertEquals(TASK, deliver.deliveries().getFirst().taskName(), "the player is sent to the staging task");
        }
    }

    @DisplayName("A player who leaves while waiting is not sent to the staging task when the service becomes ready")
    @Test
    void leavingPlayerIsNotSentWhenTheServiceIsReady(Env env) {
        FakeOnDemandServices services = new FakeOnDemandServices();
        RecordingDeliver deliver = new RecordingDeliver();
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = fixture(env, deliver, services)) {
            AbstractInventory submenu = openStagingMenu(env, fixture, player);
            click(env, submenu, player, new Click.Left(STAGING_SLOT));
            NavigatorFixture.tick(env, 1);

            env.process().eventHandler().call(new PlayerDisconnectEvent(player));
            services.serve(TASK, new ServiceState(SERVICE, Phase.RUNNING, 0, 10));
            NavigatorFixture.tick(env, TICKS_PAST_A_REFRESH);

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a player who left must not be sent");
        }
    }

    private static NavigatorFixture fixture(Env env, RecordingDeliver deliver, FakeOnDemandServices services) {
        return NavigatorFixture.startWithStaging(env, deliver, new FakePermissionService().allowEveryone(STAGING_PERMISSION), services, new AdjustableClock(Instant.parse("2026-10-26T12:00:00Z")));
    }
}
