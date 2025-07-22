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
package net.onelitefeather.titan.app.helper;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
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

    private ClickHolder clickElytra(Player player, int slot, Click click) {
        deliver.sendPlayer(player, DeliverComponent.taskBuilder().taskName("ElytraRace").player(player).build());
        return ClickHolder.cancelClick();
    }

    private ClickHolder clickSurvival(Player player, int slot, Click click) {
        deliver.sendPlayer(player, DeliverComponent.taskBuilder().player(player).taskName("Survival").build());
        return ClickHolder.cancelClick();
    }

    private ClickHolder clickSlender(Player player, int slot, Click click) {
        deliver.sendPlayer(player, DeliverComponent.taskBuilder().player(player).taskName("cygnus").build());
        return ClickHolder.cancelClick();
    }

    private ClickHolder clickCreative(Player player, int slot, Click click) {
        deliver.sendPlayer(player, DeliverComponent.taskBuilder().player(player).taskName("MemberBuild").build());
        return ClickHolder.cancelClick();
    }

    public static NavigationHelper instance(Deliver deliver) {
        return new NavigationHelper(deliver);
    }

}
