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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePackSettingsTest {

    private static final String HASH = "0123456789abcdef0123456789abcdef01234567";

    private static ResourcePackDefinition pack() {
        return new ResourcePackDefinition(UUID.randomUUID(), "https://packs.example/pack-" + HASH + ".zip", HASH, false, null);
    }

    @Test
    @DisplayName("Without any pack the settings are disabled")
    void testDisabled() {
        ResourcePackSettings settings = ResourcePackSettings.disabled();

        assertFalse(settings.enabled());
        assertNull(settings.packFor(PackSlot.BASE));
        assertNull(settings.packFor(PackSlot.SEASON));
    }

    @Test
    @DisplayName("A season pack alone is enough to enable delivery")
    void testSeasonOnlyIsEnabled() {
        ResourcePackSettings settings = new ResourcePackSettings(null, pack(), 0L, false, ".");

        assertTrue(settings.enabled());
    }

    @Test
    @DisplayName("An unset timeout falls back to the default instead of meaning no timeout")
    void testTimeoutDefault() {
        ResourcePackSettings settings = new ResourcePackSettings(pack(), null, 0L, false, ".");

        assertEquals(ResourcePackSettings.DEFAULT_RESPONSE_TIMEOUT, settings.responseTimeout());
    }

    @Test
    @DisplayName("A negative timeout disables the guard, a positive one is kept")
    void testTimeoutValues() {
        assertTrue(new ResourcePackSettings(pack(), null, -1L, false, ".").responseTimeout().isNegative());
        assertEquals(Duration.ofSeconds(3), new ResourcePackSettings(pack(), null, 3000L, false, ".").responseTimeout());
    }

    @Test
    @DisplayName("Bedrock delivery is off unless the configuration turns it on")
    void testBedrockDeliveryDefault() {
        assertFalse(ResourcePackSettings.disabled().sendToBedrockPlayers());
    }

    @Test
    @DisplayName("A missing bedrock prefix falls back to floodgate's default")
    void testBedrockPrefixDefault() {
        ResourcePackSettings settings = new ResourcePackSettings(pack(), null, 0L, false, null);

        assertEquals(ResourcePackSettings.DEFAULT_BEDROCK_NAME_PREFIX, settings.bedrockNamePrefix());
    }

    @Test
    @DisplayName("Changing the season leaves the base pack and every other value untouched")
    void testWithSeason() {
        ResourcePackDefinition base = pack();
        ResourcePackDefinition oldSeason = pack();
        ResourcePackDefinition newSeason = pack();
        ResourcePackSettings settings = new ResourcePackSettings(base, oldSeason, 4000L, true, "!");

        ResourcePackSettings changed = settings.withSeason(newSeason);

        assertSame(base, changed.base());
        assertSame(newSeason, changed.season());
        assertEquals(4000L, changed.responseTimeoutMillis());
        assertTrue(changed.sendToBedrockPlayers());
        assertEquals("!", changed.bedrockNamePrefix());
    }

    @Test
    @DisplayName("A season can end without a successor")
    void testSeasonCanBeCleared() {
        ResourcePackSettings settings = new ResourcePackSettings(pack(), pack(), 0L, false, ".");

        ResourcePackSettings changed = settings.withSeason(null);

        assertNull(changed.season());
        assertTrue(changed.enabled());
    }
}
