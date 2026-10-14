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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.ServerFlag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.portal.Box;
import net.onelitefeather.titan.core.portal.LobbyPortals;
import net.onelitefeather.titan.core.portal.Portal;
import net.onelitefeather.titan.core.portal.ServiceCount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The click hint as a player sees it in the opened navigator: the Aves inventory of the player's
 * language, with the portal of the lobby map decided at each refresh.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorClickHintTest {

    private static final int ELYTRA_RACE_SLOT = 0;
    private static final int SPAWN_SLOT = 2;
    private static final int SURVIVAL_SLOT = 4;
    private static final Box AREA = new Box(new Vec(10, 40, 20), new Vec(12, 42, 22));
    private static final LobbyPortals NO_PORTALS = List::of;
    private static final LobbyPortals SURVIVAL_PORTAL = () -> List.of(new Portal("survival", AREA, "Survival", null));
    private static final ServiceCount RACE_ONE = new ServiceCount("ElytraRace-1", 3, 50);
    private static final ServiceCount RACE_TWO = new ServiceCount("ElytraRace-2", 10, 50);
    // One refresh period; a test ticks this long so the refresh it needs has run.
    private static final int REFRESH_TICKS = NavigatorFixture.REFRESH_SECONDS * ServerFlag.SERVER_TICKS_PER_SECOND;
    private static final String BUILD_PERMISSION = "titan.navigator.buildserver";

    private static FakeFeatureFlags slenderActive() {
        return new FakeFeatureFlags().declare(Destination.SLENDER_FLAG, true);
    }

    private static void tick(Env env, int ticks) {
        for (int i = 0; i < ticks; i++) {
            env.tick();
        }
    }

    private static List<String> lore(ItemStack item) {
        return Optional.ofNullable(item.get(DataComponents.LORE)).orElse(List.of()).stream().map(line -> PlainTextComponentSerializer.plainText().serialize(line)).toList();
    }

    private static AbstractInventory open(NavigatorFixture fixture, Player player) {
        fixture.equip(player);
        fixture.useFeather(player);
        return player.getOpenInventory();
    }

    private static Player player(Env env, Locale locale) {
        Player player = env.createPlayer(env.createFlatInstance(), new Pos(11.5, 40, 60));
        player.setLocale(locale);
        return player;
    }

    @DisplayName("With a portal, Survival names the left click and the right click")
    @Test
    void survivalWithPortalNamesBothClicks(Env env) {
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), slenderActive(), new FakePermissionService(), SURVIVAL_PORTAL)) {
            AbstractInventory inventory = open(fixture, player(env, Locale.ENGLISH));

            Assertions.assertEquals(List.of("Left click: connect · Right click: walk to portal"), lore(inventory.getItemStack(SURVIVAL_SLOT)));
        }
    }

    @DisplayName("Without a portal, Survival names only the left click")
    @Test
    void survivalWithoutPortalNamesOnlyTheLeftClick(Env env) {
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), slenderActive(), new FakePermissionService(), NO_PORTALS)) {
            AbstractInventory inventory = open(fixture, player(env, Locale.ENGLISH));

            Assertions.assertEquals(List.of("Left click: connect"), lore(inventory.getItemStack(SURVIVAL_SLOT)));
        }
    }

    @DisplayName("ElytraRace running on several services names the server choice on the left click")
    @Test
    void elytraRaceWithSeveralServicesNamesTheServerChoice(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE, RACE_TWO);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), slenderActive(), new FakePermissionService(), counts, NO_PORTALS)) {
            tick(env, REFRESH_TICKS);
            AbstractInventory inventory = open(fixture, player(env, Locale.ENGLISH));

            Assertions.assertEquals(List.of("13 online", "Left click: choose a server"), lore(inventory.getItemStack(ELYTRA_RACE_SLOT)), "the picker opens for several services, so the hint names it");
        }
    }

    @DisplayName("ElytraRace running on one service names the connect on the left click, because the click connects directly")
    @Test
    void elytraRaceWithOneServiceNamesTheConnect(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), slenderActive(), new FakePermissionService(), counts, NO_PORTALS)) {
            tick(env, REFRESH_TICKS);
            AbstractInventory inventory = open(fixture, player(env, Locale.ENGLISH));

            Assertions.assertEquals(List.of("3 online", "Left click: connect"), lore(inventory.getItemStack(ELYTRA_RACE_SLOT)), "one service never opens the picker, so the hint must not promise it");
        }
    }

    @DisplayName("ElytraRace running nowhere names the connect on the left click")
    @Test
    void elytraRaceWithoutServicesNamesTheConnect(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), slenderActive(), new FakePermissionService(), counts, NO_PORTALS)) {
            tick(env, REFRESH_TICKS);
            AbstractInventory inventory = open(fixture, player(env, Locale.ENGLISH));

            Assertions.assertEquals(List.of("Left click: connect"), lore(inventory.getItemStack(ELYTRA_RACE_SLOT)), "nothing running means a plain connect, as before the picker");
        }
    }

    @DisplayName("Opening right after a portal was added shows the walk-to-portal hint without waiting for a refresh")
    @Test
    void hintIsFreshWhenOpenedRightAfterPortalAdded(Env env) {
        AtomicReference<List<Portal>> portals = new AtomicReference<>(List.of());
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), slenderActive(), new FakePermissionService(), () -> portals.get())) {
            tick(env, REFRESH_TICKS);
            portals.set(List.of(new Portal("survival", AREA, "Survival", null)));

            AbstractInventory inventory = open(fixture, player(env, Locale.ENGLISH));

            Assertions.assertEquals(List.of("Left click: connect · Right click: walk to portal"), lore(inventory.getItemStack(SURVIVAL_SLOT)), "the menu must show the portal that exists when it opens, not the one the last refresh saw");
        }
    }

    @DisplayName("The spawn entry has no click hint")
    @Test
    void spawnHasNoClickHint(Env env) {
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), slenderActive(), new FakePermissionService(), SURVIVAL_PORTAL)) {
            AbstractInventory inventory = open(fixture, player(env, Locale.ENGLISH));

            Assertions.assertTrue(lore(inventory.getItemStack(SPAWN_SLOT)).isEmpty(), "spawn's click does not depend on the mouse button");
        }
    }

    @DisplayName("A German player reads the German click hint")
    @Test
    void germanPlayerReadsGermanHint(Env env) {
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), slenderActive(), new FakePermissionService(), SURVIVAL_PORTAL)) {
            AbstractInventory inventory = open(fixture, player(env, Locale.GERMANY));

            Assertions.assertEquals(List.of("Linksklick: verbinden · Rechtsklick: zum Portal laufen"), lore(inventory.getItemStack(SURVIVAL_SLOT)));
        }
    }

    @DisplayName("Removing the portal drops the right click from the hint at the next refresh")
    @Test
    void removedPortalDropsRightClickAtNextRefresh(Env env) {
        AtomicReference<List<Portal>> portals = new AtomicReference<>(List.of(new Portal("survival", AREA, "Survival", null)));
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), slenderActive(), new FakePermissionService(), () -> portals.get())) {
            AbstractInventory inventory = open(fixture, player(env, Locale.ENGLISH));
            portals.set(List.of());

            tick(env, REFRESH_TICKS);

            Assertions.assertEquals(List.of("Left click: connect"), lore(inventory.getItemStack(SURVIVAL_SLOT)), "the right click must be gone once a refresh has seen the portal removed");
        }
    }

    @DisplayName("A German player with the build permission reads the German hint on Build in the team menu")
    @Test
    void germanTeamPlayerReadsGermanHintOnBuild(Env env) {
        Player player = player(env, Locale.GERMANY);
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), slenderActive(), permissions, NO_PORTALS)) {
            AbstractInventory inventory = open(fixture, player);

            Assertions.assertEquals(List.of("Linksklick: verbinden"), lore(inventory.getItemStack(Destination.BUILD.slot())), "the team menu in German must name the connect in German");
        }
    }
}
