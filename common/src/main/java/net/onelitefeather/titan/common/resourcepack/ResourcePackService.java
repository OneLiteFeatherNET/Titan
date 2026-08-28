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
import net.minestom.server.entity.Player;
import net.theevilreaper.aves.resourcepack.ResourcePackCondition;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Delivers the lobby's resource pack stack and keeps track of what every player holds.
 *
 * <p>The model is one large base pack with an identifier that never changes plus a small season
 * delta with an identifier of its own. A season change pops exactly the season identifier and
 * pushes the new one; the base pack is never touched and stays in the client cache.
 *
 * <p>Four things this class exists to get right, all of them properties of Minestom or of the
 * protocol rather than of Titan:
 * <ol>
 * <li>Packs are pushed one at a time and removed by identifier.
 * {@code ResourcePackRequest#replace(true)} is never used, because Minestom implements it as
 * pop-all-then-push: the client drops every pack and reloads twice.</li>
 * <li>{@code ConnectionManager.doConfiguration()} calls {@code packFuture.join()} with no
 * timeout before it sends {@code FinishConfigurationPacket}. A client that never answers parks
 * that configuration thread forever, so every push arms {@linkplain #armTimeout a guard} that
 * completes the future and lets the server proceed.</li>
 * <li>Geyser answers on a Bedrock player's behalf and reports success for a pack the player
 * never received. Bedrock players are therefore recognised up front and, by default, excluded
 * from delivery; their reported status is never evaluated.</li>
 * <li>Minestom tracks only the packs whose answer is still outstanding, never what a player
 * holds, so the {@link HeldPackRegistry} keeps that book.</li>
 * </ol>
 *
 * <p>With no pack configured the service is inert: every entry point returns without sending
 * anything.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class ResourcePackService implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePackService.class);

    private final HeldPackRegistry registry;
    private final BedrockDetector bedrockDetector;
    private final PackTimeoutScheduler timeoutScheduler;
    private final Map<PackSlot, ResourcePackCondition> conditions = new EnumMap<>(PackSlot.class);
    private volatile ResourcePackSettings settings;

    /**
     * Creates a service with explicit collaborators. Tests use this to substitute the detector
     * and the scheduler.
     *
     * @param settings         the settings to start with
     * @param registry         the book of packs the players hold
     * @param bedrockDetector  recognises players whose reported status must be ignored
     * @param timeoutScheduler runs the timeout guard
     */
    public ResourcePackService(ResourcePackSettings settings, HeldPackRegistry registry, BedrockDetector bedrockDetector, PackTimeoutScheduler timeoutScheduler) {
        this.settings = Objects.requireNonNull(settings, "The resource pack settings must not be null");
        this.registry = Objects.requireNonNull(registry, "The held pack registry must not be null");
        this.bedrockDetector = Objects.requireNonNull(bedrockDetector, "The bedrock detector must not be null");
        this.timeoutScheduler = Objects.requireNonNull(timeoutScheduler, "The timeout scheduler must not be null");
    }

    /**
     * Creates the service Titan runs with: a fresh registry, Floodgate detection using the
     * configured prefix and a daemon-thread timeout scheduler.
     *
     * @param settings the loaded settings
     * @return the service
     */
    public static ResourcePackService create(ResourcePackSettings settings) {
        return new ResourcePackService(settings, new HeldPackRegistry(), BedrockDetector.floodgate(settings.bedrockNamePrefix()), new DaemonPackTimeoutScheduler());
    }

    /**
     * Registers what should happen when a client reports a terminal status for a slot - for
     * instance a message on a failed download, or a kick when a required pack is declined.
     *
     * <p>The condition is Aves' {@link ResourcePackCondition}. It receives no pack identifier, so
     * one condition is registered per slot and this service routes the report to the right one.
     *
     * @param slot      the slot the condition applies to
     * @param condition the condition to run
     * @return this service
     */
    public ResourcePackService withCondition(PackSlot slot, ResourcePackCondition condition) {
        this.conditions.put(slot, condition);
        return this;
    }

    /**
     * The settings currently in effect.
     *
     * @return the settings
     */
    public ResourcePackSettings settings() {
        return this.settings;
    }

    /**
     * Whether any pack is configured. When this is {@code false} the lobby behaves exactly as a
     * lobby without the feature: no listener needs registering and nothing is ever sent.
     *
     * @return {@code true} when at least one pack is configured
     */
    public boolean enabled() {
        return this.settings.enabled();
    }

    /**
     * The book of packs the players hold.
     *
     * @return the registry
     */
    public HeldPackRegistry registry() {
        return this.registry;
    }

    /**
     * Pushes every configured pack the player does not already hold. Called from the
     * configuration phase, so the client downloads before it reaches the world.
     *
     * @param player the player entering configuration
     */
    public void onConfiguration(Player player) {
        ResourcePackSettings current = this.settings;
        if (!current.enabled()) {
            return;
        }
        boolean bedrock = this.bedrockDetector.isBedrock(player);
        if (bedrock && !current.sendToBedrockPlayers()) {
            LOGGER.debug("Skipping resource packs for the bedrock player {} - geyser would report success without delivering anything", player.getUsername());
            return;
        }
        boolean pushed = false;
        for (PackSlot slot : PackSlot.values()) {
            pushed |= this.push(player, slot, current.packFor(slot), bedrock);
        }
        if (pushed) {
            this.armTimeout(player, current.responseTimeout());
        }
    }

    /**
     * Replaces the season pack for every player who is online, and for everyone who joins from
     * now on.
     *
     * <p>Only the season identifier is popped. The base pack is never removed, so the client
     * keeps it cached and reloads just the small delta.
     *
     * @param season  the new season pack, or {@code null} to end the season without a successor
     * @param players the players currently online
     */
    public void applySeason(@Nullable ResourcePackDefinition season, Collection<Player> players) {
        ResourcePackSettings current = this.settings.withSeason(season);
        this.settings = current;
        for (Player player : players) {
            UUID playerId = player.getUuid();
            boolean bedrock = this.bedrockDetector.isBedrock(player);
            if (bedrock && !current.sendToBedrockPlayers()) {
                continue;
            }
            Optional<HeldPackRegistry.Entry> held = this.registry.entry(playerId, PackSlot.SEASON);
            if (held.isPresent() && (season == null || !held.get().packId().equals(season.id()))) {
                player.removeResourcePacks(held.get().packId());
                this.registry.forget(playerId, PackSlot.SEASON);
            }
            if (this.push(player, PackSlot.SEASON, season, bedrock)) {
                this.armTimeout(player, current.responseTimeout());
            }
        }
    }

    /**
     * Handles a client's answer to a pushed pack.
     *
     * <p>Reports from Bedrock players are dropped without being looked at: Geyser sends them on
     * the player's behalf and they say nothing about what the client actually has.
     *
     * @param player the player who answered
     * @param packId the pack the answer is about
     * @param status the reported status
     */
    public void onStatus(Player player, UUID packId, ResourcePackStatus status) {
        if (this.bedrockDetector.isBedrock(player)) {
            LOGGER.debug("Ignoring the reported pack status {} from the bedrock player {} - geyser answers on their behalf", status, player.getUsername());
            return;
        }
        UUID playerId = player.getUuid();
        PackSlot slot = this.registry.slotOf(playerId, packId);
        if (slot == null) {
            return;
        }
        if (status.intermediate()) {
            return;
        }
        if (status == ResourcePackStatus.SUCCESSFULLY_LOADED) {
            this.registry.confirm(playerId, slot);
        } else {
            this.registry.forget(playerId, slot);
        }
        ResourcePackCondition condition = this.conditions.get(slot);
        if (condition != null) {
            condition.handleStatus(player, status);
        }
    }

    /**
     * Drops everything remembered about a player.
     *
     * @param playerId the player who left
     */
    public void onDisconnect(UUID playerId) {
        this.registry.forget(playerId);
    }

    /**
     * Pushes one pack, unless it is not configured or the player already holds it.
     *
     * @param player     the receiving player
     * @param slot       the slot being filled
     * @param definition the pack to push, or {@code null} when that slot is not served
     * @param bedrock    whether the receiver is a Bedrock player
     * @return {@code true} when a push packet was sent
     */
    private boolean push(Player player, PackSlot slot, @Nullable ResourcePackDefinition definition, boolean bedrock) {
        if (definition == null) {
            return false;
        }
        UUID playerId = player.getUuid();
        if (this.registry.holds(playerId, slot, definition.id())) {
            return false;
        }
        // A required pack makes Minestom kick on any terminal status other than success. For a
        // bedrock player that verdict would rest on Geyser's invented answer, so the requirement
        // is dropped for them.
        boolean required = definition.required() && !bedrock;
        player.sendResourcePacks(definition.toRequest(required));
        this.registry.markSent(playerId, slot, definition.id());
        return true;
    }

    /**
     * Arms the guard against a client that never answers.
     *
     * <p>{@code ConnectionManager.doConfiguration()} joins the player's pack future without a
     * timeout, so nothing else can unblock that thread. The guard completes the future itself
     * once the configured time has passed; configuration then continues and the player enters
     * the lobby without the pack, which is the outcome the requirement asks for. A late answer
     * from the client is still processed afterwards - it just no longer holds anything up.
     *
     * @param player  the player that was pushed to
     * @param timeout how long to wait; a non-positive value disables the guard
     */
    private void armTimeout(Player player, Duration timeout) {
        if (timeout.isZero() || timeout.isNegative()) {
            return;
        }
        this.timeoutScheduler.schedule(() -> this.expireTimeout(player), timeout);
    }

    /**
     * Completes the player's pending pack future so a parked configuration thread proceeds.
     *
     * @param player the player whose future is released
     */
    private void expireTimeout(Player player) {
        CompletableFuture<Void> future = player.getResourcePackFuture();
        if (future == null || future.isDone()) {
            return;
        }
        LOGGER.warn("{} did not answer the resource pack request in time - continuing without it", player.getUsername());
        future.complete(null);
    }

    /**
     * Releases the timeout scheduler when the lobby shuts down.
     */
    @Override
    public void close() {
        if (this.timeoutScheduler instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception exception) {
                LOGGER.warn("Unable to stop the resource pack timeout scheduler", exception);
            }
        }
    }
}
