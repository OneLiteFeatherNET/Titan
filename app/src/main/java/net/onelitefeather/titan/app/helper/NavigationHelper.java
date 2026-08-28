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
import net.onelitefeather.titan.api.deliver.Deliver;
import net.onelitefeather.titan.common.feature.FeatureAudience;
import net.onelitefeather.titan.common.feature.FeatureGate;
import net.onelitefeather.titan.common.feature.TitanFeatures;
import net.onelitefeather.titan.common.navigator.BuildServerAccess;
import net.onelitefeather.titan.common.navigator.BuildServerDirectory;
import net.onelitefeather.titan.common.navigator.NavigatorEntry;
import net.onelitefeather.titan.common.navigator.NavigatorLayout;
import net.onelitefeather.titan.common.utils.Items;
import net.theevilreaper.aves.inventory.InventoryLayout;
import net.theevilreaper.aves.inventory.PersonalInventoryBuilder;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.util.LayoutCalculator;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builds and opens the navigator menu.
 *
 * <p>The menu is one row and always one row, for everyone. Which entries a player is offered is
 * decided per player: the four game modes are public, the build servers need
 * {@value BuildServerAccess#PERMISSION} (US-5.01). The slots are not fixed — they are computed
 * from the entries the player may see, so an entry that was filtered out leaves no reserved slot
 * behind and a player without the permission sees exactly the menu of a lobby that has no build
 * servers at all (US-5.02, NFR-005). See {@link NavigatorLayout}.
 *
 * <p>Both the permission and the list of reachable build servers are read while the menu is being
 * drawn, not when the helper is created, so a menu opened a second time reflects a stopped server
 * or a withdrawn permission (US-5.04).
 *
 * @author TheMeinerLP
 * @version 2.0.0
 * @since 1.15.0
 */
public class NavigationHelper {

    private static final InventoryType NAVIGATOR_TYPE = InventoryType.CHEST_1_ROW;

    /**
     * The destinations every lobby offers, each paired with the feature that releases it.
     * A destination is drawn only when {@link FeatureGate} admits its feature for the player
     * (US-3.01 to US-3.04, US-3.06) - the build servers add their own permission check on top.
     */
    private static final List<GatedEntry> PUBLIC_ENTRIES = List.of(
            new GatedEntry(TitanFeatures.NAVIGATOR_ELYTRA, NavigatorEntry.task(Items.NAVIGATOR_ELYTRA_ITEM_STACK, "ElytraRace")), new GatedEntry(TitanFeatures.NAVIGATOR_SURVIVAL, NavigatorEntry.task(Items.NAVIGATOR_SURVIVAL_ITEM_STACK, "Survival")), new GatedEntry(TitanFeatures.NAVIGATOR_SLENDER, NavigatorEntry.task(Items.NAVIGATOR_SLENDER_ITEM_STACK, "cygnus")), new GatedEntry(TitanFeatures.NAVIGATOR_CREATIVE, NavigatorEntry.task(Items.NAVIGATOR_CREATIVE_ITEM_STACK, "MemberBuild")));

    /**
     * A public destination together with the feature flag that releases it.
     *
     * @param feature the feature deciding whether the destination is offered at all
     * @param entry   the destination as the layout sees it
     */
    private record GatedEntry(TitanFeatures feature, NavigatorEntry entry) {
    }

    private final String inventoryName = "<yellow>Navigator";
    private final Deliver deliver;
    private final FeatureAudience audience;
    private final FeatureGate featureGate;
    private final BuildServerDirectory buildServers;
    private final BuildServerAccess access;

    private final LoadingCache<UUID, PersonalInventoryBuilder> inventoryBuilderLoadingCache = Caffeine.newBuilder().maximumSize(10000).expireAfterWrite(Duration.ofMinutes(5)).refreshAfterWrite(Duration.ofMinutes(1)).build(key -> createPersonalInventoryBuilder(
            MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(key)));

    private NavigationHelper(Deliver deliver, FeatureAudience audience, FeatureGate featureGate, BuildServerDirectory buildServers, BuildServerAccess access) {
        this.deliver = deliver;
        this.audience = audience;
        this.featureGate = featureGate;
        this.buildServers = buildServers;
        this.access = access;
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

    private @Nullable PersonalInventoryBuilder createPersonalInventoryBuilder(@Nullable Player player) {
        if (player == null)
            return null;
        PersonalInventoryBuilder inventoryBuilder = new PersonalInventoryBuilder(
                MiniMessage.miniMessage().deserialize(inventoryName), NAVIGATOR_TYPE, player);
        inventoryBuilder.setLayout(InventoryLayout.fromType(NAVIGATOR_TYPE));
        inventoryBuilder.setDataLayoutFunction(layout -> {
            InventoryLayout finalLayout = layout != null ? layout : InventoryLayout.fromType(NAVIGATOR_TYPE);

            // Blank the whole row first: every slot that no visible entry claims is filler, and
            // filler is what a slot holding a hidden entry would have to look like anyway.
            finalLayout.setItems(LayoutCalculator.fillRow(NAVIGATOR_TYPE), Items.NAVIGATOR_BLANK_ITEM_STACK);
            for (NavigatorLayout.Placement placement : layoutFor(player.getUuid())) {
                NavigatorEntry entry = placement.entry();
                finalLayout.setItem(placement.slot(), entry.icon(), (clicker, slot, click, itemStack, result) -> {
                    this.deliver.sendPlayer(clicker, entry.toComponent(clicker.getUuid()));
                    result.accept(ClickHolder.cancelClick());
                });
            }
            return finalLayout;
        });
        inventoryBuilder.register();
        return inventoryBuilder;
    }

    /**
     * Plans the menu for one player: which entries they are offered and where those sit.
     *
     * @param playerId the player the menu is drawn for
     * @return the visible entries with their slots, in ascending slot order
     */
    List<NavigatorLayout.Placement> layoutFor(UUID playerId) {
        return NavigatorLayout.plan(entriesFor(playerId), playerId, this.audience, NAVIGATOR_TYPE.getSize());
    }

    /**
     * Collects every entry the navigator could offer this player. The build servers are only
     * looked up for a player who holds the permission — a player who does not is not a reason to
     * ask CloudNet anything, and their menu must not depend on the answer.
     *
     * <p>What the directory reports is taken as it stands. CloudNet is asked for the services of
     * the build task by name ({@code servicesByTask}) and is the authority on which those are;
     * re-deriving the membership from the service name here could only ever subtract from that
     * answer, and would subtract everything for a task whose {@code nameSplitter} is not the one
     * Titan is configured with. Deriving membership from a name is still what
     * {@link BuildServerAccess} does for the guard, where it has to be a name, because the guard
     * must also recognise a build server that is not in this list any more.
     *
     * @param playerId the player the menu is drawn for
     * @return the public entries, followed by the reachable build servers in a stable order
     */
    private List<NavigatorEntry> entriesFor(UUID playerId) {
        List<NavigatorEntry> entries = new ArrayList<>(PUBLIC_ENTRIES.size());
        for (GatedEntry gated : PUBLIC_ENTRIES) {
            if (this.featureGate.isVisibleTo(gated.feature(), playerId)) {
                entries.add(gated.entry());
            }
        }
        if (!this.audience.hasPermission(playerId, this.access.permission())) {
            return List.copyOf(entries);
        }
        this.buildServers.reachableServices().stream().sorted().map(service -> NavigatorEntry.restrictedServer(Items.navigatorBuildServer(service), service, this.access.permission())).forEach(entries::add);
        return List.copyOf(entries);
    }

    /**
     * Creates a navigator that offers the public entries only. Used where no permission backend is
     * available, which is the safe reading of "unknown player".
     *
     * @param deliver the delivery used to move a player on click
     * @return a navigator without build servers
     */
    public static NavigationHelper instance(Deliver deliver, FeatureGate featureGate) {
        return instance(deliver, FeatureAudience.denyAll(), featureGate, BuildServerDirectory.empty(), BuildServerAccess.defaults());
    }

    /**
     * Creates a navigator that can also offer the build servers.
     *
     * @param deliver      the delivery used to move a player on click
     * @param audience     the source of permission answers, asked every time the menu is drawn
     * @param buildServers the currently reachable build servers
     * @param access       which destinations are build servers and what they require
     * @return the navigator
     */
    public static NavigationHelper instance(Deliver deliver, FeatureAudience audience, FeatureGate featureGate, BuildServerDirectory buildServers, BuildServerAccess access) {
        return new NavigationHelper(deliver, audience, featureGate, buildServers, access);
    }

}
