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
import java.util.ArrayList;
import java.util.List;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.portal.ServiceCount;
import net.onelitefeather.titan.core.service.ServiceState;
import net.onelitefeather.titan.core.service.ServiceState.Phase;
import net.onelitefeather.titan.core.testfixtures.AdjustableClock;
import net.onelitefeather.titan.core.testfixtures.FakeOnDemandServices;
import net.onelitefeather.titan.core.testfixtures.FakePermissionService;
import net.onelitefeather.titan.core.testfixtures.FakePlayerCounts;
import net.onelitefeather.titan.core.testfixtures.RecordingDeliver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Slender (Staging): a click connects straight to the {@code Slender-Staging} task behind
 * {@code cygnus.staging.join}, and the real Slender never reads or joins a staging service. The
 * counts answer by exact task name, as CloudNet does for {@code servicesByTask}.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorStagingTest {

    private static final String STAGING_PERMISSION = "cygnus.staging.join";
    private static final String SLENDER_PERMISSION = "titan.navigator.slender";
    private static final int STAGING_SLOT = Destination.SLENDER_STAGING.slot();
    private static final int SLENDER_SLOT = Destination.SLENDER.slot();
    private static final String STAGING_SERVICE = "Slender-Staging-1";
    private static final Pos SPAWN = new Pos(11.5, 40, 60);

    private static AbstractInventory openNavigator(NavigatorFixture fixture, Player player) {
        fixture.equip(player);
        fixture.useFeather(player);
        return player.getOpenInventory();
    }

    // The staging entries live in the submenu, which the hub on the main grid opens.
    private static AbstractInventory openStagingMenu(Env env, NavigatorFixture fixture, Player player) {
        AbstractInventory navigator = openNavigator(fixture, player);
        click(env, navigator, player, new Click.Left(NavigatorLayout.STAGING_HUB_SLOT));
        return player.getOpenInventory();
    }

    private static InventoryPreClickEvent click(Env env, AbstractInventory inventory, Player player, Click click) {
        InventoryPreClickEvent event = new InventoryPreClickEvent(inventory, player, click);
        env.process().eventHandler().call(event);
        return event;
    }

    // The plain names of every entry the inventory shows; blank panes have no custom name.
    private static List<String> entryNames(AbstractInventory inventory) {
        MiniMessage mini = MiniMessage.miniMessage();
        List<String> names = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItemStack(slot);
            if (stack.get(DataComponents.CUSTOM_NAME) != null) {
                names.add(mini.stripTags(mini.serialize(stack.get(DataComponents.CUSTOM_NAME))));
            }
        }
        return names;
    }

    @DisplayName("A click on Slender (Staging) in the submenu connects to the Slender-Staging task and cancels the click")
    @Test
    void clickingStagingConnectsToTheStagingTask(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        FakeOnDemandServices services = new FakeOnDemandServices().serve("Slender-Staging", new ServiceState(STAGING_SERVICE, Phase.RUNNING, 0, 10));
        try (NavigatorFixture fixture = NavigatorFixture.startWithStaging(env, deliver, new FakePermissionService().allowEveryone(STAGING_PERMISSION), services, new AdjustableClock(Instant.parse("2026-10-26T12:00:00Z")))) {
            AbstractInventory navigator = openStagingMenu(env, fixture, player);

            InventoryPreClickEvent event = click(env, navigator, player, new Click.Left(STAGING_SLOT));
            env.tick();

            Assertions.assertTrue(event.isCancelled(), "the click must be cancelled so the icon stays in place");
            Assertions.assertEquals(1, deliver.deliveries().size(), "one click sends exactly one player");
            Assertions.assertEquals("Slender-Staging", deliver.deliveries().getFirst().taskName(), "the staging entry connects to the Slender-Staging task");
        }
    }

    @DisplayName("A click on Slender (Staging) after the permission was denied connects nobody and closes the menu")
    @Test
    void deniedClickOnStagingConnectsNobody(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), STAGING_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, permissions)) {
            AbstractInventory submenu = openStagingMenu(env, fixture, player);
            permissions.set(player.getUuid(), STAGING_PERMISSION, PermissionResult.DENIED);

            click(env, submenu, player, new Click.Left(STAGING_SLOT));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "a player who lost cygnus.staging.join must not be sent");
            Assertions.assertNull(player.getOpenInventory(), "the menu closes on a denied click");
        }
    }

    @DisplayName("The team menu shows the staging hub on slot 16 and the public menu leaves that slot grey")
    @Test
    void teamMenuShowsStagingAndPublicMenuDoesNot(Env env) {
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService())) {
            Assertions.assertEquals(Material.STRUCTURE_BLOCK, fixture.module().teamInventory().getItemStack(NavigatorLayout.STAGING_HUB_SLOT).material(), "the team menu shows the staging hub");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, fixture.module().publicInventory().getItemStack(NavigatorLayout.STAGING_HUB_SLOT).material(), "the public menu has no staging hub");
        }
    }

    @DisplayName("A left click on Slender never joins Slender-Staging-1, even when it is the only service with room")
    @Test
    void slenderQuickJoinNeverPicksTheStagingService(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("Slender", new ServiceCount("Slender-1", 10, 10));
        counts.serveServices("Slender-Staging", new ServiceCount(STAGING_SERVICE, 0, 10));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone(SLENDER_PERMISSION), counts)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(SLENDER_SLOT));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "no Slender service has room, so nobody is sent to the staging one");
        }
    }

    @DisplayName("A left click on Slender joins the Slender service and not the fuller Slender-Staging-1")
    @Test
    void slenderQuickJoinStaysOnSlenderServices(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("Slender", new ServiceCount("Slender-1", 2, 10));
        counts.serveServices("Slender-Staging", new ServiceCount(STAGING_SERVICE, 9, 10));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone(SLENDER_PERMISSION), counts)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Left(SLENDER_SLOT));
            env.tick();

            Assertions.assertEquals(1, deliver.deliveries().size(), "one click sends exactly one player");
            Assertions.assertEquals("Slender-1", deliver.deliveries().getFirst().serverName(), "the quick join picks from the Slender services only");
        }
    }

    @DisplayName("The Slender server list never lists Slender-Staging-1, even when it is the only service with room")
    @Test
    void slenderServerListNeverListsTheStagingService(Env env) {
        RecordingDeliver deliver = new RecordingDeliver();
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("Slender-Staging", new ServiceCount(STAGING_SERVICE, 0, 10));
        Player player = env.createPlayer(env.createFlatInstance(), SPAWN);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, deliver, new FakePermissionService().allowEveryone(SLENDER_PERMISSION), counts)) {
            AbstractInventory navigator = openNavigator(fixture, player);

            click(env, navigator, player, new Click.Right(SLENDER_SLOT));
            env.tick();

            Assertions.assertTrue(deliver.deliveries().isEmpty(), "the list opens instead of connecting");
            Assertions.assertFalse(entryNames(player.getOpenInventory()).contains(STAGING_SERVICE), "the Slender list must not show the staging service");
        }
    }
}
