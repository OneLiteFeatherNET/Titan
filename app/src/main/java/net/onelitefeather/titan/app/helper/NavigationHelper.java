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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.common.utils.Items;
import net.theevilreaper.aves.inventory.InventoryLayout;
import net.theevilreaper.aves.inventory.PersonalInventoryBuilder;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.util.LayoutCalculator;
import org.togglz.core.user.SimpleFeatureUser;
import org.togglz.core.user.thread.ThreadLocalUserProvider;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

public class NavigationHelper {

    private final String inventoryName = "<yellow>Navigator";
    private final Deliver deliver;

    private final LoadingCache<UUID, PersonalInventoryBuilder> inventoryBuilderLoadingCache = Caffeine.newBuilder().maximumSize(10000).expireAfterWrite(Duration.ofMinutes(5)).refreshAfterWrite(Duration.ofMinutes(1)).build(key -> createPersonalInventoryBuilder(
            MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(key)));

    private NavigationHelper(Deliver deliver) {
        this.deliver = deliver;
    }

    public void openNavigator(Player player) {
        PersonalInventoryBuilder personalInventoryBuilder = inventoryBuilderLoadingCache.get(player.getUuid());
        personalInventoryBuilder.invalidateDataLayout();
        personalInventoryBuilder.open();
    }

    public void setItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setItemStack(4, Items.PLAYER_TELEPORTER);
        player.getInventory().setEquipment(EquipmentSlot.CHESTPLATE, (byte) EquipmentSlot.CHESTPLATE.armorSlot(), Items.PLAYER_ELYTRA);
    }

    private PersonalInventoryBuilder createPersonalInventoryBuilder(Player player) {
        if (player == null)
            return null;
        PersonalInventoryBuilder inventoryBuilder = new PersonalInventoryBuilder(
                MiniMessage.miniMessage().deserialize(inventoryName), InventoryType.CHEST_1_ROW, player);
        inventoryBuilder.setLayout(InventoryLayout.fromType(InventoryType.CHEST_1_ROW));
        inventoryBuilder.setDataLayoutFunction(layout -> {
            InventoryLayout finalLayout = layout != null ? layout : InventoryLayout.fromType(InventoryType.CHEST_1_ROW);

            finalLayout.setItems(LayoutCalculator.fillRow(InventoryType.CHEST_1_ROW), Items.NAVIGATOR_BLANK_ITEM_STACK);
            ThreadLocalUserProvider.bind(toUser(player));
            finalLayout.setItem(0, Items.NAVIGATOR_ELYTRA_ITEM_STACK, this::clickElytra);
            finalLayout.setItem(4, Items.NAVIGATOR_SURVIVAL_ITEM_STACK, this::clickSurvival);
            finalLayout.setItem(5, Items.NAVIGATOR_SLENDER_ITEM_STACK, this::clickSlender);
            finalLayout.setItem(8, Items.NAVIGATOR_CREATIVE_ITEM_STACK, this::clickCreative);
            ThreadLocalUserProvider.release();
            return finalLayout;
        });
        inventoryBuilder.register();
        return inventoryBuilder;
    }

    private SimpleFeatureUser toUser(Player player) {
        return new SimpleFeatureUser(player.getUsername());
    }

    private void clickElytra(Player player, int slot, Click click, ItemStack itemStack, Consumer<ClickHolder> result) {
        deliver.sendPlayer(player, DeliverComponent.taskBuilder().taskName("ElytraRace").player(player).build());
        result.accept(ClickHolder.cancelClick());
    }

    private void clickSurvival(Player player, int slot, Click click, ItemStack itemStack, Consumer<ClickHolder> result) {
        deliver.sendPlayer(player, DeliverComponent.taskBuilder().player(player).taskName("Survival").build());
        result.accept(ClickHolder.cancelClick());
    }

    private void clickSlender(Player player, int slot, Click click, ItemStack itemStack, Consumer<ClickHolder> result) {
        deliver.sendPlayer(player, DeliverComponent.taskBuilder().player(player).taskName("cygnus").build());
        result.accept(ClickHolder.cancelClick());
    }

    private void clickCreative(Player player, int slot, Click click, ItemStack itemStack, Consumer<ClickHolder> result) {
        deliver.sendPlayer(player, DeliverComponent.taskBuilder().player(player).taskName("MemberBuild").build());
        result.accept(ClickHolder.cancelClick());
    }

    public static NavigationHelper instance(Deliver deliver) {
        return new NavigationHelper(deliver);
    }

}
