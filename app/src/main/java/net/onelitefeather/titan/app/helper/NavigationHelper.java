package net.onelitefeather.titan.app.helper;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.icevizion.aves.inventory.InventoryLayout;
import de.icevizion.aves.inventory.PersonalInventoryBuilder;
import de.icevizion.aves.inventory.util.LayoutCalculator;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.inventory.condition.InventoryConditionResult;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.common.utils.Items;
import org.togglz.core.user.SimpleFeatureUser;
import org.togglz.core.user.thread.ThreadLocalUserProvider;

import java.time.Duration;
import java.util.UUID;

public class NavigationHelper {

    private final String inventoryName = "<yellow>Navigator";
    private final Deliver deliver;

    private final LoadingCache<UUID, PersonalInventoryBuilder> inventoryBuilderLoadingCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .refreshAfterWrite(Duration.ofMinutes(1))
            .build(key -> createPersonalInventoryBuilder(MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(key)));

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
        player.getInventory().setChestplate(Items.PLAYER_ELYTRA);
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

    private void clickElytra(Player player, int slot, ClickType type, InventoryConditionResult conditionResult) {
        conditionResult.setCancel(true);
        deliver.sendPlayer(player, DeliverComponent.taskBuilder().taskName("ElytraRace").player(player).build());
    }

    private void clickSurvival(Player player, int slot, ClickType type, InventoryConditionResult conditionResult) {
        conditionResult.setCancel(true);
        deliver.sendPlayer(player, DeliverComponent.taskBuilder().player(player).taskName("Survival").build());
    }

    private void clickSlender(Player player, int slot, ClickType type, InventoryConditionResult conditionResult) {
        conditionResult.setCancel(true);
        deliver.sendPlayer(player, DeliverComponent.taskBuilder().player(player).taskName("cygnus").build());
    }

    private void clickCreative(Player player, int slot, ClickType type, InventoryConditionResult conditionResult) {
        conditionResult.setCancel(true);
        deliver.sendPlayer(player, DeliverComponent.taskBuilder().player(player).taskName("MemberBuild").build());
    }

    public static NavigationHelper instance(Deliver deliver) {
        return new NavigationHelper(deliver);
    }


}
