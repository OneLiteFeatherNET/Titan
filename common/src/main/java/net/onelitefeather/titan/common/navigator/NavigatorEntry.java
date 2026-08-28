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
package net.onelitefeather.titan.common.navigator;

import net.minestom.server.item.ItemStack;
import net.onelitefeather.deliver.DeliverComponent;
import net.onelitefeather.deliver.DeliverType;
import net.onelitefeather.titan.common.feature.FeatureAudience;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One destination the navigator can offer: the icon shown for it, where a click sends the player,
 * and the permission a player needs to be offered it at all.
 *
 * <p>An entry with no permission is public. An entry with one is invisible to everyone who does
 * not hold it — and invisible means gone, not greyed out and not a reserved slot, which is why
 * {@link NavigatorLayout} places entries only after filtering them.
 *
 * @param icon        the item shown in the menu
 * @param type        whether {@link #destination()} names a CloudNet task or a single service
 * @param destination the task or service name a click connects to
 * @param permission  the permission required to see this entry, or {@code null} when it is public
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public record NavigatorEntry(ItemStack icon, DeliverType type, String destination,
                             @Nullable String permission) {

    /**
     * Creates a public entry that connects to the best service of a CloudNet task.
     *
     * @param icon     the item shown in the menu
     * @param taskName the CloudNet task a click connects to
     * @return an entry every player sees
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static NavigatorEntry task(ItemStack icon, String taskName) {
        return new NavigatorEntry(icon, DeliverType.TASK, taskName, null);
    }

    /**
     * Creates an entry that connects to one specific CloudNet service and is only offered to
     * holders of the given permission.
     *
     * @param icon        the item shown in the menu
     * @param serviceName the CloudNet service a click connects to
     * @param permission  the permission required to see the entry
     * @return an entry restricted to holders of that permission
     */
    @Contract(value = "_, _, _ -> new", pure = true)
    public static NavigatorEntry restrictedServer(ItemStack icon, String serviceName, String permission) {
        return new NavigatorEntry(icon, DeliverType.SERVER, serviceName, permission);
    }

    /**
     * Checks whether this entry may be shown to the given player.
     *
     * @param playerId the player's unique id
     * @param audience the source of permission answers
     * @return whether the entry is public or the player holds its permission
     */
    @Contract(pure = true)
    public boolean isVisibleTo(UUID playerId, FeatureAudience audience) {
        return this.permission == null || audience.hasPermission(playerId, this.permission);
    }

    /**
     * Builds the delivery request a click on this entry produces.
     *
     * <p>The request is only a request: whether the player is actually moved is decided again by
     * {@link GuardedDeliver}, because the menu the click came from is not trustworthy evidence
     * that the player was ever allowed to see this entry (US-5.03).
     *
     * @param playerId the player to move
     * @return the component describing the requested switch
     */
    @Contract(value = "_ -> new", pure = true)
    public DeliverComponent toComponent(UUID playerId) {
        return switch (this.type) {
            case TASK ->
                DeliverComponent.taskBuilder().playerId(playerId).taskName(this.destination).build();
            case SERVER ->
                DeliverComponent.serverBuilder().playerId(playerId).serverName(this.destination).build();
        };
    }
}
