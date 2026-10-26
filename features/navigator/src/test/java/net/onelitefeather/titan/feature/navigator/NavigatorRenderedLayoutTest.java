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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.core.permission.PermissionResult;
import net.onelitefeather.titan.core.testfixtures.FakePermissionService;
import net.onelitefeather.titan.core.testfixtures.RecordingDeliver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The menu as a player sees it: four rows, the frame, the entries on their slots and the grey
 * panes.
 */
@ExtendWith(MicrotusExtension.class)
class NavigatorRenderedLayoutTest {

    private static final String SLENDER_PERMISSION = "titan.navigator.slender";
    private static final String BUILD_PERMISSION = "titan.navigator.buildserver";
    private static final String BOUNCE_PERMISSION = "titan.navigator.bounce";
    private static final String STAGING_PERMISSION = "cygnus.staging.join";

    private static AbstractInventory openNavigator(NavigatorFixture fixture, Player player) {
        fixture.equip(player);
        fixture.useFeather(player);
        return player.getOpenInventory();
    }

    // The plain text of a stack's custom name; a stack without one has an empty name.
    private static String nameOf(AbstractInventory inventory, int slot) {
        ItemStack stack = inventory.getItemStack(slot);
        if (stack.get(DataComponents.CUSTOM_NAME) == null) {
            return "";
        }
        MiniMessage mini = MiniMessage.miniMessage();
        return mini.stripTags(mini.serialize(stack.get(DataComponents.CUSTOM_NAME)));
    }

    @DisplayName("The navigator has four rows of nine slots")
    @Test
    void fourRows(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService())) {
            Assertions.assertEquals(36, openNavigator(fixture, player).getSize(), "four rows of nine slots");
        }
    }

    @DisplayName("The frame alternates light blue and cyan panes, and the inside is grey")
    @Test
    void frameAndInsideAreGlass(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService())) {
            AbstractInventory menu = openNavigator(fixture, player);
            Assertions.assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, menu.getItemStack(0).material(), "the top-left corner is light blue");
            Assertions.assertEquals(Material.CYAN_STAINED_GLASS_PANE, menu.getItemStack(1).material(), "the next frame pane is cyan");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getItemStack(10).material(), "an empty inner slot is grey");
        }
    }

    @DisplayName("The entries sit on their slots: ElytraRace on 11, Survival on 20, Creative on 22")
    @Test
    void entriesSitOnTheirSlots(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService())) {
            AbstractInventory menu = openNavigator(fixture, player);
            Assertions.assertEquals(Material.ELYTRA, menu.getItemStack(11).material(), "ElytraRace is on slot 11");
            Assertions.assertEquals(Material.GRASS_BLOCK, menu.getItemStack(20).material(), "Survival is on slot 20");
            Assertions.assertEquals(Material.WOODEN_AXE, menu.getItemStack(22).material(), "Creative is on slot 22");
        }
    }

    @DisplayName("Spawn is the compass in the bottom centre")
    @Test
    void spawnIsTheBottomCentre(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService())) {
            Assertions.assertEquals(Material.COMPASS, openNavigator(fixture, player).getItemStack(31).material(), "spawn is on slot 31");
        }
    }

    @DisplayName("Without the permissions Slender and Build leave grey panes on their slots")
    @Test
    void hiddenEntriesLeaveGreyPanes(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService())) {
            AbstractInventory menu = openNavigator(fixture, player);
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getItemStack(15).material(), "Slender is hidden on slot 15");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getItemStack(24).material(), "Build is hidden on slot 24");
        }
    }

    @DisplayName("With the Slender permission Slender shows on slot 15")
    @Test
    void grantedSlenderShowsOnSlot15(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), SLENDER_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            Assertions.assertEquals(Material.ENDERMAN_SPAWN_EGG, openNavigator(fixture, player).getItemStack(15).material(), "a granted Slender is on slot 15");
        }
    }

    @DisplayName("With the Bounce and Slender permissions Bounce sits on slot 13 between ElytraRace and Slender")
    @Test
    void grantedBounceShowsOnSlot13BetweenRaceAndSlender(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BOUNCE_PERMISSION, PermissionResult.ALLOWED).set(player.getUuid(), SLENDER_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            AbstractInventory menu = openNavigator(fixture, player);

            Assertions.assertEquals(Material.ELYTRA, menu.getItemStack(11).material(), "ElytraRace is on slot 11");
            Assertions.assertEquals(Material.SLIME_BALL, menu.getItemStack(13).material(), "a granted Bounce is on slot 13");
            Assertions.assertEquals(Material.ENDERMAN_SPAWN_EGG, menu.getItemStack(15).material(), "Slender is on slot 15");
        }
    }

    @DisplayName("Without the Bounce permission slot 13 is a grey pane")
    @Test
    void hiddenBounceLeavesGreyPaneOnSlot13(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService())) {
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, openNavigator(fixture, player).getItemStack(13).material(), "a hidden Bounce leaves slot 13 blank");
        }
    }

    @DisplayName("With the Build permission Build shows on slot 24")
    @Test
    void grantedBuildShowsOnSlot24(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            Assertions.assertEquals(Material.SCAFFOLDING, openNavigator(fixture, player).getItemStack(24).material(), "a granted Build is on slot 24");
        }
    }

    @DisplayName("Without cygnus.staging.join slot 16 is a grey pane when the permission is not set")
    @Test
    void notSetStagingLeavesGreyPaneOnSlot16(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService())) {
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, openNavigator(fixture, player).getItemStack(16).material(), "an unset staging grant leaves slot 16 blank");
        }
    }

    @DisplayName("Without cygnus.staging.join slot 16 is a grey pane when the permission is denied")
    @Test
    void deniedStagingLeavesGreyPaneOnSlot16(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), STAGING_PERMISSION, PermissionResult.DENIED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, openNavigator(fixture, player).getItemStack(16).material(), "a denied staging grant leaves slot 16 blank");
        }
    }

    @DisplayName("With cygnus.staging.join the staging hub shows on slot 16 as a structure block named Staging")
    @Test
    void grantedStagingShowsTheHubOnSlot16(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), STAGING_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            AbstractInventory menu = openNavigator(fixture, player);

            Assertions.assertEquals(Material.STRUCTURE_BLOCK, menu.getItemStack(NavigatorLayout.STAGING_HUB_SLOT).material(), "a granted staging permission shows the hub as a structure block on slot 16");
            Assertions.assertEquals("Staging", nameOf(menu, NavigatorLayout.STAGING_HUB_SLOT), "the hub is named Staging");
            Assertions.assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getItemStack(15).material(), "the real Slender stays hidden without its own grant");
        }
    }

    @DisplayName("Slender (Staging) in the submenu ends its lore with the team-only hint in English")
    @Test
    void stagingLoreEndsWithTheTeamOnlyHint(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        player.setLocale(Locale.ENGLISH);
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), STAGING_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            AbstractInventory navigator = openNavigator(fixture, player);
            click(env, navigator, player, new Click.Left(NavigatorLayout.STAGING_HUB_SLOT));
            List<Component> lore = player.getOpenInventory().getItemStack(Destination.SLENDER_STAGING.slot()).get(DataComponents.LORE);

            Assertions.assertNotNull(lore, "the staging entry has lore");
            Assertions.assertEquals("Test environment – team only", PlainTextComponentSerializer.plainText().serialize(lore.getLast()), "the last line is the team-only hint");
        }
    }

    private static void click(Env env, AbstractInventory inventory, Player player, Click click) {
        env.process().eventHandler().call(new InventoryPreClickEvent(inventory, player, click));
    }
}
