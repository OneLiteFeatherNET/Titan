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

import java.time.Duration;

/**
 * Everything the lobby needs to deliver resource packs, as it is read from
 * {@code resource-packs.json}.
 *
 * <p>Both packs are optional. With neither configured the settings are
 * {@linkplain #enabled() disabled} and nothing is ever pushed - that is the state of a lobby
 * without a pack server, and it must stay indistinguishable from a lobby that never knew about
 * resource packs.
 *
 * @param base                  the long-lived base pack, or {@code null} when none is served
 * @param season                the current season delta pack, or {@code null} when none is served
 * @param responseTimeoutMillis how long to wait for a client's answer before continuing;
 *                              {@code 0} (or absent) selects {@link #DEFAULT_RESPONSE_TIMEOUT},
 *                              a negative value disables the guard
 * @param sendToBedrockPlayers  whether Bedrock players receive the packs at all; defaults to
 *                              {@code false}, because Geyser reports success for packs the
 *                              player never received
 * @param bedrockNamePrefix     the Floodgate name prefix used to recognise Bedrock players;
 *                              {@code null} selects {@link #DEFAULT_BEDROCK_NAME_PREFIX}, an
 *                              empty string disables prefix matching
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public record ResourcePackSettings(@Nullable ResourcePackDefinition base,
                                   @Nullable ResourcePackDefinition season,
                                   long responseTimeoutMillis,
                                   boolean sendToBedrockPlayers, String bedrockNamePrefix) {

    /** Applied when the configuration leaves the timeout unset. */
    public static final Duration DEFAULT_RESPONSE_TIMEOUT = Duration.ofSeconds(10);

    /** Floodgate's default prefix for Bedrock player names. */
    public static final String DEFAULT_BEDROCK_NAME_PREFIX = ".";

    private static final ResourcePackSettings DISABLED = new ResourcePackSettings(null, null, 0L, false, DEFAULT_BEDROCK_NAME_PREFIX);

    /**
     * Fills in the defaults for values a configuration file may legitimately omit.
     */
    public ResourcePackSettings {
        if (responseTimeoutMillis == 0L) {
            responseTimeoutMillis = DEFAULT_RESPONSE_TIMEOUT.toMillis();
        }
        if (bedrockNamePrefix == null) {
            bedrockNamePrefix = DEFAULT_BEDROCK_NAME_PREFIX;
        }
    }

    /**
     * The settings of a lobby without any configured pack: nothing is delivered.
     *
     * @return settings that make the whole feature inert
     */
    public static ResourcePackSettings disabled() {
        return DISABLED;
    }

    /**
     * Whether any pack is configured at all.
     *
     * @return {@code true} when at least one pack would be delivered
     */
    public boolean enabled() {
        return this.base != null || this.season != null;
    }

    /**
     * The pack configured for a slot.
     *
     * @param slot the slot to look up
     * @return the pack, or {@code null} when that slot is not served
     */
    public @Nullable ResourcePackDefinition packFor(PackSlot slot) {
        return switch (slot) {
            case BASE -> this.base;
            case SEASON -> this.season;
        };
    }

    /**
     * How long a client may take to answer a pack push before the lobby stops waiting.
     *
     * @return the timeout; {@linkplain Duration#isNegative() negative} when the guard is off
     */
    public Duration responseTimeout() {
        return Duration.ofMillis(this.responseTimeoutMillis);
    }

    /**
     * The same settings with a different season pack. Used when a season turns; the base pack
     * and every other value stay untouched.
     *
     * @param season the new season pack, or {@code null} to serve none
     * @return the updated settings
     */
    public ResourcePackSettings withSeason(@Nullable ResourcePackDefinition season) {
        return new ResourcePackSettings(this.base, season, this.responseTimeoutMillis, this.sendToBedrockPlayers, this.bedrockNamePrefix);
    }
}
