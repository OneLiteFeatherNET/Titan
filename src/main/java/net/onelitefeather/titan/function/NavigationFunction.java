package net.onelitefeather.titan.function;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.icevizion.aves.inventory.InventoryLayout;
import de.icevizion.aves.inventory.PersonalInventoryBuilder;
import de.icevizion.aves.inventory.util.LayoutCalculator;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.inventory.condition.InventoryConditionResult;
import net.minestom.server.item.ItemStack;
import net.onelitefeather.titan.deliver.Deliver;
import net.onelitefeather.titan.utils.TitanFeatures;
import org.togglz.core.user.thread.ThreadLocalUserProvider;

import java.time.Duration;
import java.util.UUID;

public final class NavigationFunction implements TitanFunction{
    private final String inventoryName = "<yellow>Navigator";
    private final Deliver deliver;

    private final LoadingCache<UUID, PersonalInventoryBuilder> inventoryBuilderLoadingCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .refreshAfterWrite(Duration.ofMinutes(1))
            .build(key -> createPersonalInventoryBuilder(MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(key)));

    private NavigationFunction(Deliver deliver, EventNode<Event> node) {
        this.deliver = deliver;
        node.addListener(PlayerUseItemEvent.class, this::playerUseItemEvent);
    }

    private PersonalInventoryBuilder createPersonalInventoryBuilder(Player player) {
        if (player == null) return null;
        PersonalInventoryBuilder inventoryBuilder = new PersonalInventoryBuilder(
                MiniMessage.miniMessage().deserialize(inventoryName),
                InventoryType.CHEST_1_ROW,
                player
        );
        inventoryBuilder.setLayout(InventoryLayout.fromType(InventoryType.CHEST_1_ROW));
        inventoryBuilder.setDataLayoutFunction(layout -> {
            InventoryLayout finalLayout = layout != null ? layout : InventoryLayout.fromType(InventoryType.CHEST_1_ROW);
            finalLayout.setNonClickItems(LayoutCalculator.fillRow(InventoryType.CHEST_1_ROW), ItemModule.NAVIGATOR_BLANK_ITEM_STACK);
            ThreadLocalUserProvider.bind(toUser(player));
            if (TitanFeatures.NAVIGATOR_ELYTRA.isActive()) {
                finalLayout.setItem(0, ItemModule.NAVIGATOR_ELYTRA_ITEM_STACK, this::clickElytra);
            }
            if (TitanFeatures.SLENDER.isActive()) {
                finalLayout.setItem(3,ItemModule.NAVIGATOR_SLENDER_ITEM_STACK, this::clickSlender);
            }
            if (TitanFeatures.NAVIGATOR_SURVIVAL .isActive()) {
                finalLayout.setItem(4, ItemModule.NAVIGATOR_SURVIVAL_ITEM_STACK, this::clickSurvival);
            }
            if (TitanFeatures.MANIS.isActive()) {
                finalLayout.setItem(5, ItemModule.NAVIGATOR_SLENDER_ITEM_STACK, this::clickSurvival);
            }
            if (TitanFeatures.NAVIGATOR_CREATIVE.isActive()) {
                finalLayout.setItem(8, ItemModule.NAVIGATOR_CREATIVE_ITEM_STACK, this::clickCreative);
            }
            ThreadLocalUserProvider.release();
            return finalLayout;
        });
        inventoryBuilder.register();
        return inventoryBuilder;
    }

    public void setItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setItemStack(4, ItemModule.PLAYER_TELEPORTER);
        player.getInventory().setChestplate(ItemModule.PLAYER_ELYTRA);
    }

    private void clickElytra(Player player, int slot, ClickType type, InventoryConditionResult conditionResult) {
        conditionResult.setCancel(true);
        deliver.sendPlayer(player, "ElytraRace");
    }

    private void clickSurvival(Player player, int slot, ClickType type, InventoryConditionResult conditionResult) {
        conditionResult.setCancel(true);
        deliver.sendPlayer(player, "Survival");
    }

    private void clickSlender(Player player, int slot, ClickType type, InventoryConditionResult conditionResult) {
        conditionResult.setCancel(true);
        deliver.sendPlayer(player, "Slender");
    }

    private void clickCreative(Player player, int slot, ClickType type, InventoryConditionResult conditionResult) {
        conditionResult.setCancel(true);
        deliver.sendPlayer(player, "MemberBuild");
    }

    private void playerUseItemEvent(PlayerUseItemEvent event) {
        ItemStack item = event.getItemStack();
        if (item.isSimilar(ItemModule.PLAYER_TELEPORTER)) {
            PersonalInventoryBuilder personalInventoryBuilder = inventoryBuilderLoadingCache.get(event.getPlayer().getUuid());
            personalInventoryBuilder.invalidateDataLayout();
            personalInventoryBuilder.open();
        }
    }

    public static NavigationFunction instance(Deliver deliver, EventNode<Event> node) {
        return new NavigationFunction(deliver, node);
    }


}
