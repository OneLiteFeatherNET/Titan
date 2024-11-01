package net.onelitefeather.titan.app.helper;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.api.deliver.model.DeliverComponent;
import net.onelitefeather.titan.app.testutils.DummyDeliver;
import net.onelitefeather.titan.common.utils.Items;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MicrotusExtension.class)
class NavigationHelperTest {

    @DisplayName("Test if the NavigationHelper is set with the correct items")
    @Test
    void testNavigationHelperIsItemsSet(Env env) {
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance());

        Instance flatInstance = env.createFlatInstance();
        Player realPlayer = env.createPlayer(flatInstance);
        PlayerInventory realInventory = realPlayer.getInventory();
        Player player = spy(realPlayer);
        doReturn(spy(realInventory)).when(player).getInventory();

        helper.setItems(player);

        verify(player.getInventory(), atLeastOnce()).clear();
        verify(player.getInventory(), atLeastOnce()).setItemStack(4, Items.PLAYER_TELEPORTER);
        verify(player.getInventory(), atLeastOnce()).setChestplate(Items.PLAYER_ELYTRA);
    }

    @DisplayName("Test if the NavigationHelper open the navigation gui")
    @Test
    void testNavigationHelperOpenNavigationGui(Env env) {
        NavigationHelper helper = NavigationHelper.instance(DummyDeliver.instance());

        Instance flatInstance = env.createFlatInstance();
        Player realPlayer = env.createPlayer(flatInstance);

        helper.openNavigator(realPlayer);

        Assertions.assertNotNull(realPlayer.getOpenInventory());
    }

    @Disabled
    @DisplayName("Test if clicked on the teleporter item the navigation gui is opened")
    @Test
    void testNavigationHelperOpenNavigationGuiByClick(Env env) {
        Deliver deliver = spy(DummyDeliver.instance());
        NavigationHelper helper = NavigationHelper.instance(deliver);

        Instance flatInstance = env.createFlatInstance();
        Player realPlayer = env.createPlayer(flatInstance);

        helper.setItems(realPlayer);
        helper.openNavigator(realPlayer);
        System.out.println(realPlayer.getOpenInventory().getWindowId());

        leftClickOpenInventory(realPlayer, 0, Items.NAVIGATOR_ELYTRA_ITEM_STACK);
        verify(deliver, atLeastOnce()).sendPlayer(any(), any());
        leftClickOpenInventory(realPlayer, 3, Items.NAVIGATOR_SLENDER_ITEM_STACK);
        leftClickOpenInventory(realPlayer, 4, Items.NAVIGATOR_SURVIVAL_ITEM_STACK);
        leftClickOpenInventory(realPlayer, 5, Items.NAVIGATOR_SLENDER_ITEM_STACK);
        leftClickOpenInventory(realPlayer, 8, Items.NAVIGATOR_CREATIVE_ITEM_STACK);
        env.tick();


    }

    private void leftClickOpenInventory(Player player, int slot, ItemStack clickedItem) {
        _leftClick(player.getOpenInventory(), true, player, slot, clickedItem);
    }
    private void _leftClick(Inventory openInventory, boolean clickOpenInventory, Player player, int slot, ItemStack clickedItem) {
        final byte windowId = openInventory != null ? openInventory.getWindowId() : 0;
        if (clickOpenInventory) {
            assert openInventory != null;
            // Do not touch slot
        } else {
            int offset = openInventory != null ? openInventory.getInnerSize() : 0;
            slot = PlayerInventoryUtils.convertToPacketSlot(slot);
            if (openInventory != null) {
                slot = slot - 9 + offset;
            }
        }
        player.addPacketToQueue(new ClientClickWindowPacket(windowId, 0, (short) slot, (byte) 0,
                ClientClickWindowPacket.ClickType.PICKUP, List.of(), clickedItem));
        player.interpretPacketQueue();
    }

}
