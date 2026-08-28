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

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers which pack sits in which slot for every player.
 *
 * <p>Minestom tracks only the packs whose answer is still outstanding
 * ({@code Player#pendingResourcePacks}) and forgets a pack the moment the client reports a
 * terminal status. Nothing in the server knows afterwards what a player actually holds - but
 * that is exactly what a season change needs, because it must pop the identifier the player was
 * given, which may be an older season than the one currently configured. So the lobby keeps this
 * book itself.
 *
 * <p>An entry is created when a pack is pushed ({@link #markSent}) and promoted when the client
 * confirms it ({@link #confirm}). Both states count as held: a pack that was pushed but never
 * answered may still be sitting in the client, and popping an identifier the client does not
 * have is harmless, while failing to pop one it does have is not.
 *
 * <p>All methods are safe to call from several threads - pushes happen on configuration threads,
 * a season change on whichever thread triggers it.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public final class HeldPackRegistry {

    private final Map<UUID, Map<PackSlot, Entry>> byPlayer = new ConcurrentHashMap<>();

    /**
     * One pack a player was given.
     *
     * @param packId    the pack identifier that was pushed
     * @param confirmed whether the client reported it as successfully loaded
     * @author TheMeinerLP
     * @version 1.0.0
     * @since 1.15.0
     */
    public record Entry(UUID packId, boolean confirmed) {
    }

    /**
     * Records that a pack was pushed to a player, replacing whatever that slot held before.
     *
     * @param playerId the player the pack was pushed to
     * @param slot     the slot the pack fills
     * @param packId   the pushed pack identifier
     */
    public void markSent(UUID playerId, PackSlot slot, UUID packId) {
        this.byPlayer.computeIfAbsent(playerId, key -> new ConcurrentHashMap<>()).put(slot, new Entry(packId, false));
    }

    /**
     * Marks the pack in a slot as confirmed by the client. Does nothing when the slot is empty.
     *
     * @param playerId the player who answered
     * @param slot     the slot that was confirmed
     */
    public void confirm(UUID playerId, PackSlot slot) {
        this.byPlayer.computeIfPresent(playerId, (key, slots) -> {
            Entry entry = slots.get(slot);
            if (entry != null) {
                slots.put(slot, new Entry(entry.packId(), true));
            }
            return slots;
        });
    }

    /**
     * Forgets the pack in a slot, because the client declined it, failed to load it or had it
     * removed.
     *
     * @param playerId the player to update
     * @param slot     the slot to clear
     */
    public void forget(UUID playerId, PackSlot slot) {
        this.byPlayer.computeIfPresent(playerId, (key, slots) -> {
            slots.remove(slot);
            return slots.isEmpty() ? null : slots;
        });
    }

    /**
     * Forgets everything about a player, for instance when they disconnect.
     *
     * @param playerId the player to drop
     */
    public void forget(UUID playerId) {
        this.byPlayer.remove(playerId);
    }

    /**
     * The pack a player holds in a slot, whether or not the client confirmed it.
     *
     * @param playerId the player to look up
     * @param slot     the slot to look up
     * @return the entry, or empty when the player holds nothing in that slot
     */
    public Optional<Entry> entry(UUID playerId, PackSlot slot) {
        Map<PackSlot, Entry> slots = this.byPlayer.get(playerId);
        return slots == null ? Optional.empty() : Optional.ofNullable(slots.get(slot));
    }

    /**
     * Whether a player already holds a specific pack, so it does not need to be pushed again.
     *
     * @param playerId the player to look up
     * @param slot     the slot to look up
     * @param packId   the pack identifier to compare against
     * @return {@code true} when that exact pack sits in that slot
     */
    public boolean holds(UUID playerId, PackSlot slot, UUID packId) {
        return this.entry(playerId, slot).filter(entry -> entry.packId().equals(packId)).isPresent();
    }

    /**
     * Resolves which slot a pack identifier belongs to for a player. Needed to route a status
     * report, which carries only the pack identifier, to the right slot.
     *
     * @param playerId the player who reported
     * @param packId   the reported pack identifier
     * @return the slot, or {@code null} when the player was never given that pack
     */
    public @Nullable PackSlot slotOf(UUID playerId, UUID packId) {
        Map<PackSlot, Entry> slots = this.byPlayer.get(playerId);
        if (slots == null) {
            return null;
        }
        for (Map.Entry<PackSlot, Entry> candidate : slots.entrySet()) {
            if (candidate.getValue().packId().equals(packId)) {
                return candidate.getKey();
            }
        }
        return null;
    }

    /**
     * The number of players the registry currently knows about. Intended for tests and
     * diagnostics.
     *
     * @return the number of tracked players
     */
    public int trackedPlayers() {
        return this.byPlayer.size();
    }
}
