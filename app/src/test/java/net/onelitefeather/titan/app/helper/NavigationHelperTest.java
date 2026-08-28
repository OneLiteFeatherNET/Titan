/**
 * Copyright (C) 2025 OneLiteFeather Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package net.onelitefeather.titan.app.helper;

import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.testutils.DummyDeliver;
import net.onelitefeather.titan.app.testutils.TestAudience;
import net.onelitefeather.titan.app.testutils.TestFeatureGate;
import net.onelitefeather.titan.common.feature.FeatureAudience;
import net.onelitefeather.titan.common.feature.FeatureGate;
import net.onelitefeather.titan.common.feature.ReleaseStage;
import net.onelitefeather.titan.common.feature.TitanFeatures;
import net.onelitefeather.titan.common.navigator.BuildServerAccess;
import net.onelitefeather.titan.common.navigator.BuildServerDirectory;
import net.onelitefeather.titan.common.utils.Items;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MicrotusExtension.class)
class NavigationHelperTest {

    /** The material of the filler pane every slot that no visible entry claims falls back to. */
    private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

    @DisplayName("Test if the NavigationHelper is set with the correct items")
    @Test
    void testNavigationHelperIsItemsSet(Env env) {
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance(), allReleased().gate());

        Instance flatInstance = env.createFlatInstance();
        Player realPlayer = env.createPlayer(flatInstance);
        PlayerInventory realInventory = realPlayer.getInventory();
        Player player = spy(realPlayer);
        doReturn(spy(realInventory)).when(player).getInventory();

        helper.setItems(player);

        verify(player.getInventory(), atLeastOnce()).clear();
        verify(player.getInventory(), atLeastOnce()).setItemStack(4, Items.PLAYER_TELEPORTER);
        verify(player.getInventory(), atLeastOnce()).setEquipment(EquipmentSlot.CHESTPLATE, (byte) EquipmentSlot.CHESTPLATE.armorSlot(), Items.PLAYER_ELYTRA);
    }

    @DisplayName("Test if the NavigationHelper open the navigation gui")
    @Test
    void testNavigationHelperOpenNavigationGui(Env env) {
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance(), allReleased().gate());

        Instance flatInstance = env.createFlatInstance();
        Player realPlayer = env.createPlayer(flatInstance);

        helper.openNavigator(realPlayer);

        Assertions.assertNotNull(realPlayer.getOpenInventory());
    }

    @DisplayName("A team member with the permission sees the reachable build servers")
    @Test
    void testBuildServersShownToTeamMember(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        TestAudience audience = new TestAudience().grant(player.getUuid(), BuildServerAccess.PERMISSION);

        ItemStack[] contents = openWith(env, player, audience, "Build-1", "Build-2");

        Assertions.assertTrue(contains(contents, Items.navigatorBuildServer("Build-1")), "The first build server should be offered");
        Assertions.assertTrue(contains(contents, Items.navigatorBuildServer("Build-2")), "The second build server should be offered");
    }

    @DisplayName("A player without the permission sees no build server and no gap where one would be")
    @Test
    void testBuildServersHiddenWithoutPermission(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        FeatureAudience audience = FeatureAudience.denyAll();

        ItemStack[] withBuildServers = openWith(env, player, audience, "Build-1", "Build-2");
        ItemStack[] withoutBuildServers = openWith(env, player, audience);

        Assertions.assertFalse(contains(withBuildServers, Items.navigatorBuildServer("Build-1")), "A hidden build server must not be rendered");
        // NFR-005: not merely absent - indistinguishable. The menu is identical to the one of a
        // lobby that has no build servers at all, so no empty slot hints at what was removed.
        Assertions.assertArrayEquals(withoutBuildServers, withBuildServers, "The hidden entries must leave the menu unchanged");
    }

    @DisplayName("The unprivileged menu is one uninterrupted block of public entries between filler")
    @Test
    void testUnprivilegedMenuHasNoEmptySlot(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);

        ItemStack[] contents = openWith(env, player, FeatureAudience.denyAll(), "Build-1", "Build-2");

        Assertions.assertTrue(contents[2].isSimilar(Items.NAVIGATOR_ELYTRA_ITEM_STACK), "The public entries should start at slot 2");
        Assertions.assertTrue(contents[3].isSimilar(Items.NAVIGATOR_SURVIVAL_ITEM_STACK));
        Assertions.assertTrue(contents[4].isSimilar(Items.NAVIGATOR_SLENDER_ITEM_STACK));
        Assertions.assertTrue(contents[5].isSimilar(Items.NAVIGATOR_CREATIVE_ITEM_STACK));
        for (int slot : new int[]{0, 1, 6, 7, 8}) {
            Assertions.assertTrue(contents[slot].isSimilar(Items.NAVIGATOR_BLANK_ITEM_STACK), "Slot " + slot + " should hold the same filler every player sees");
        }
    }

    @DisplayName("A build server that stopped is dropped from the menu on the next open")
    @Test
    void testOnlyReachableBuildServersAreShown(Env env) {
        Instance flatInstance = env.createFlatInstance();
        Player player = env.createPlayer(flatInstance);
        TestAudience audience = new TestAudience().grant(player.getUuid(), BuildServerAccess.PERMISSION);

        ItemStack[] running = openWith(env, player, audience, "Build-1");
        ItemStack[] stopped = openWith(env, player, audience);

        Assertions.assertTrue(contains(running, Items.navigatorBuildServer("Build-1")), "A running build server should be offered");
        Assertions.assertFalse(contains(stopped, Items.navigatorBuildServer("Build-1")), "A build server that is no longer reachable must disappear (US-5.04)");
    }

    @DisplayName("A generally released destination is shown to an ordinary player")
    @Test
    void generallyReleasedEntriesAreShown(Env env) {
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance(), allReleased().gate());
        Player player = env.createPlayer(env.createFlatInstance());

        Set<Material> shown = openedMaterials(env, helper, player);

        Assertions.assertTrue(shown.contains(Material.ELYTRA), "the elytra race should be offered");
        Assertions.assertTrue(shown.contains(Material.GRASS_BLOCK), "survival should be offered");
        Assertions.assertTrue(shown.contains(Material.ENDERMAN_SPAWN_EGG), "slender should be offered");
        Assertions.assertTrue(shown.contains(Material.WOODEN_AXE), "creative should be offered");
    }

    @DisplayName("The kill switch removes the destination from the navigator, not just from /season status")
    @Test
    void killSwitchHidesTheEntry(Env env) {
        // The scenario the gate exists for: an operator writes NAVIGATOR_ELYTRA = false and
        // expects players to stop seeing the item, not merely a status line that says so.
        TestFeatureGate features = allReleased().killSwitch(TitanFeatures.NAVIGATOR_ELYTRA);
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance(), features.gate());
        Player player = env.createPlayer(env.createFlatInstance());

        Set<Material> shown = openedMaterials(env, helper, player);

        Assertions.assertFalse(shown.contains(Material.ELYTRA), "the switched-off elytra race must be gone from the menu");
        Assertions.assertTrue(shown.contains(Material.GRASS_BLOCK), "the other destinations stay");
        Assertions.assertTrue(shown.contains(Material.ENDERMAN_SPAWN_EGG), "the other destinations stay");
        Assertions.assertTrue(shown.contains(Material.WOODEN_AXE), "the other destinations stay");
    }

    @DisplayName("An internal destination is hidden from a player without the permission")
    @Test
    void internalStageHidesTheEntryFromOrdinaryPlayers(Env env) {
        TestFeatureGate features = allReleased().release(TitanFeatures.NAVIGATOR_SURVIVAL, ReleaseStage.INTERNAL);
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance(), features.gate());
        Player player = env.createPlayer(env.createFlatInstance());

        Assertions.assertFalse(openedMaterials(env, helper, player).contains(Material.GRASS_BLOCK), "an internal destination must not reach an ordinary player");
    }

    @DisplayName("An internal destination is shown to a team member")
    @Test
    void internalStageShowsTheEntryToTheTeam(Env env) {
        TestFeatureGate features = allReleased().release(TitanFeatures.NAVIGATOR_SURVIVAL, ReleaseStage.INTERNAL);
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance(), features.gate());
        Player player = env.createPlayer(env.createFlatInstance());
        features.grant(player.getUuid(), ReleaseStage.INTERNAL_PERMISSION);

        Assertions.assertTrue(openedMaterials(env, helper, player).contains(Material.GRASS_BLOCK), "a team member should be offered the internal destination");
    }

    @DisplayName("A lite destination is shown to the lite group and hidden from everyone else")
    @Test
    void liteStageFollowsTheGroup(Env env) {
        TestFeatureGate features = allReleased().release(TitanFeatures.NAVIGATOR_SLENDER, ReleaseStage.LITE);
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance(), features.gate());
        Instance instance = env.createFlatInstance();
        Player ordinary = env.createPlayer(instance);
        Player lite = env.createPlayer(instance);
        features.join(lite.getUuid(), ReleaseStage.LITE_GROUP);

        Assertions.assertFalse(openedMaterials(env, helper, ordinary).contains(Material.ENDERMAN_SPAWN_EGG), "a lite destination must stay hidden from everyone outside the group");
        Assertions.assertTrue(openedMaterials(env, helper, lite).contains(Material.ENDERMAN_SPAWN_EGG), "a member of the lite group should be offered it");
    }

    @DisplayName("A flag flipped between two opens takes effect on the second open")
    @Test
    void reopeningPicksUpAFlagChange(Env env) {
        TestFeatureGate features = allReleased();
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance(), features.gate());
        Player player = env.createPlayer(env.createFlatInstance());

        Assertions.assertTrue(openedMaterials(env, helper, player).contains(Material.ELYTRA), "the released destination is offered on the first open");

        // The per-player inventory builder is cached; the layout must not be.
        features.killSwitch(TitanFeatures.NAVIGATOR_ELYTRA);

        Assertions.assertFalse(openedMaterials(env, helper, player).contains(Material.ELYTRA), "the second open must reflect the flag change");
    }

    /** A fixture in which every navigator destination is generally released. */
    private static TestFeatureGate allReleased() {
        return TestFeatureGate.create().release(TitanFeatures.NAVIGATOR_ELYTRA, ReleaseStage.GA).release(TitanFeatures.NAVIGATOR_SURVIVAL, ReleaseStage.GA).release(TitanFeatures.NAVIGATOR_SLENDER, ReleaseStage.GA).release(TitanFeatures.NAVIGATOR_CREATIVE, ReleaseStage.GA);
    }

    /**
     * Opens the navigator and returns what the player actually sees. Aves fills the data layout on
     * the next tick ({@code InventoryBuilder.retrieveDataLayout} schedules it), so the inventory is
     * still empty right after {@code open()} - the tick is part of opening the menu.
     */
    private static ItemStack[] opened(Env env, NavigationHelper helper, Player player) {
        helper.openNavigator(player);
        Assertions.assertNotNull(player.getOpenInventory(), "the navigator should be open");
        env.tick();
        return player.getOpenInventory().getItemStacks().clone();
    }

    /**
     * Opens the navigator and reports which destinations it ended up offering, by icon material.
     *
     * <p>Slots are derived from the entries that survive filtering, so nothing here may depend on
     * a slot index: the question a gate test asks is whether a destination is in the menu at all.
     * The filler is not a destination and is dropped, so {@code contains(FILLER)} can never be the
     * accident that makes an assertion pass.
     */
    private static Set<Material> openedMaterials(Env env, NavigationHelper helper, Player player) {
        Set<Material> materials = new HashSet<>();
        for (ItemStack stack : opened(env, helper, player)) {
            if (stack != null && !stack.isAir() && stack.material() != FILLER) {
                materials.add(stack.material());
            }
        }
        return materials;
    }

    /**
     * Opens the navigator of a lobby offering the given build servers, with every public
     * destination generally released, and returns what the player actually sees.
     */
    private static ItemStack[] openWith(Env env, Player player, FeatureAudience audience, String... reachableServices) {
        BuildServerDirectory directory = () -> List.of(reachableServices);
        FeatureGate gate = allReleased().gate();
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance(), audience, gate, directory, BuildServerAccess.defaults());
        return opened(env, helper, player);
    }

    private static boolean contains(ItemStack[] contents, ItemStack expected) {
        return Arrays.stream(contents).anyMatch(stack -> stack != null && stack.isSimilar(expected));
    }

    // @Disabled
    // @DisplayName("Test if clicked on the teleporter item the navigation gui is
    // opened")
    // @Test
    // void testNavigationHelperOpenNavigationGuiByClick(Env env) {
    // Deliver deliver = spy(DummyDeliver.instance());
    // NavigationHelper helper = NavigationHelper.instance(deliver);
    //
    // Instance flatInstance = env.createFlatInstance();
    // Player realPlayer = env.createPlayer(flatInstance);
    //
    // helper.setItems(realPlayer);
    // helper.openNavigator(realPlayer);
    // System.out.println(realPlayer.getOpenInventory().getWindowId());
    //
    // leftClickOpenInventory(realPlayer, 0, Items.NAVIGATOR_ELYTRA_ITEM_STACK);
    // verify(deliver, atLeastOnce()).sendPlayer(any(), any());
    // leftClickOpenInventory(realPlayer, 3, Items.NAVIGATOR_SLENDER_ITEM_STACK);
    // leftClickOpenInventory(realPlayer, 4, Items.NAVIGATOR_SURVIVAL_ITEM_STACK);
    // leftClickOpenInventory(realPlayer, 5, Items.NAVIGATOR_SLENDER_ITEM_STACK);
    // leftClickOpenInventory(realPlayer, 8, Items.NAVIGATOR_CREATIVE_ITEM_STACK);
    // env.tick();
    //
    //
    // }
    //
    // private void leftClickOpenInventory(Player player, int slot, ItemStack
    // clickedItem) {
    // _leftClick(player.getOpenInventory(), true, player, slot, clickedItem);
    // }
    // private void _leftClick(AbstractInventory openInventory, boolean
    // clickOpenInventory, Player player, int slot, ItemStack clickedItem) {
    // final byte windowId = openInventory != null ? openInventory.getWindowId() :
    // 0;
    // if (clickOpenInventory) {
    // assert openInventory != null;
    // // Do not touch slot
    // } else {
    // int offset = openInventory != null ? openInventory.getInnerSize() : 0;
    // slot = PlayerInventoryUtils.convertMinestomSlotToPlayerInventorySlot(slot);
    // if (openInventory != null) {
    // slot = slot - 9 + offset;
    // }
    // }
    // player.addPacketToQueue(new ClientClickWindowPacket(windowId, 0, (short)
    // slot, (byte) 0,
    // ClientClickWindowPacket.ClickType.PICKUP, Map.of(), clickedItem));
    // player.interpretPacketQueue();
    // }
}
