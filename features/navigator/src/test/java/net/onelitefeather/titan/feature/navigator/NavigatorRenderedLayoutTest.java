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
import java.util.stream.IntStream;
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
 * The menu as a player sees it: four rows, the dark frame, the entries centred in their rows and an
 * empty inside wherever no entry is shown.
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

    private static boolean contains(AbstractInventory inventory, Material material) {
        return IntStream.range(0, inventory.getSize()).anyMatch(slot -> inventory.getItemStack(slot).material() == material);
    }

    @DisplayName("The navigator has four rows of nine slots")
    @Test
    void fourRows(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService())) {
            Assertions.assertEquals(36, openNavigator(fixture, player).getSize(), "four rows of nine slots");
        }
    }

    @DisplayName("The frame is one dark pane without a name, and the inside is empty")
    @Test
    void frameIsOneDarkPaneAndTheInsideIsEmpty(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService())) {
            AbstractInventory menu = openNavigator(fixture, player);
            Assertions.assertEquals(Material.BLACK_STAINED_GLASS_PANE, menu.getItemStack(0).material(), "the top-left corner is the dark pane");
            Assertions.assertEquals(Material.BLACK_STAINED_GLASS_PANE, menu.getItemStack(1).material(), "the next frame pane is the same dark pane");
            Assertions.assertEquals("", nameOf(menu, 0), "a frame pane has no name");
            Assertions.assertEquals(Material.AIR, menu.getItemStack(10).material(), "an empty inner slot holds no item");
            Assertions.assertFalse(contains(menu, Material.GRAY_STAINED_GLASS_PANE), "no grey filler pane is shown");
        }
    }

    @DisplayName("ElytraRace alone sits on slot 13, Survival on 21 and Creative on 22")
    @Test
    void entriesSitOnTheirSlots(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService())) {
            AbstractInventory menu = openNavigator(fixture, player);
            Assertions.assertEquals(Material.ELYTRA, menu.getItemStack(13).material(), "ElytraRace is alone in row one, on slot 13");
            Assertions.assertEquals(Material.GRASS_BLOCK, menu.getItemStack(21).material(), "Survival is on slot 21");
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

    @DisplayName("Without the permissions Slender and Build their entries are absent and leave no gap")
    @Test
    void hiddenEntriesLeaveNoItem(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService())) {
            AbstractInventory menu = openNavigator(fixture, player);
            Assertions.assertFalse(contains(menu, Material.ENDERMAN_SPAWN_EGG), "Slender is hidden");
            Assertions.assertEquals(Material.AIR, menu.getItemStack(23).material(), "Build is hidden, so slot 23 stays empty");
        }
    }

    @DisplayName("With the Slender permission Slender shows on slot 13 beside ElytraRace on slot 12")
    @Test
    void grantedSlenderShowsBesideElytraRace(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), SLENDER_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            AbstractInventory menu = openNavigator(fixture, player);
            Assertions.assertEquals(Material.ELYTRA, menu.getItemStack(12).material(), "ElytraRace is on slot 12");
            Assertions.assertEquals(Material.ENDERMAN_SPAWN_EGG, menu.getItemStack(13).material(), "a granted Slender is on slot 13");
        }
    }

    @DisplayName("With the Bounce and Slender permissions the minigames are ElytraRace, Bounce and Slender on slots 12 to 14")
    @Test
    void grantedBounceSitsBetweenRaceAndSlender(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BOUNCE_PERMISSION, PermissionResult.ALLOWED).set(player.getUuid(), SLENDER_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            AbstractInventory menu = openNavigator(fixture, player);

            Assertions.assertEquals(Material.ELYTRA, menu.getItemStack(12).material(), "ElytraRace is on slot 12");
            Assertions.assertEquals(Material.SLIME_BALL, menu.getItemStack(13).material(), "a granted Bounce is on slot 13");
            Assertions.assertEquals(Material.ENDERMAN_SPAWN_EGG, menu.getItemStack(14).material(), "Slender is on slot 14");
        }
    }

    @DisplayName("Without the Bounce permission ElytraRace takes slot 13 and no Bounce shows")
    @Test
    void hiddenBounceLeavesNoBounceItem(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService())) {
            AbstractInventory menu = openNavigator(fixture, player);
            Assertions.assertEquals(Material.ELYTRA, menu.getItemStack(13).material(), "ElytraRace moves to slot 13 without Bounce");
            Assertions.assertFalse(contains(menu, Material.SLIME_BALL), "a hidden Bounce shows nothing");
        }
    }

    @DisplayName("With the Build permission Build shows on slot 23")
    @Test
    void grantedBuildShowsOnSlot23(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), BUILD_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            Assertions.assertEquals(Material.SCAFFOLDING, openNavigator(fixture, player).getItemStack(23).material(), "a granted Build is on slot 23");
        }
    }

    @DisplayName("Without cygnus.staging.join the hub corner keeps the dark frame pane when the permission is not set")
    @Test
    void notSetStagingLeavesTheFramePaneOnSlot35(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), new FakePermissionService())) {
            Assertions.assertEquals(Material.BLACK_STAINED_GLASS_PANE, openNavigator(fixture, player).getItemStack(NavigatorLayout.STAGING_HUB_SLOT).material(), "an unset staging grant leaves slot 35 to the frame");
        }
    }

    @DisplayName("Without cygnus.staging.join the hub corner keeps the dark frame pane when the permission is denied")
    @Test
    void deniedStagingLeavesTheFramePaneOnSlot35(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), STAGING_PERMISSION, PermissionResult.DENIED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            Assertions.assertEquals(Material.BLACK_STAINED_GLASS_PANE, openNavigator(fixture, player).getItemStack(NavigatorLayout.STAGING_HUB_SLOT).material(), "a denied staging grant leaves slot 35 to the frame");
        }
    }

    @DisplayName("With cygnus.staging.join the staging hub shows on slot 35 as a structure block named Staging")
    @Test
    void grantedStagingShowsTheHubOnSlot35(Env env) {
        Player player = env.createPlayer(env.createFlatInstance());
        FakePermissionService permissions = new FakePermissionService().set(player.getUuid(), STAGING_PERMISSION, PermissionResult.ALLOWED);
        try (NavigatorFixture fixture = NavigatorFixture.start(env, new RecordingDeliver(), permissions)) {
            AbstractInventory menu = openNavigator(fixture, player);

            Assertions.assertEquals(Material.STRUCTURE_BLOCK, menu.getItemStack(NavigatorLayout.STAGING_HUB_SLOT).material(), "a granted staging permission shows the hub as a structure block on slot 35");
            Assertions.assertEquals("Staging", nameOf(menu, NavigatorLayout.STAGING_HUB_SLOT), "the hub is named Staging");
            Assertions.assertFalse(contains(menu, Material.ENDERMAN_SPAWN_EGG), "the real Slender stays hidden without its own grant");
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
