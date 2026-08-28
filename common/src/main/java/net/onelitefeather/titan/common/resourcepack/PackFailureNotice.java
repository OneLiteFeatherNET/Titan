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
package net.onelitefeather.titan.common.resourcepack;

import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.entity.Player;
import net.theevilreaper.aves.resourcepack.ResourcePackCondition;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Tells a player that a resource pack did not make it, instead of leaving them to wonder why the
 * lobby looks wrong.
 *
 * <p>This is the condition {@link ResourcePackService#withCondition} exists for. Only the three
 * statuses that mean "the pack is broken or unreachable" produce a message:
 * {@link ResourcePackStatus#FAILED_DOWNLOAD}, {@link ResourcePackStatus#INVALID_URL} and
 * {@link ResourcePackStatus#FAILED_RELOAD}. They are also logged, because all three point at the
 * pack server rather than at the player.
 *
 * <p>{@link ResourcePackStatus#DECLINED} is deliberately silent: the player chose it, and for a
 * required pack Minestom has already kicked them by the time this runs. Nagging someone about a
 * choice they just made is not a notice, it is noise.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class PackFailureNotice implements ResourcePackCondition {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackFailureNotice.class);

    private static final String DEFAULT_MESSAGE = "<gray>The <yellow><slot></yellow> resource pack could not be loaded. The lobby works without it, but you will not see its textures.";

    private final PackSlot slot;
    private final Component message;

    /**
     * Creates the notice for one slot.
     *
     * @param slot the slot this notice reports on
     */
    public PackFailureNotice(PackSlot slot) {
        this(slot, MiniMessage.miniMessage().deserialize(DEFAULT_MESSAGE.replace("<slot>", slot.name().toLowerCase(Locale.ROOT))));
    }

    /**
     * Creates the notice with a message of its own.
     *
     * @param slot    the slot this notice reports on
     * @param message the message sent to the player
     */
    public PackFailureNotice(PackSlot slot, Component message) {
        this.slot = slot;
        this.message = message;
    }

    @Override
    public void handleStatus(@NotNull Player player, @NotNull ResourcePackStatus resourcePackStatus) {
        switch (resourcePackStatus) {
            case FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD -> {
                LOGGER.warn("The {} resource pack failed for {} with {} - check that the configured url is reachable and serves the configured hash", this.slot, player.getUsername(), resourcePackStatus);
                player.sendMessage(this.message);
            }
            default -> {
                // Nothing to report: the pack loaded, or the player declined it on purpose.
            }
        }
    }
}
