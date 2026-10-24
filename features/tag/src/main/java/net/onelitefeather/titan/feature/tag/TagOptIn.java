/**
 * Copyright 2025 OneLiteFeather Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.onelitefeather.titan.feature.tag;

import jakarta.inject.Singleton;
import java.util.Objects;
import java.util.UUID;
import net.onelitefeather.titan.core.settings.PlayerSettings;

/**
 * Switches a player's tag setting for {@code /tag}. The setting itself is the only source of
 * truth: the settings menu and this command write the same value, and the change listener of
 * {@link TagModule} reacts to either.
 */
@Singleton
public final class TagOptIn {

    /** What a toggle did; the command turns it into a message. */
    public enum Toggle {
        SWITCHED_ON,
        SWITCHED_OFF,
        /** The setting could not be written; it keeps its value. */
        UNAVAILABLE
    }

    private final PlayerSettings settings;

    public TagOptIn(PlayerSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
    }

    /**
     * Flips the player's choice. A write that fails leaves the stored value alone, so the player
     * is never shown a state the settings do not hold.
     */
    public Toggle toggle(UUID player) {
        boolean wasOn = this.settings.isEnabled(player, TagSettings.ENABLED);
        try {
            this.settings.set(player, TagSettings.ENABLED, !wasOn);
        } catch (RuntimeException unavailable) {
            return Toggle.UNAVAILABLE;
        }
        return wasOn ? Toggle.SWITCHED_OFF : Toggle.SWITCHED_ON;
    }
}
