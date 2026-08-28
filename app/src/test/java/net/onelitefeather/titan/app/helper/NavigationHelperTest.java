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
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.app.testutils.DummyDeliver;
import net.onelitefeather.titan.app.testutils.TestFeatureGate;
import net.onelitefeather.titan.common.feature.ReleaseStage;
import net.onelitefeather.titan.common.utils.Items;
import net.onelitefeather.titan.common.utils.TitanFeatures;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MicrotusExtension.class)
class NavigationHelperTest {

    private static final int SLOT_ELYTRA = 0;
    private static final int SLOT_SURVIVAL = 4;
    private static final int SLOT_SLENDER = 5;
    private static final int SLOT_CREATIVE = 8;

    /** A fixture in which every navigator destination is generally released. */
    private static TestFeatureGate allReleased() {
        return TestFeatureGate.create().release(TitanFeatures.NAVIGATOR_ELYTRA, ReleaseStage.GA).release(TitanFeatures.NAVIGATOR_SURVIVAL, ReleaseStage.GA).release(TitanFeatures.NAVIGATOR_SLENDER, ReleaseStage.GA).release(TitanFeatures.NAVIGATOR_CREATIVE, ReleaseStage.GA);
    }

    /**
     * Opens the navigator and reports the material a slot ended up showing.
     *
     * <p>Aves applies the data layout on the next tick ({@code InventoryBuilder.retrieveDataLayout}
     * schedules it), so the inventory is still empty right after {@code open()} - the tick is what
     * makes this assert against what a player actually sees.
     */
    private static Material openedSlot(Env env, NavigationHelper helper, Player player, int slot) {
        helper.openNavigator(player);
        Assertions.assertNotNull(player.getOpenInventory(), "the navigator should be open");
        env.tick();
        return player.getOpenInventory().getItemStack(slot).material();
    }

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

    @DisplayName("A generally released destination is shown to an ordinary player")
    @Test
    void generallyReleasedEntriesAreShown(Env env) {
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance(), allReleased().gate());
        Player player = env.createPlayer(env.createFlatInstance());

        assertEquals(Material.ELYTRA, openedSlot(env, helper, player, SLOT_ELYTRA));
        assertEquals(Material.GRASS_BLOCK, openedSlot(env, helper, player, SLOT_SURVIVAL));
        assertEquals(Material.ENDERMAN_SPAWN_EGG, openedSlot(env, helper, player, SLOT_SLENDER));
        assertEquals(Material.WOODEN_AXE, openedSlot(env, helper, player, SLOT_CREATIVE));
    }

    @DisplayName("The kill switch removes the destination from the navigator, not just from /season status")
    @Test
    void killSwitchHidesTheEntry(Env env) {
        // The scenario the gate exists for: an operator writes NAVIGATOR_ELYTRA = false and
        // expects players to stop seeing the item, not merely a status line that says so.
        TestFeatureGate features = allReleased().killSwitch(TitanFeatures.NAVIGATOR_ELYTRA);
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance(), features.gate());
        Player player = env.createPlayer(env.createFlatInstance());

        assertEquals(Material.GRAY_STAINED_GLASS_PANE, openedSlot(env, helper, player, SLOT_ELYTRA), "the elytra slot must fall back to the filler pane");
        assertEquals(Material.GRASS_BLOCK, openedSlot(env, helper, player, SLOT_SURVIVAL), "the other destinations keep their slots");
        assertEquals(Material.ENDERMAN_SPAWN_EGG, openedSlot(env, helper, player, SLOT_SLENDER));
        assertEquals(Material.WOODEN_AXE, openedSlot(env, helper, player, SLOT_CREATIVE));
    }

    @DisplayName("An internal destination is hidden from a player without the permission")
    @Test
    void internalStageHidesTheEntryFromOrdinaryPlayers(Env env) {
        TestFeatureGate features = allReleased().release(TitanFeatures.NAVIGATOR_SURVIVAL, ReleaseStage.INTERNAL);
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance(), features.gate());
        Player player = env.createPlayer(env.createFlatInstance());

        assertEquals(Material.GRAY_STAINED_GLASS_PANE, openedSlot(env, helper, player, SLOT_SURVIVAL));
    }

    @DisplayName("An internal destination is shown to a team member")
    @Test
    void internalStageShowsTheEntryToTheTeam(Env env) {
        TestFeatureGate features = allReleased().release(TitanFeatures.NAVIGATOR_SURVIVAL, ReleaseStage.INTERNAL);
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance(), features.gate());
        Player player = env.createPlayer(env.createFlatInstance());
        features.grant(player.getUuid(), ReleaseStage.INTERNAL_PERMISSION);

        assertEquals(Material.GRASS_BLOCK, openedSlot(env, helper, player, SLOT_SURVIVAL));
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

        assertEquals(Material.GRAY_STAINED_GLASS_PANE, openedSlot(env, helper, ordinary, SLOT_SLENDER));
        assertEquals(Material.ENDERMAN_SPAWN_EGG, openedSlot(env, helper, lite, SLOT_SLENDER));
    }

    @DisplayName("A flag flipped between two opens takes effect on the second open")
    @Test
    void reopeningPicksUpAFlagChange(Env env) {
        TestFeatureGate features = allReleased();
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance(), features.gate());
        Player player = env.createPlayer(env.createFlatInstance());

        assertEquals(Material.ELYTRA, openedSlot(env, helper, player, SLOT_ELYTRA));

        // The per-player inventory builder is cached; the layout must not be.
        features.killSwitch(TitanFeatures.NAVIGATOR_ELYTRA);

        assertEquals(Material.GRAY_STAINED_GLASS_PANE, openedSlot(env, helper, player, SLOT_ELYTRA));
    }
}
