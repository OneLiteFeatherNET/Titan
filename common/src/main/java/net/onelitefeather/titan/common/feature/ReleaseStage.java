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
package net.onelitefeather.titan.common.feature;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * The audience a feature has been released to. The stages widen in one direction only:
 * {@link #INTERNAL} → {@link #LITE} → {@link #GA}. Every stage includes the audience of the
 * stages before it, so a team member keeps seeing a feature when it moves on to lite players.
 *
 * <p>The stage of a feature is stored as the Togglz feature-state parameter
 * {@value FeatureGate#STAGE_PARAMETER}; a feature without that parameter is treated as
 * {@link #DEFAULT}, which is the narrowest audience rather than the widest.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.15.0
 */
public enum ReleaseStage {

    /** Only holders of {@value #INTERNAL_PERMISSION} see the feature. */
    INTERNAL("internal"),

    /** Members of the {@value #LITE_GROUP} group see the feature, and so does the team. */
    LITE("lite"),

    /** Every player sees the feature. */
    GA("ga");

    /** Permission that marks a team member allowed to see features under internal test. */
    public static final String INTERNAL_PERMISSION = "titan.feature.internal";

    /** LuckPerms group whose members get early access at stage {@link #LITE}. */
    public static final String LITE_GROUP = "lite";

    /** Stage assumed for a feature whose stage parameter is missing or unreadable. */
    public static final ReleaseStage DEFAULT = INTERNAL;

    private final String id;

    ReleaseStage(String id) {
        this.id = id;
    }

    /**
     * Resolves the stage written in a flag file.
     *
     * @param id the configured stage id, may be {@code null} when the parameter is absent
     * @return the matching stage, or an empty optional when the id is absent or unknown
     */
    @Contract(pure = true)
    public static Optional<ReleaseStage> fromId(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (ReleaseStage stage : values()) {
            if (stage.id.equals(normalized)) {
                return Optional.of(stage);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the id used in the flag file, for example {@code lite}.
     *
     * @return the configured id of this stage
     */
    @Contract(pure = true)
    public String id() {
        return this.id;
    }

    /**
     * Checks whether a player belongs to the audience of this stage.
     *
     * @param playerId the player's unique id
     * @param audience the source of permission and group answers
     * @return whether the player is part of this stage's audience
     */
    @Contract(pure = true)
    public boolean admits(UUID playerId, FeatureAudience audience) {
        return switch (this) {
            case GA -> true;
            case LITE ->
                audience.hasPermission(playerId, INTERNAL_PERMISSION) || audience.inGroup(playerId, LITE_GROUP);
            case INTERNAL -> audience.hasPermission(playerId, INTERNAL_PERMISSION);
        };
    }
}
