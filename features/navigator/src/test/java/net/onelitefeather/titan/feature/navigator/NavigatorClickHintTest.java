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

import static net.onelitefeather.titan.feature.navigator.NavigatorFixture.tick;

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
 * The lore of each entry as a player sees it in the opened navigator: the Aves inventory of the
 * player's language, with the portal of the lobby map decided when the menu opens.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorClickHintTest {

    private static final int ELYTRA_RACE_SLOT = Destination.ELYTRA_RACE.slot();
    private static final int SPAWN_SLOT = NavigatorLayout.SPAWN_SLOT;
    private static final int SURVIVAL_SLOT = Destination.SURVIVAL.slot();
    private static final int BUILD_SLOT = Destination.BUILD.slot();
    private static final String SEPARATOR = "━━━━━━━━━━━━";
    private static final Box AREA = new Box(new Vec(10, 40, 20), new Vec(12, 42, 22));
    private static final LobbyPortals NO_PORTALS = List::of;
    private static final LobbyPortals SURVIVAL_PORTAL = () -> List.of(new Portal("survival", AREA, "Survival", null));
    private static final LobbyPortals RACE_PORTAL = () -> List.of(new Portal("elytrarace", AREA, "ElytraRace", null));
    private static final ServiceCount RACE_ONE = new ServiceCount("ElytraRace-1", 3, 50);
    private static final ServiceCount RACE_TWO = new ServiceCount("ElytraRace-2", 10, 50);
    // One refresh period; a test ticks this long so the refresh it needs has run.
    private static final int REFRESH_TICKS = NavigatorFixture.REFRESH_SECONDS * ServerFlag.SERVER_TICKS_PER_SECOND;
    private static final String BUILD_PERMISSION = "titan.navigator.buildserver";
    private static final String SLENDER_PERMISSION = "titan.navigator.slender";
    private static final String BOUNCE_PERMISSION = "titan.navigator.bounce";
    private static final int BOUNCE_SLOT = Destination.BOUNCE.slot();

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

    @DisplayName("With a portal, Survival lists the left click and the shift click, after a blank line")
    @Test
    void survivalWithPortalListsBothClicks(Env env) {
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone(SLENDER_PERMISSION), SURVIVAL_PORTAL)) {
            AbstractInventory inventory = open(fixture, player(env, Locale.ENGLISH));

            Assertions.assertEquals(List.of(SEPARATOR, "", "▸ Left click  Connect", "▸ Shift click  Walk to portal"), lore(inventory.getItemStack(SURVIVAL_SLOT)), "a server with a portal lists connect and the walk to the portal");
        }
    }

    @DisplayName("Without a portal, Survival lists only the connect")
    @Test
    void survivalWithoutPortalListsOnlyTheConnect(Env env) {
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone(SLENDER_PERMISSION), NO_PORTALS)) {
            AbstractInventory inventory = open(fixture, player(env, Locale.ENGLISH));

            Assertions.assertEquals(List.of(SEPARATOR, "", "▸ Left click  Connect"), lore(inventory.getItemStack(SURVIVAL_SLOT)), "without a portal there is no shift line");
        }
    }

    @DisplayName("ElytraRace lists quick join and the server choice even with several services")
    @Test
    void elytraRaceWithSeveralServicesListsQuickJoinAndPicker(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE, RACE_TWO);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone(SLENDER_PERMISSION), counts, NO_PORTALS)) {
            tick(env, REFRESH_TICKS);
            AbstractInventory inventory = open(fixture, player(env, Locale.ENGLISH));

            Assertions.assertEquals(List.of(SEPARATOR, "Players  13 / 100", "", "▸ Left click  Quick join", "▸ Right click  Choose a server"), lore(inventory.getItemStack(ELYTRA_RACE_SLOT)), "the players are summed and the maximum is known for both services");
        }
    }

    @DisplayName("Bounce running nowhere lists quick join and the server choice, and no shift line without a portal")
    @Test
    void bounceWithoutServicesListsQuickJoinAndPickerOnly(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone(BOUNCE_PERMISSION), counts, NO_PORTALS)) {
            tick(env, REFRESH_TICKS);
            AbstractInventory inventory = open(fixture, player(env, Locale.ENGLISH));

            Assertions.assertEquals(List.of(SEPARATOR, "", "▸ Left click  Quick join", "▸ Right click  Choose a server"), lore(inventory.getItemStack(BOUNCE_SLOT)), "Bounce lists the same clicks as ElytraRace and no players line while it runs nowhere");
        }
    }

    @DisplayName("ElytraRace with one service lists the same clicks, because the right click always opens the picker")
    @Test
    void elytraRaceWithOneServiceListsTheSameClicks(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone(SLENDER_PERMISSION), counts, NO_PORTALS)) {
            tick(env, REFRESH_TICKS);
            AbstractInventory inventory = open(fixture, player(env, Locale.ENGLISH));

            Assertions.assertEquals(List.of(SEPARATOR, "Players  3 / 50", "", "▸ Left click  Quick join", "▸ Right click  Choose a server"), lore(inventory.getItemStack(ELYTRA_RACE_SLOT)), "the hint no longer depends on how many services run");
        }
    }

    @DisplayName("ElytraRace running nowhere lists the same clicks without a players line")
    @Test
    void elytraRaceWithoutServicesListsTheSameClicks(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone(SLENDER_PERMISSION), counts, NO_PORTALS)) {
            tick(env, REFRESH_TICKS);
            AbstractInventory inventory = open(fixture, player(env, Locale.ENGLISH));

            Assertions.assertEquals(List.of(SEPARATOR, "", "▸ Left click  Quick join", "▸ Right click  Choose a server"), lore(inventory.getItemStack(ELYTRA_RACE_SLOT)), "an unknown count adds no players line");
        }
    }

    @DisplayName("ElytraRace with a portal lists the shift line last")
    @Test
    void elytraRaceWithPortalListsTheShiftLineLast(Env env) {
        FakePlayerCounts counts = new FakePlayerCounts();
        counts.serveServices("ElytraRace", RACE_ONE, RACE_TWO);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone(SLENDER_PERMISSION), counts, RACE_PORTAL)) {
            tick(env, REFRESH_TICKS);
            AbstractInventory inventory = open(fixture, player(env, Locale.ENGLISH));

            Assertions.assertEquals(List.of(SEPARATOR, "Players  13 / 100", "", "▸ Left click  Quick join", "▸ Right click  Choose a server", "▸ Shift click  Walk to portal"), lore(inventory.getItemStack(ELYTRA_RACE_SLOT)), "the portal line follows the two click lines");
        }
    }

    @DisplayName("Opening right after a portal was added shows the walk-to-portal line without waiting for a refresh")
    @Test
    void hintIsFreshWhenOpenedRightAfterPortalAdded(Env env) {
        AtomicReference<List<Portal>> portals = new AtomicReference<>(List.of());
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone(SLENDER_PERMISSION), () -> portals.get())) {
            tick(env, REFRESH_TICKS);
            portals.set(List.of(new Portal("survival", AREA, "Survival", null)));

            AbstractInventory inventory = open(fixture, player(env, Locale.ENGLISH));

            Assertions.assertEquals(List.of(SEPARATOR, "", "▸ Left click  Connect", "▸ Shift click  Walk to portal"), lore(inventory.getItemStack(SURVIVAL_SLOT)), "the menu must show the portal that exists when it opens, not the one the last refresh saw");
        }
    }

    @DisplayName("The spawn entry has no lore")
    @Test
    void spawnHasNoLore(Env env) {
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone(SLENDER_PERMISSION), SURVIVAL_PORTAL)) {
            AbstractInventory inventory = open(fixture, player(env, Locale.ENGLISH));

            Assertions.assertTrue(lore(inventory.getItemStack(SPAWN_SLOT)).isEmpty(), "spawn must show no click lines, because its click does not depend on the mouse button");
        }
    }

    @DisplayName("A German player reads the German lines")
    @Test
    void germanPlayerReadsGermanLines(Env env) {
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone(SLENDER_PERMISSION), SURVIVAL_PORTAL)) {
            AbstractInventory inventory = open(fixture, player(env, Locale.GERMANY));

            Assertions.assertEquals(List.of(SEPARATOR, "", "▸ Linksklick  Verbinden", "▸ Shift-Klick  Zum Portal laufen"), lore(inventory.getItemStack(SURVIVAL_SLOT)), "the German player must read the German lines");
        }
    }

    @DisplayName("Removing the portal drops the shift line at the next refresh")
    @Test
    void removedPortalDropsTheShiftLineAtNextRefresh(Env env) {
        AtomicReference<List<Portal>> portals = new AtomicReference<>(List.of(new Portal("survival", AREA, "Survival", null)));
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService().allowEveryone(SLENDER_PERMISSION), () -> portals.get())) {
            AbstractInventory inventory = open(fixture, player(env, Locale.ENGLISH));
            portals.set(List.of());

            tick(env, REFRESH_TICKS + 1);

            Assertions.assertEquals(List.of(SEPARATOR, "", "▸ Left click  Connect"), lore(inventory.getItemStack(SURVIVAL_SLOT)), "the shift line must be gone once a refresh has seen the portal removed");
        }
    }

    @DisplayName("A German player with the build permission reads the German connect on Build in the team menu")
    @Test
    void germanTeamPlayerReadsGermanConnectOnBuild(Env env) {
        Player player = player(env, Locale.GERMANY);
        FakePermissionService permissions = new FakePermissionService().allowEveryone(SLENDER_PERMISSION).set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions, NO_PORTALS)) {
            AbstractInventory inventory = open(fixture, player);

            Assertions.assertEquals(List.of(SEPARATOR, "", "▸ Linksklick  Verbinden"), lore(inventory.getItemStack(BUILD_SLOT)), "the team menu in German must name the connect in German");
        }
    }
}
